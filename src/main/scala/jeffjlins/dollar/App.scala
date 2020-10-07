package jeffjlins.dollar

import com.google.api.services.sheets.v4.SheetsScopes
import jeffjlins.dollar.domain.{DashboardTab, TransactionTab}
import jeffjlins.dollar.dao.{SheetsConnection, TransDao}

object App {
  def main(args: Array[String]): Unit = {
    val prefs = new Preferences()
    val conn = SheetsConnection(prefs.credentialsPath, prefs.tokensDir, SheetsScopes.SPREADSHEETS :: Nil)
    val transDao = TransDao(conn.sheetsApi, prefs.transSheetFileId)
    val transTab = TransactionTab(transDao.transactionsTabData(), prefs.superCategories)
    val dashboardTab = DashboardTab(transTab, prefs.superCategories, prefs.panelPrefs)
    val resp = transDao.write(dashboardTab.writerModel(transDao.dashboardTabId()) :: Nil)
    resp.foreach(println)
  }

}