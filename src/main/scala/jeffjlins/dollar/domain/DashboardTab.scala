package jeffjlins.dollar.domain

import com.google.api.services.sheets.v4.model._
import jeffjlins.dollar.PanelPrefs

import scala.jdk.CollectionConverters._

case class DashboardTab(transTab: TransactionTab, superCategories: Map[String, List[String]], panelPrefs: List[PanelPrefs]) {
//  lazy val dashboardTabApi = {
//    val sheets = transFile.transFileApi.getSheets.asScala.toList
//    sheets.find(_.getProperties.getTitle == TransSheetFile.tabs.dashboard).get
//  }

  lazy val panels: List[Panel] = {
    val datePanel = DatePanel(Left(2), Left(0), transTab.allCats.flatMap(_.transactionsAll))
    val panelInputs: List[(PanelPrefs, TransactionCat)] = panelPrefs.flatMap { pref =>
      if (pref.childrenOnly) {
        superCategories(pref.superCat).map { c =>
          pref -> transTab.allCats.find(_.path == c).get
        }
      } else (pref -> transTab.allCats.find(c => c.path.isEmpty && c.superCategory == pref.superCat).get :: Nil)
    }

    panelInputs.foldLeft[List[Panel]](datePanel :: Nil) { (panels, panelInput) =>
      DetailPanel(Left(0), Right(panels.head), panelInput._1.total, panelInput._2, datePanel.months, panelInput._1.order) :: panels
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
      .setFields("userEnteredValue")
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

//  def execute = {
//    val gson = new GsonBuilder().setPrettyPrinting.create
//    //val resp = sheetService.spreadsheets().values().batchUpdate(TransSheetFile.id, new BatchUpdateValuesRequest().setData((new ValueRange().setRange("A1:" + colZToLetter(width) + height).setValues(grid.map(_.asJava).asJava) :: Nil).asJava))
//    val req = new UpdateCellsRequest()
//      .setRows(grid)
//      .setFields("userEnteredValue")
//      .setRange(
//        new GridRange()
//          .setSheetId(dashboardTabApi.getProperties.getSheetId)
//          .setStartColumnIndex(0)
//          .setEndColumnIndex(width)
//          .setStartRowIndex(0)
//          .setEndRowIndex(height)
//      )
//    println(gson.toJson(req))
//
//    val breq = new BatchUpdateSpreadsheetRequest().setRequests(java.util.List.of(new Request().setUpdateCells(req)))
//    //println("===========")
//    //println(gson.toJson(breq))
//
//    val call = sheetFiles.sheetsApi.spreadsheets().batchUpdate(TransSheetFile.id, breq)
//    //println("===========")
//    //println(gson.toJson(call))
//
//    call.execute().getReplies
//  }


}