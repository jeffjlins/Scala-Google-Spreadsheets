package jeffjlins.dollar.domain

import com.google.api.services.sheets.v4.model._
import jeffjlins.dollar.{BasicPanelPrefs, DetailPanelPrefs}

import scala.jdk.CollectionConverters._

case class DashboardTab(transTab: TransactionTab, assetsTab: AssetsTab, superCategories: Map[String, List[String]], detailPanelPrefs: List[DetailPanelPrefs], datePanelPrefs: BasicPanelPrefs, summaryPanelPrefs: BasicPanelPrefs, assetsPanelPrefs: BasicPanelPrefs) {

  lazy val panels: List[Panel] = {
    val months = transTab.allCats.flatMap(_.transactionsAll).filter(_.deprecated == false).map(_.dateMonth).distinct.sorted.reverse

    val datePanel = DatePanel(Left(2), Left(0), months, datePanelPrefs.format)
    val incomeTrans = superCategories("Income").flatMap(x => transTab.rowsAdjusted.filter(_.category.startsWith(x)))
    val expenseTrans = superCategories("Expenses").flatMap(x => transTab.rowsAdjusted.filter(_.category.startsWith(x)))
    val summaryPanel = SummaryPanel(Left(0), Right(datePanel), incomeTrans, expenseTrans, datePanel.months, summaryPanelPrefs.format)
    val assetsPanel = AssetPanel(Left(0), Right(summaryPanel), incomeTrans ++ expenseTrans, assetsTab, datePanel.months, assetsPanelPrefs.format)
    val panelInputs: List[(DetailPanelPrefs, TransactionCat)] = detailPanelPrefs.flatMap { pref =>
      if (pref.childrenOnly) {
        superCategories(pref.superCat).map { c =>
          pref -> transTab.allCatsAdjusted.find(_.path == c).get
        }
      } else (pref -> transTab.allCatsAdjusted.find(c => c.path.isEmpty && c.superCategory == pref.superCat).get :: Nil)
    }

    panelInputs.foldLeft[List[Panel]](assetsPanel :: summaryPanel :: datePanel :: Nil) { (panels, panelInput) =>
      DetailPanel(Left(0), Right(panels.head), panelInput._1.total, panelInput._2, datePanel.months, panelInput._1.order, panelInput._1.formats) :: panels
    }
  }

  lazy val height: Int = calcHeight(panels)
  protected def calcHeight(panels: List[Panel]): Int = panels.map(p => p.height + p.y).max

  lazy val width: Int = calcWidth(panels)
  protected def calcWidth(panels: List[Panel]): Int = panels.map(p => p.width + p.x).max

  lazy val grid: java.util.List[RowData] = grid(panels)
  protected def grid(panels: List[Panel]): java.util.List[RowData] = {
    (0 until height).map { h =>
      val cells = (0 until width).map { w =>
        panels.flatMap(_.getCell(w, h)).headOption.getOrElse(new CellData())
      }
      new RowData().setValues(cells.asJava)
    }.asJava
  }

  def writerModel(sheetId: Int): Request = writerModel(sheetId, grid, width, height)
  protected def writerModel(sheetId: Int, gridData: java.util.List[RowData], w: Int, h: Int): Request = {
    val req = new UpdateCellsRequest()
      .setRows(gridData)
      .setFields("userEnteredValue,userEnteredFormat")
      .setRange(
        new GridRange()
          .setSheetId(sheetId)
          .setStartColumnIndex(0)
          .setEndColumnIndex(w)
          .setStartRowIndex(0)
          .setEndRowIndex(h)
      )
    new Request().setUpdateCells(req)
  }

}