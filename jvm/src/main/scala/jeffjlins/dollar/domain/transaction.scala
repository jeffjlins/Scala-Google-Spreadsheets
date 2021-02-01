package jeffjlins.dollar.domain

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, YearMonth}

import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue, RowData}
import jeffjlins.dollar.domain.Transaction.{DCell, Fields}

/**
 *
 * @param cells
 * @param rowNum
 * @param deprecated If alternative transactions are created that replace this transaction then this transaction is considered deprecated and won't be counted in dashboard calculations
 * @param overlay If this transaction was created as part of a group of new transactions that replace another as alternatives then they are considered overlay and will not be written back to the sheet and are only used for analysis
 */
case class Transaction(cells: List[DCell], rowNum: Option[Int], deprecated: Boolean = false, overlay: Boolean = false) {
  import scala.language.implicitConversions
  private implicit def extToString(e: ExtendedValue): String = e.getStringValue
  private implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  private implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  private implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  private implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue()) // LocalDate.of(1899, 12, 30).until(value, ChronoUnit.DAYS).toDouble

  val id: String = uev(Fields.Id)
  val date: LocalDate = uev(Fields.Date)
  val name: String = uev(Fields.Name)
  val memo: Option[String] = uev(Fields.Memo)
  val amount: Double = uev(Fields.Amount)
  val adjustedAmount: Option[String] = uev(Fields.Adjustment)
  val category: String = if (uev(Fields.Category) ==  null) "Unknown Expenses/Unassigned" else uev(Fields.Category)
  val recurring: Option[String] = uev(Fields.Recurring)
  val notes: Option[String] = uev(Fields.Notes)
  val bank: String = uev(Fields.Bank)
  val account: String = uev(Fields.Account)
  val accountType: String = uev(Fields.AccountType)
  val checkNumber: Option[Double] = uev(Fields.CheckNumber)
  val currency: String = uev(Fields.Currency)
  val transactionType: Option[String] = uev(Fields.TransactionType)
  val idType: String = uev(Fields.IdType)
  val lastUpdated: String = uev(Fields.LastUpdated)
  val created: String = uev(Fields.Created)
  val importFile: Option[String] = uev(Fields.ImportFile)
  val exportId: Option[String] = uev(Fields.ExportId)

  val dateMonth = YearMonth.from(date)
  val modified: Boolean = cells.exists(_.changed)

  def virtualize: Transaction = copy(overlay = true)

  def reinterpret: Transaction = copy(deprecated = true)

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
    Transaction(modifiedCell :: cells.filterNot(_ == cellToModify), rowNum, deprecated, overlay)
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
    val Recurring = Value("Recurring")
    val Notes = Value("Notes")
    val Bank = Value("Bank")
    val Account = Value("Account")
    val AccountType = Value("Account Type")
    val CheckNumber = Value("Check Number")
    val Currency = Value("Currency")
    val TransactionType = Value("Transaction Type")
    val IdType = Value("ID Type")
    val LastUpdated = Value("Last Updated")
    val Created = Value("Created")
    val ImportFile = Value("Import File")
    val ExportId = Value("Export ID")
  }

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