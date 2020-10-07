package jeffjlins.dollar

import com.google.api.services.sheets.v4.model.{CellFormat, Color, TextFormat}

class Preferences {
  val credentialsPath = "/credentials.json"
  val tokensDir = "tokens"

  val transSheetFileId = "18F_yzi6MuYqmocVZb6EVEHoHjunTgMT0AHKMqDxW760"

  val superCategories: Map[String, List[String]] = Map(
    "Expenses" -> ("Primary Expenses" :: "Avoidable Expenses" :: "Unavoidable Expenses" :: "Planned Expenses" :: "Unknown Expenses" :: Nil),
    "Income" -> ("Income" :: Nil)
  )
  val panelPrefs = PanelPrefs(formats("detail.income"), "Income", true, true) :: PanelPrefs(formats("detail.expenseSummary"), "Expenses", false, true) :: PanelPrefs(formats("detail.expense"), "Expenses", true, false, "Primary Expenses" :: "Avoidable Expenses" :: "Unavoidable Expenses" :: "Planned Expenses" :: "Unknown Expenses" :: Nil) :: Nil
  val formats = Map(
    "date" -> Map(
      "default" -> new CellFormat().setBackgroundColor(clr("#262626")).setHorizontalAlignment("center").setTextFormat(new TextFormat().setForegroundColor(clr("#ffffff")).setBold(true))
    ),
    "detail.income" -> Map(
      "title" -> new CellFormat(),
      "wall" -> new CellFormat(),
      "lineLabel" -> new CellFormat(),
      "line" -> new CellFormat(),
      "sum" -> new CellFormat()
    ),
    "detail.expense" -> Map(
      "title" -> new CellFormat(),
      "wall" -> new CellFormat(),
      "lineLabel" -> new CellFormat(),
      "line" -> new CellFormat(),
      "sum" -> new CellFormat()
    ),
    "detail.expenseSummary" -> Map(
      "title" -> new CellFormat(),
      "wall" -> new CellFormat(),
      "lineLabel" -> new CellFormat(),
      "line" -> new CellFormat(),
      "sum" -> new CellFormat()
    )
  )

  def clr(hex: String) = {
    val red = Integer.parseInt(hex.drop(1).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    val green = Integer.parseInt(hex.drop(3).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    val blue = Integer.parseInt(hex.drop(5).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    new Color().setRed(red).setGreen(green).setBlue(blue)
  }

}

case class PanelPrefs(formats: Map[String, CellFormat], superCat: String, childrenOnly: Boolean, total: Boolean, order: List[String] = Nil)