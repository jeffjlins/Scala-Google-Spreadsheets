package jeffjlins.dollar

import com.google.api.services.sheets.v4.model.{CellFormat, Color, TextFormat}
import jeffjlins.dollar.util.Utils

class Preferences {
  val credentialsPath = "/credentials.json"
  val tokensDir = "tokens"

  val transSheetFileId = "18F_yzi6MuYqmocVZb6EVEHoHjunTgMT0AHKMqDxW760"
  val financeAppSheetFileId = "1nyET27BtBI_sl8lXjUPLt9b7fC75IJ0t8LcH2m0Uzt8"

  val superCategories: Map[String, List[String]] = Map(
    "Expenses" -> ("Primary Expenses" :: "Avoidable Expenses" :: "Unavoidable Expenses" :: "Planned Expenses" :: "Unknown Expenses" :: Nil),
    "Income" -> ("Income" :: Nil),
    "Reallocation" -> ("Reallocation" :: Nil)
  )
  val formats = Map(
    "date" -> Map(
      "default" -> new CellFormat().setBackgroundColor(clr("#262626")).setHorizontalAlignment("center").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(true))
    ),
    "detail.expense" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#c2d69b")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#c2d69b")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#eaf1dd")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#eaf1dd")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#525a42")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "detail.income" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#b4a7d6")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#b4a7d6")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#d9d2e9")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#d9d2e9")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#6b647e")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "detail.expenseSummary" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#b4a7d6")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#b4a7d6")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#d9d2e9")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#d9d2e9")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#6b647e")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "summary" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#d9d9d9")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#d9d9d9")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#f2f2f2")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#f2f2f2")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#666666")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "assets" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#ffe599")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#ffe599")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#fff2cc")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#fff2cc")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#bf9000")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "detail.reallocation" -> Map(
      "title" -> new CellFormat().setBackgroundColor(clr("#ffe599")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(true)),
      "wall" -> new CellFormat().setBackgroundColor(clr("#ffe599")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "lineLabel" -> new CellFormat().setBackgroundColor(clr("#fff2cc")).setHorizontalAlignment("left").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "line" -> new CellFormat().setBackgroundColor(clr("#fff2cc")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "sum" -> new CellFormat().setBackgroundColor(clr("#bf9000")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(false))
    ),
    "recurring.values" -> Map(
      "Pending" -> new CellFormat().setBackgroundColor(clr("#c9daf8")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "Underpaid" -> new CellFormat().setBackgroundColor(clr("#f4cccc")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "Fulfilled" -> new CellFormat().setBackgroundColor(clr("#d9ead3")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "Late" -> new CellFormat().setBackgroundColor(clr("#fff2cc")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "Rectified" -> new CellFormat().setBackgroundColor(clr("#fce5cd")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "NotApplicable" -> new CellFormat().setBackgroundColor(clr("#f3f3f3")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false)),
      "Overpaid" -> new CellFormat().setBackgroundColor(clr("#b6d7a8")).setHorizontalAlignment("right").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false))
    ),
    "recurring.date" -> Map(
      "default" -> new CellFormat().setBackgroundColor(clr("#efefef")).setHorizontalAlignment("center").setTextFormat(new TextFormat().setForegroundColor(clr("#000000")).setBold(false))
    ),
  )
  val datePanelPrefs: BasicPanelPrefs = BasicPanelPrefs(formats("date"))
  val summaryPanelPrefs: BasicPanelPrefs = BasicPanelPrefs(formats("summary"))
  val assetsPanelPrefs: BasicPanelPrefs = BasicPanelPrefs(formats("assets"))
  val detailPanelPrefs: List[DetailPanelPrefs] = DetailPanelPrefs(formats("detail.reallocation"), "Reallocation", true, false) :: DetailPanelPrefs(formats("detail.income"), "Income", true, true) :: DetailPanelPrefs(formats("detail.expenseSummary"), "Expenses", false, true) :: DetailPanelPrefs(formats("detail.expense"), "Expenses", true, false, "Primary Expenses" :: "Avoidable Expenses" :: "Unavoidable Expenses" :: "Planned Expenses" :: "Unknown Expenses" :: Nil) :: Nil
  val recurringDatePanelPrefs: BasicPanelPrefs = BasicPanelPrefs(formats("recurring.date"))
  val recurringValuesPanelPrefs: BasicPanelPrefs = BasicPanelPrefs(formats("recurring.values"))

  def clr(hex: String): Color = Utils.hexToColor(hex)

}

case class DetailPanelPrefs(formats: Map[String, CellFormat], superCat: String, childrenOnly: Boolean, total: Boolean, order: List[String] = Nil)
case class BasicPanelPrefs(format: Map[String, CellFormat])