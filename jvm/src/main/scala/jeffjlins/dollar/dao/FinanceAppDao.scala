package jeffjlins.dollar.dao

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.{GridData, Request, Response, Spreadsheet}

case class FinanceAppDao(conn: Sheets, fileId: String) extends SheetsDao {
  protected var fileCache: Option[Either[Spreadsheet, Spreadsheet]] = None

  object AppFileTab extends Enumeration {
    val rules = Value("rules")
    val recurring = Value("recurring")
  }

  override def write(requests: List[Request]): List[Response] = super[SheetsDao].write(requests)

  def rulesTabId(refreshCache: Boolean = false): Int = readTabId(AppFileTab.rules.toString, refreshCache)
  def rulesTabData(refreshCache: Boolean = false): GridData = readData(AppFileTab.rules.toString, refreshCache)

  def recurringTabId(refreshCache: Boolean = false): Int = readTabId(AppFileTab.recurring.toString, refreshCache)
  def recurringTabData(refreshCache: Boolean = false): GridData = readData(AppFileTab.recurring.toString, refreshCache)

}
