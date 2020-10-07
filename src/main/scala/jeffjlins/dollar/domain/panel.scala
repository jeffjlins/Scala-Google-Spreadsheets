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

case class DatePanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], trans: List[Transaction]) extends Panel {
  private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")
  val months = trans.map(_.dateMonth).distinct.sorted
  override val width: Int = months.length
  override val height: Int = 1

  private val monthsIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x -> t._1).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    if (xx >= x && xx < x + width - 1 && yy == y)
      Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(monthFmt.format(monthsIndex(xx)))))
    else
      None
  }
}

case class DetailPanel(xPlacement: Either[Int, Panel], yPlacement: Either[Int, Panel], total: Boolean, cat: TransactionCat, months: List[YearMonth], orderPreference: List[String] = Nil) extends Panel {
  val title = true
  private val leftBorderWidth = 1
  private val catNameWidth = 1
  override val width: Int = months.length + leftBorderWidth + catNameWidth
  private val titleHeight = if (title) 1 else 0
  private val sumLineHeight = (if (total) 1 else 0)
  override val height: Int = cat.cats.length + titleHeight + sumLineHeight

  //private val monthFmt = DateTimeFormatter.ofPattern("YYYY-MM")
  private val cleanOrderPref = orderPreference.flatMap(c => cat.cats.find(_.path == c))
  private val subcatOrder = cleanOrderPref ++ cat.cats.filter(c => !cleanOrderPref.contains(c.path))
  private val subcatIndex = subcatOrder.zipWithIndex.map(t => t._2 + y + 1 -> t._1).toMap //((this.y + 1) until (this.y + 1 + subcatOrder.length)).zip(subcatOrder).toMap
  private val monthIndex: Map[Int, YearMonth] = months.zipWithIndex.map(t => t._2 + x + 2 -> t._1).toMap
  //private val monthIndex = ((this.x + 2) until (this.x + 2 + months.length)).zip(months).toMap

  override def getCell(xx: Int, yy: Int): Option[CellData] = {
    // Top left corner
    if (xx == x && yy == y) Some(new CellData())
    // Top row
    else if (xx >= x && xx < x + width && xx != x + leftBorderWidth && yy == y) Some(new CellData())
    // Left column
    else if (xx == x && yy >= y && yy < y + height) Some(new CellData())
    // Panel name cell
    else if (xx == x + leftBorderWidth && yy == y) Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(cat.name)))
    // Subcat name cells
    else if (xx == x + leftBorderWidth && yy >= y + titleHeight && yy < y + height - sumLineHeight)
      Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(subcatIndex(yy).name)))
    // Subcat amounts
    else if (xx >= x + leftBorderWidth + catNameWidth && xx < x + width && yy >= y + titleHeight && yy < y + height - sumLineHeight) {
      val total = subcatIndex(yy).transactionsAll.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum //t => monthFmt.format(t.date) == monthIndex(xx)).map(_.amount).sum
      Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(total)))
    }
    // Sum
    else if (xx == x + leftBorderWidth  && yy == y + height - sumLineHeight) Some(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("SUM")))
    // Totals
    else if (xx >= x + leftBorderWidth + catNameWidth  && xx < x + width && yy == y + height - 1) {
      val total = cat.transactionsAll.filter(_.dateMonth.equals(monthIndex(xx))).map(_.amount).sum //filter(t => monthFmt.format(t.date) == monthIndex(xx)).map(_.amount).sum
      Some(new CellData().setUserEnteredValue(new ExtendedValue().setNumberValue(total)))
    }
    // outside range
    else None
  }
}

