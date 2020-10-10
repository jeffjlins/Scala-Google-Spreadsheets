package jeffjlins.dollar.domain

import java.time.{LocalDate, YearMonth}

import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue}

import scala.util.Try

//amount used, month used and year used is a formula - do that when writing
//need to make sure the tag list works
case class Transaction(cells: List[CellData], adjusted: Boolean, rowNum: Int, id: String, source: String, date: LocalDate, dateOverride: Option[String], name: String, memo: Option[String], amount: Double, adjustedAmount: Option[String], portion: Option[Double], link: Option[String], linkType: Option[String], category: String, notes: Option[String], tags: List[String], entity: Option[String], city: Option[String], stateProvince: Option[String], country: Option[String], paymentType: Option[String], paymentAccount: Option[String], bank: String, account: String, accountType: String, checkNumber: Option[Double], currency: String, transactionType: String, idType: String, amountUsed: String, dateUsed: Option[String], yearUsed: String, monthUsed: String, location: Option[String], lastUpdated: String, lastWritten: String, importFile: Option[String]) {
  val dateMonth = YearMonth.from(date)
}
object Transaction {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())

  def apply(cols: List[String], rowNum: Int, cells: List[CellData]): Transaction = {
    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue)).toMap
    val amt: Double = data("Amount")
    val cat: String = if (data("Category") == null && amt < 0) "Unknown Expenses/Unassigned" else if (data("Category") == null && amt > 0) "Income/Unassigned" else data("Category")
    new Transaction(cells, false, rowNum, data("Id"), data("Source"), data("Date"), data("Date Override"), data("Name"), data("Memo"), amt, data("Adjusted Amount"), data("Portion"), data("Link"), data("Link Type"), cat, data("Notes"), "" :: Nil, data("Entity"), data("City"), data("State/Province"), data("Country"), data("Payment Type"), data("Payment Account"), data("Bank"), data("Account"), data("Account Type"), data("Check Number"), data("Currency"), data("Transaction Type"), data("ID Type"), data("Amount Used"), data("Date Used"), data("Year Used"), data("Month Used"), data("Location"), data("Last Updated"), data("Last Written"), data("Import File"))
  }
}

case class CatName(name: String, path: String, parentPath: Option[String], depth: Int)

case class TransactionCat(name: String, superCategory: String, path: String, parentPath: Option[String], header: List[String], transactionsDirect:List[Transaction], cats: List[TransactionCat]) {
  lazy val transactionsAll: List[Transaction] = transactionsDirect ++ cats.flatMap(_.transactionsAll)
  lazy val total: Double = transactionsAll.map(_.amount).sum
  val isBranch = transactionsDirect.isEmpty && cats.nonEmpty
  val isLeaf = transactionsDirect.nonEmpty && cats.isEmpty
}