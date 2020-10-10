package jeffjlins.dollar.domain

import java.time.YearMonth
import java.time.format.DateTimeFormatter

import com.google.api.services.sheets.v4.model.{CellData, CellFormat, ExtendedValue}

trait Panel {
  val xPlacement: Either[Int, Panel]
  val yPlacement: Either[Int, Panel]
  val width: Int
  val height: Int

  val x: Int = xPlacement match {
    case Left(i) => i
    case Right(pnl) => pnl.x + pnl.width
  }

  val y: Int = yPlacement match {
    case Left(i) => i
    case Right(pnl) => pnl.y + pnl.height
  }

  def getCell(x: Int, y: Int): Option[CellData]
}

case class DatePanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], trans: List[Transaction], format: Map[String, CellFormat]) extends Panel {
  private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")
  val months = trans.map(_.dateMonth).distinct.sorted.reverse
  override val width: Int = months.length
  override val height: Int = 1

  private val monthsIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x -> t._1).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    if (xx >= x && xx < x + width && yy == y)
      Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(monthFmt.format(monthsIndex(xx)))).setUserEnteredFormat(format("default")))
    else
      None
  }
}

case class SummaryPanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], incomeTrans: List[Transaction], expenseTrans: List[Transaction], months: List[YearMonth], format: Map[String, CellFormat]) extends Panel {
  val title = true
  private val leftBorderWidth = 1
  private val lineLabelWidth = 1
  override val width: Int = months.length + leftBorderWidth + lineLabelWidth
  private val titleHeight = if (title) 1 else 0
  override val height: Int = titleHeight + 5

  private val monthIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x + 2 -> t._1).toMap
  private val rowLabelIndex: Map[Int, String] = ("Total Assets" :: "Total Cashflow" :: "Total Income" :: "Total Expenses" :: "Total Surplus" :: Nil).zipWithIndex.map(t => (t._2 + y + titleHeight) -> t._1).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    val validRange = yy >= y && yy < y + height && xx >= x && xx < x + width
    val borderColumn = xx == x
    val nameColumn = xx == x + leftBorderWidth
    val topRow = yy == y
    if (validRange) {
      // Top Left Corner
      if (borderColumn && topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Top Border
      else if (topRow && !borderColumn && !nameColumn)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Left Border
      else if (borderColumn && !topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Panel name cell
      else if (topRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Summary")).setUserEnteredFormat(format("title")))
      // Line name cells
      else if (!topRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(rowLabelIndex(yy))).setUserEnteredFormat(format("lineLabel")))
      // Amounts
      else if (!topRow && !nameColumn) {
        val total = rowLabelIndex(yy) match {
          case "Total Assets" => 0
          case "Total Cashflow" => 0
          case "Total Income" => incomeTrans.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum
          case "Total Expenses" => expenseTrans.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum
          case "Total Surplus" => incomeTrans.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum - expenseTrans.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum
        }
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(total)).setUserEnteredFormat(format("line")))
      }
      // Impossible hopefully
      else throw new Exception("grid location unaccounted for")
    }
    // Outside range
    else None
  }
}

