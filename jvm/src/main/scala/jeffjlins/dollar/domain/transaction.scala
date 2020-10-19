package jeffjlins.dollar.domain

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, YearMonth}

import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue, RowData}
import jeffjlins.dollar.domain.Transaction.{DCell, Fields}

import scala.util.Try

//amount used, month used and year used is a formula - do that when writing
//need to make sure the tag list works
//case class Transaction(cells: List[CellData], cols: List[String], adjusted: Boolean, rowNum: Int, id: String, date: LocalDate, name: String, memo: Option[String], amount: Double, adjustedAmount: Option[String], category: String, notes: Option[String], bank: String, account: String, accountType: String, checkNumber: Option[Double], currency: String, transactionType: String, idType: String, lastUpdated: String, lastWritten: String, importFile: Option[String]) {
//  val dateMonth = YearMonth.from(date)
//
//  def modify(cellToModify: DCell, f: CellData => CellData): Transaction = {
//    val modifiedCell = cellToModify.copy(cellData = f(cellToModify.cellData.clone()), modifications = f :: cellToModify.modifications, changed = true)
//    Transaction(modifiedCell :: cells.filterNot(_ == cellToModify))
//  }
//}
//object Transaction {
//  import scala.language.implicitConversions
//  implicit def extToString(e: ExtendedValue): String = e.getStringValue
//  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
//  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
//  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
//  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())
//
//  def apply(cols: List[String], rowNum: Int, cells: List[CellData]): Transaction = {
//    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue)).toMap
//    val amt: Double = data("Amount")
//    val cat: String = if (data("Category") == null && amt < 0) "Unknown Expenses/Unassigned" else if (data("Category") == null && amt > 0) "Income/Unassigned" else data("Category")
//    new Transaction(cells, cols, false, rowNum, data("Id"), data("Date"), data("Name"), data("Memo"), amt, data("Adjusted Amount"), cat, data("Notes"), data("Bank"), data("Account"), data("Account Type"), data("Check Number"), data("Currency"), data("Transaction Type"), data("ID Type"), data("Last Updated"), data("Last Written"), data("Import File"))
//  }
//}

/**
 *
 * @param cells
 * @param rowNum
 * @param reinterpreted If alternative transactions are created that replace this transaction then this transaction is considered reinterpreted and won't be counted in dashboard calculations
 * @param virtual If this transaction was created as part of a group of new transactions that replace another as alternatives then they are considered virtual and will not be written back to the sheet and are only used for analysis
 */
case class Transaction(cells: List[DCell], rowNum: Option[Int], reinterpreted: Boolean = false, virtual: Boolean = false) {
  import scala.language.implicitConversions
  private implicit def extToString(e: ExtendedValue): String = e.getStringValue
  private implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  private implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  private implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  private implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())

  val id: String = uev(Fields.Id)
  val date: LocalDate = uev(Fields.Date)
  val name: String = uev(Fields.Name)
  val memo: Option[String] = uev(Fields.Memo)
  val amount: Double = uev(Fields.Amount)
  val adjustedAmount: Option[String] = uev(Fields.Adjustment)
  val category: String = if (uev(Fields.Category) ==  null) "Unknown Expenses/Unassigned" else uev(Fields.Category)
  val notes: Option[String] = uev(Fields.Notes)
  val bank: String = uev(Fields.Bank)
  val account: String = uev(Fields.Account)
  val accountType: String = uev(Fields.AccountType)
  val checkNumber: Option[Double] = uev(Fields.CheckNumber)
  val currency: String = uev(Fields.Currency)
  val transactionType: String = uev(Fields.TransactionType)
  val idType: String = uev(Fields.IdType)
  val lastUpdated: String = uev(Fields.LastUpdated)
  val lastWritten: String = uev(Fields.LastWritten)
  val importFile: Option[String] = uev(Fields.ImportFile)

  val dateMonth = YearMonth.from(date)
  val modified: Boolean = cells.exists(_.changed)

  def virtualize: Transaction = copy(virtual = true)

  def reinterpret: Transaction = copy(reinterpreted = true)

  def modifyField(cellName: Fields.Value, v: String): Transaction = {
    modifyField(cellName)((cd: CellData) => cd.setUserEnteredValue(new ExtendedValue().setStringValue(v)))
  }

  def modifyField(cellName: Fields.Value, v: Double): Transaction = {
    modifyField(cellName)((cd: CellData) => cd.setUserEnteredValue(new ExtendedValue().setNumberValue(v)))
  }

  def modifyField(cellName: Fields.Value, v: LocalDate): Transaction = {
    val numberValue = LocalDate.of(1899, 12, 30).until(v, ChronoUnit.DAYS).toDouble
    modifyField(cellName)((cd: CellData) => cd.setUserEnteredValue(new ExtendedValue().setNumberValue(numberValue)))
  }

  def modifyField(cellName: Fields.Value, v: ExtendedValue): Transaction = {
    modifyField(cellName)((cd: CellData) => cd.setUserEnteredValue(v))
  }

  def modifyField(cellName: Fields.Value)(f: CellData => CellData): Transaction = {
    val cellToModify: DCell = cells.find(_.name == cellName).get
    val modifiedCell = cellToModify.copy(cellData = f(cellToModify.cellData.clone()), modifications = f :: cellToModify.modifications, changed = true)
    Transaction(modifiedCell :: cells.filterNot(_ == cellToModify), rowNum, reinterpreted, virtual)
  }

  def changedFields: List[DCell] = {
    cells.filter(_.changed)
  }

  private def uev(name: Fields.Value): ExtendedValue = cells.find(_.name == name).get.cellData.getUserEnteredValue
}

