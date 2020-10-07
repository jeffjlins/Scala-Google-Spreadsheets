package jeffjlins.dollar.dao

import scala.jdk.CollectionConverters._
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.{GridData, Request, Response, RowData, Spreadsheet}
import jeffjlins.dollar.Preferences
import cats.syntax.all._

case class TransDao(conn: Sheets, fileId: String) extends SheetsDao {
  protected var fileCache: Option[Either[Spreadsheet, Spreadsheet]] = None

  object TransFileTab extends Enumeration {
    val dashboard = Value("Dashboard")
    val transactions = Value("transactions")
  }

  override def write(requests: List[Request]): List[Response] = super[SheetsDao].write(requests)

  def dashboardTabId(refreshCache: Boolean = false): Int = readTabId(TransFileTab.dashboard.toString, refreshCache)
  def dashboardTabData(refreshCache: Boolean = false): GridData = readData(TransFileTab.dashboard.toString, refreshCache)

  def transactionsTabId(refreshCache: Boolean = false): Int = readTabId(TransFileTab.transactions.toString, refreshCache)
  def transactionsTabData(refreshCache: Boolean = false): GridData = readData(TransFileTab.transactions.toString, refreshCache)

}
