package jeffjlins.dollar.domain

import java.time.format.DateTimeFormatter

import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.{CellData, CellFormat, ExtendedValue, GridRange, NumberFormat, Request, RowData, UpdateCellsRequest}
import jeffjlins.dollar.Preferences
import jeffjlins.dollar.dao.{SheetsConnection, TransDao}
import org.scalatest.flatspec.AnyFlatSpec

import scala.jdk.CollectionConverters._

class TransactionSpec extends AnyFlatSpec {
  val prefs = new Preferences()
  val conn = SheetsConnection(prefs.credentialsPath, prefs.tokensDir, SheetsScopes.SPREADSHEETS :: Nil)
  val transDao = TransDao(conn.sheetsApi, prefs.transSheetFileId)

  "Date converter" should "convert sheets date number values to java.time dates" in {
//    val cell = new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("2020-10-11")).setUserEnteredFormat(new CellFormat().setNumberFormat(new NumberFormat().setType("DATE")))
//    val grid = List(new RowData().setValues(List(cell).asJava)).asJava
//    val req = new Request().setUpdateCells(
//      new UpdateCellsRequest()
//      .setRows(grid)
//      .setFields("userEnteredValue,userEnteredFormat")
//      .setRange(
//        new GridRange()
//          .setSheetId(transDao.scratchTabId())
//          .setStartColumnIndex(0)
//          .setEndColumnIndex(1)
//          .setStartRowIndex(0)
//          .setEndRowIndex(1)
//      )
//    )
//    val resp = transDao.write(req :: Nil)
//    resp.foreach(println)
//    Thread.sleep(5000)

//    val data = transDao.scratchTabData().getRowData.asScala.head.getValues.asScala.head
//    val eff = data.getUserEnteredValue

    val date = Transaction.extToLocalDate(new ExtendedValue().setNumberValue(42198.0))


    //assert(eff.getNumberValue == 42198)
    assert(date.getYear == 2015)
    assert(date.getMonth.getValue == 7)
    assert(date.getDayOfMonth == 13)
    assert(DateTimeFormatter.ofPattern("yyyy-MM").format(date) == "2015-07")
  }
}
