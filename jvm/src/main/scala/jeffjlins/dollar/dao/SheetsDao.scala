package jeffjlins.dollar.dao

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.{BatchUpdateSpreadsheetRequest, GridData, Request, Response, Sheet, Spreadsheet}

import scala.jdk.CollectionConverters._
import cats.syntax.all._

trait SheetsDao {
  val fileId: String
  val conn: Sheets
  protected var fileCache: Option[Either[Spreadsheet, Spreadsheet]]

  protected def write(requests: List[Request]): List[Response] = {
    val breq = new BatchUpdateSpreadsheetRequest().setRequests(requests.asJava)
    conn.spreadsheets().batchUpdate(fileId, breq).execute().getReplies.asScala.toList
  }

  protected def read(includeGridData: Boolean, refreshCache: Boolean = false): Spreadsheet = {
    (refreshCache, includeGridData, fileCache) match {
      case (false, true, Some(Right(file))) => file
      case (false, false, Some(e)) => e.getOrElse(e.swap.getOrElse(???))
      case _ =>
        val file = conn.spreadsheets.get(fileId).setIncludeGridData(true).execute()
        if (includeGridData) fileCache = file.asRight[Spreadsheet].some else fileCache = file.asLeft[Spreadsheet].some
        file
    }
  }

  protected def readTab(tabName: String, includeGridData: Boolean, refreshCache: Boolean = false): Sheet = {
    read(includeGridData, refreshCache).getSheets.asScala.toList.find(_.getProperties.getTitle == tabName).get
  }

  protected def readTabId(tabName: String, refreshCache: Boolean = false): Int = {
    readTab(tabName, false, refreshCache).getProperties.getSheetId
  }

  protected def readData(tabName: String, refreshCache: Boolean = false): GridData = {
    readTab(tabName, false, refreshCache).getData.asScala.toList.head
  }
}