case class AssetPanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], trans: List[Transaction], assetsTab: AssetsTab, months: List[YearMonth], format: Map[String, CellFormat]) extends Panel {
  private val assetCats = assetsTab.allAssetCats
  private val title = true
  private val leftBorderWidth = 1
  private val catNameWidth = 1
  override val width: Int = months.length + leftBorderWidth + catNameWidth
  private val titleHeight = if (title) 1 else 0
  override val height: Int = assetCats.length + titleHeight

  private val assetCatIndex = assetCats.zipWithIndex.map(x => (x._2 + y + titleHeight) -> x._1).toMap
  private val monthIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x + 2 -> t._1).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    val validRange = yy >= y && yy < y + height && xx >= x && xx < x + width
    val borderColumn = xx == x
    val nameColumn = xx == x + leftBorderWidth
    val topRow = yy == y


    if (validRange) {
      // Top Left Corner
      if (borderColumn && topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Top Border
      else if (topRow && !borderColumn && !nameColumn)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Left Border
      else if (borderColumn && !topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Panel name cell
      else if (topRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Assets")).setUserEnteredFormat(format("title")))
      // Line label cells
      else if (!topRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(assetCatIndex(yy).name)).setUserEnteredFormat(format("lineLabel")))
      // Amounts
      else if (!topRow && !nameColumn && !borderColumn) {
        val assetCat = assetCatIndex(yy)
        val amt: Double = if (assetCat.useTransactions) {
          //end of month or current day
          assetCat.assets.filter(_.date.isAfter(monthIndex(xx).atEndOfMonth())).sortBy(_.date).headOption.map { asset =>
            asset.amount + trans.filter(x => x.date.isAfter(monthIndex(xx).atEndOfMonth().minusDays(1)) && x.date.isBefore(asset.date)).map(_.amount * -1).sum
          }.getOrElse(0)
        } else {
          assetCat.assets.filter(_.date.isBefore(monthIndex(xx).plusMonths(1).atDay(1))).sortBy(_.date).reverse.headOption.map { asset =>
            if (asset.amountType != "USD") assetsTab.convertAmount(asset, monthIndex(xx).atEndOfMonth()) else asset.amount
          }.getOrElse(0)
        }
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(amt)).setUserEnteredFormat(format("line")))
      }
      // Impossible hopefully
      else throw new Exception("grid location unaccounted for")
    }
    // Outside range
    else None
  }
}

case class DetailPanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], total: Boolean, cat: TransactionCat, months: List[YearMonth], orderPreference: List[String] = Nil, format: Map[String, CellFormat]) extends Panel {
  val title = true
  private val leftBorderWidth = 1
  private val catNameWidth = 1
  override val width: Int = months.length + leftBorderWidth + catNameWidth
  private val titleHeight = if (title) 1 else 0
  private val sumLineHeight = (if (total) 1 else 0)
  override val height: Int = cat.cats.length + titleHeight + sumLineHeight

  private val cleanOrderPref = orderPreference.flatMap(c => cat.cats.find(_.path == c))
  private val subcatOrder = cleanOrderPref ++ cat.cats.filter(c => !cleanOrderPref.contains(c.path))
  private val subcatIndex = subcatOrder.zipWithIndex.map(t => t._2 + y + 1 -> t._1).toMap
  private val monthIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x + 2 -> t._1).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    val validRange = yy >= y && yy < y + height && xx >= x && xx < x + width
    val borderColumn = xx == x
    val nameColumn = xx == x + leftBorderWidth
    val topRow = yy == y
    val lastRow = yy == y + height - 1

    if (validRange) {
      // Top Left Corner
      if (borderColumn && topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Top Border
      else if (topRow && !borderColumn && !nameColumn)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Left Border
      else if (borderColumn && !topRow)
        Some(new CellData().setUserEnteredFormat(format("wall")))
      // Panel name cell
      else if (topRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(cat.name)).setUserEnteredFormat(format("title")))
      // Subcat name cells
      else if (!topRow && nameColumn && (!total || !lastRow))
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(subcatIndex(yy).name)).setUserEnteredFormat(format("lineLabel")))
      // Subcat amounts
      else if (!topRow && !nameColumn && !borderColumn && (!total || !lastRow)) {
        val total = subcatIndex(yy).transactionsAll.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(total)).setUserEnteredFormat(format("line")))
      }
      // Sum
      else if (total && lastRow && nameColumn)
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("SUM")).setUserEnteredFormat(format("sum")))
      // Totals
      else if (total && lastRow && !nameColumn && !borderColumn) {
        val total = cat.transactionsAll.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum
        Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(total)).setUserEnteredFormat(format("sum")))
      }
      // Impossible hopefully
      else throw new Exception("grid location unaccounted for")
    }
    // Outside range
    else None
  }
}

