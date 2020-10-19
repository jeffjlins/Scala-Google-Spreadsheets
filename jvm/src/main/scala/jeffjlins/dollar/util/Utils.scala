package jeffjlins.dollar.util

import com.google.api.services.sheets.v4.model.{Color, Request}
import com.google.gson.GsonBuilder

object Utils {
  def requestToJson(request: Request):String = {
    new GsonBuilder().setPrettyPrinting().create.toJson(request)
  }
  val colZToLetter = Map(0 -> "A", 1 -> "B", 2 -> "C", 3 -> "D", 4 -> "E", 5 -> "F", 6 -> "G", 7 -> "H", 8 -> "I", 9 -> "J", 10 -> "K", 11 -> "L", 12 -> "M", 13 -> "N", 14 -> "O", 15 -> "P", 16 -> "Q", 17 -> "R", 18 -> "S", 19 -> "T", 20 -> "U", 21 -> "V", 22 -> "W", 23 -> "X", 24 -> "Y", 25 -> "Z")
  def hexToColor(hex: String) = {
    val red = Integer.parseInt(hex.drop(1).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    val green = Integer.parseInt(hex.drop(3).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    val blue = Integer.parseInt(hex.drop(5).take(2), 16).toFloat / Integer.parseInt("FF", 16).toFloat
    new Color().setRed(red).setGreen(green).setBlue(blue)
  }
}
