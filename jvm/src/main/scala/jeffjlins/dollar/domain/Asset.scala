package jeffjlins.dollar.domain

import java.time.{LocalDate, YearMonth}

import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue}

case class Asset(cells: List[CellData], account: Option[Double], item: Option[String], date: LocalDate, amount: Double, uom: String, amountType: String, category: String, useTransactions: Boolean, startOfDay: Boolean, file: Option[String]) {
  val dateMonth = YearMonth.from(date)
}

object Asset {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())

  def apply(cols: List[String], rowNum: Int, cells: List[CellData]): Asset = {
    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue).padTo(cols.length, null)).toMap
    val startOfDay = if (extToString(data("Start of Day")) == "Y") true else false
    val useTransactions = if (extToString(data("Use Transactions")) == "Y") true else false
    val (a, b, c, d, e) = (data("Account"), data("Item"), data("Date"), data("Amount"), data("Uom"))
    val f = data("Amount Type")
    val g = data("Category")
    val h = data("File")
    new Asset(cells, data("Account"), data("Item"), data("Date"), data("Amount"), data("Uom"), data("Amount Type"), data("Category"), useTransactions, startOfDay, data("File"))
  }
}

case class AssetCategory(name: String, assets: List[Asset], useTransactions: Boolean)