object Transaction {

  case class DCell(name: Fields.Value, originalCellData: CellData, cellData: CellData, colNum: Int, changed: Boolean, modifications: List[CellData => CellData] = Nil) {
    val changeTypes: List[String] = "userEnteredValue" :: "userEnteredFormat" :: Nil
  }

  object Fields extends Enumeration {
    val Id = Value("Id")
    val Date = Value("Date")
    val Name = Value("Name")
    val Memo = Value("Memo")
    val Amount = Value("Amount")
    val Adjustment = Value("Adjusted Amount")
    val Trip = Value("Trip")
    val Category = Value("Category")
    val Notes = Value("Notes")
    val Bank = Value("Bank")
    val Account = Value("Account")
    val AccountType = Value("Account Type")
    val CheckNumber = Value("Check Number")
    val Currency = Value("Currency")
    val TransactionType = Value("Transaction Type")
    val IdType = Value("ID Type")
    val LastUpdated = Value("Last Updated")
    val LastWritten = Value("Last Written")
    val ImportFile = Value("Import File")
  }

//  def apply(cols: List[String], virtual: Boolean, id: String, date: LocalDate, name: String, memo: Option[String], amount: Double, adjustedAmount: Option[String], category: String, notes: Option[String], bank: String, account: String, accountType: String, checkNumber: Option[Double], currency: String, transactionType: String, idType: String, lastUpdated: String, lastWritten: String, importFile: Option[String]) = {
//    object Dc {
//      val colIdx: Map[String, Int] = cols.zipWithIndex.toMap
//      def dc(name: CellName.Value, value: Option[Any]): DCell = {
//        value.map {
//          case (x: String) => dc(name, x)
//          case (x: LocalDate) => dc(name, x)
//          case (x: Double) => dc(name, x)
//          case _ => throw new Exception("Unexpected type for DCell value conversion")
//        }.getOrElse(dc(name))
//      }
//      def dc(name: CellName.Value, value: String): DCell = {
//        val cd = new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(value))
//        DCell(name, cd, cd, colIdx(name.toString), false)
//      }
//      def dc(name: CellName.Value, value: Double): DCell = {
//        val cd = new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(value))
//        DCell(name, cd, cd, colIdx(name.toString), false)
//      }
//      def dc(name: CellName.Value, value: LocalDate): DCell = {
//        val doubleDate = LocalDate.of(1899, 12, 30).until(value, ChronoUnit.DAYS).toDouble // ChronoUnit.DAYS.daysBetween(d1, d2)
//        val cd = new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(doubleDate))
//        DCell(name, cd, cd, colIdx(name.toString), false)
//      }
//      def dc(name: CellName.Value): DCell = {
//        val cd = new CellData().setUserEnteredValue(new ExtendedValue())
//        DCell(name, cd, cd, colIdx(name.toString), false)
//      }
//    }
//    import Dc._
//    val cells = dc(CellName.Id, id) :: ... :: Nil
//    Transaction(cells, None, false, virtual)
//  }
  def apply(cols: List[String], rowNum: Int, cells: List[CellData]): Transaction = {
    val dcells = cols.zipWithIndex.map { case (col, i) =>
      DCell(Fields.withName(col), cells(i), cells(i), i, false)
    }
    Transaction(dcells, Some(rowNum))
  }
}

///////////////

case class CatName(name: String, path: String, parentPath: Option[String], depth: Int)

case class TransactionCat(name: String, superCategory: String, path: String, parentPath: Option[String], header: List[String], transactionsDirect:List[Transaction], cats: List[TransactionCat]) {
  lazy val transactionsAll: List[Transaction] = transactionsDirect ++ cats.flatMap(_.transactionsAll)
  lazy val total: Double = transactionsAll.map(_.amount).sum
  val isBranch = transactionsDirect.isEmpty && cats.nonEmpty
  val isLeaf = transactionsDirect.nonEmpty && cats.isEmpty
}