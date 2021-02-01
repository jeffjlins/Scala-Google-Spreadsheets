package jeffjlins.dollar

import com.google.api.services.sheets.v4.SheetsScopes
import jeffjlins.dollar.domain.{AssetsTab, RulesTab, TransactionTab}
import jeffjlins.dollar.dao.{FinanceAppDao, SheetsConnection, TransDao}
import jeffjlins.dollar.presentation.{DashboardTab, RecurringTab}

object App {
  val prefs = new Preferences()
  val conn = SheetsConnection(prefs.credentialsPath, prefs.tokensDir, SheetsScopes.SPREADSHEETS :: Nil)
  val transDao = TransDao(conn.sheetsApi, prefs.transSheetFileId)
  val financeAppDao = FinanceAppDao(conn.sheetsApi, prefs.financeAppSheetFileId)
  val transTab = TransactionTab(transDao.transactionsTabData(), prefs.superCategories)
  val assetsTab = AssetsTab(transDao.assetsTabData())
  val dashboardTab = DashboardTab(transTab, assetsTab, prefs.superCategories, prefs.detailPanelPrefs, prefs.datePanelPrefs, prefs.summaryPanelPrefs, prefs.assetsPanelPrefs)
  val rulesTab = RulesTab(financeAppDao.rulesTabData())
  val recurringTab = RecurringTab(transTab, transDao.recurringTabData(), prefs.recurringDatePanelPrefs, prefs.recurringValuesPanelPrefs)

  def main(args: Array[String]): Unit = {
    args.headOption match {
      case Some("dashboard") => writeDashboard
      case Some("rules") => rules
      case Some("recurring") => writeRecurring
      case Some("all") =>
        rules
        writeDashboard
      case _ => throw new Exception("unknown operation")
    }
  }

  def writeDashboard = {
    val resp = transDao.write(dashboardTab.writerModel(transDao.dashboardTabId()) :: Nil)
    resp.foreach(println)
  }

  def rules = {
    val chgs = rulesTab.writerModelTrans(transDao.transactionsTabId(), transTab.rows)
    val transChanges = chgs //.filter(_.getUpdateCells.getRange.getStartRowIndex < 25)
    (0 to (transChanges.length / 100))
      .map(x => transChanges.slice(x * 100, (if (x * 100 + 99 > transChanges.length) transChanges.length else x * 100 + 99)))
      .map { tc =>
        val resp = transDao.write(tc)
        transChanges.map(_.getUpdateCells.getRange.getStartRowIndex).foreach(println)
        println("--------")
        resp.foreach(x => println(x.toPrettyString.length))
        Thread.sleep(2000)
        println("========")
      }
  }

  def writeRecurring = {
    val resp = transDao.write(recurringTab.writerModel(transDao.recurringTabId()) :: Nil)
    resp.foreach(println)
  }

}