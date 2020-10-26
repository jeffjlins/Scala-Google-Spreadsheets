package jeffjlins.dollar.domain

import java.time.YearMonth
import java.time.temporal.ChronoUnit

import com.google.api.services.sheets.v4.model.{CellData, GridData, GridRange, Request, RowData, UpdateCellsRequest}
import jeffjlins.dollar.BasicPanelPrefs
import cats.implicits._
import jeffjlins.dollar.util.Utils

import scala.jdk.CollectionConverters._

case class RecurringTab(transTab: TransactionTab, recurringGridData: GridData, datePanelPrefs: BasicPanelPrefs, recurringValuesPanelPrefs: BasicPanelPrefs) {
  private val defCols = 8

  private val trans = transTab.rowsAdjusted

  lazy val (firstMonth, lastMonth) = {
    val allDates = trans.filter(_.overlay == false).map(_.dateMonth).distinct.sortWith((a, b) => a.isBefore(b))
    allDates.head -> allDates.reverse.head
  }

  lazy val panels: List[Panel] = createPanels(firstMonth, lastMonth)
  protected def createPanels(fMonth: YearMonth, lMonth: YearMonth): List[Panel] = {
    val months: List[YearMonth] = (0L to ChronoUnit.MONTHS.between(firstMonth, lastMonth)).map(firstMonth.plusMonths(_)).toList

    val rows = recurringGridData.getRowData.asScala.toList.zipWithIndex.map(_.swap)
    val headers = Utils.cellsToHeaders(rows.head._2.getValues.asScala.toList.take(defCols))
    val headerIdx = headers.zipWithIndex.toMap

    val recs = rows.filter(_._2.getValues.asScala.toList(headers.indexOf("Name")).getUserEnteredValue != null).tail.map { rd =>
      val name = rd._2.getValues.asScala.toList(headerIdx(RecurringField.Name.entryName))
      RecurringFromSheetWithTrans(DDCell.create(headers, rd._2.getValues.asScala.toList.take(defCols), RecurringField), rd._1, months, trans.filter(_.recurring.exists(_ == name)))
    }

    val datePanel = DatePanel((defCols).asLeft[Panel], 0.asLeft[Panel], months, datePanelPrefs.format)
    val recPanel = RecurringValuesPanel((defCols).asLeft[Panel], datePanel.asRight[Int], recs, fMonth, lMonth, recurringValuesPanelPrefs.format)
    datePanel :: recPanel :: Nil
  }

  lazy val height: Int = calcHeight(panels)
  protected def calcHeight(panels: List[Panel]): Int = panels.map(p => p.height + p.y).max

  lazy val width: Int = calcWidth(panels)
  protected def calcWidth(panels: List[Panel]): Int = panels.map(p => p.width + p.x).max

  lazy val grid: java.util.List[RowData] = grid(panels)
  protected def grid(panels: List[Panel]): java.util.List[RowData] = {
    (0 until height).map { h =>
      val cells = (defCols until width).map { w =>
        panels.flatMap(_.getCell(w, h)).headOption.getOrElse(new CellData())
      }
      new RowData().setValues(cells.asJava)
    }.asJava
  }

  def writerModel(sheetId: Int): Request = writerModel(sheetId, grid, defCols + 1, 0, width, height)
  protected def writerModel(sheetId: Int, gridData: java.util.List[RowData], x: Int, y: Int, w: Int, h: Int): Request = {
    val req = new UpdateCellsRequest()
      .setRows(gridData)
      .setFields("userEnteredValue,userEnteredFormat")
      .setRange(
        new GridRange()
          .setSheetId(sheetId)
          .setStartColumnIndex(x)
          .setEndColumnIndex(x + w)
          .setStartRowIndex(y)
          .setEndRowIndex(y + h)
      )
    new Request().setUpdateCells(req)
  }
}
