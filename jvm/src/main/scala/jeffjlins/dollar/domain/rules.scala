package jeffjlins.dollar.domain

import java.time.LocalDate

import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue}

import scala.jdk.CollectionConverters._
import cats.syntax.all._
import jeffjlins.dollar.domain.OrExpressionRule.extToOptString

trait Rule {
  val cells: List[CellData]
  val group: String
  def matches(t: Transaction): Boolean
}

case class OrExpressionRule(cells: List[CellData], group: String, anyMatchTerms: List[String], exactMatchTerms: List[String], leftMatchTerms: List[String], rightMatchTerms: List[String]) extends Rule {
  def matches(t: Transaction): Boolean = {
    val i = (t.memo :: t.name.some :: t.memo.map(m => t.name + m) :: Nil).flatten
    //println(anyMatchTerms + " - \"" + i + "\"");
    val any = anyMatchTerms.exists(m => i.exists(_.indexOf(m) >= 0))
    val left = leftMatchTerms.exists(m => i.startsWith(m))
    val right = rightMatchTerms.exists(m => i.endsWith(m))
    val exact = exactMatchTerms.exists(m => i.contains(m))
    any || left || right || exact
  }

}

object OrExpressionRule {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())

  def apply(cols: List[String], cells: List[CellData]): OrExpressionRule = {
    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue)).toMap
    val rawTerms: List[String] = List(data.get("Param 1"), data.get("Param 2"), data.get("Param 3"), data.get("Param 4"), data.get("Param 5"), data.get("Param 6"), data.get("Param 7"), data.get("Param 8"), data.get("Param 9"), data.get("Param 10"), data.get("Param 11"), data.get("Param 12")).flatten.flatMap(extToOptString)
    val terms = rawTerms.foldLeft((List[String](), List[String](), List[String](), List[String]())) {
      case ((any, exact, left, right), x) =>
        x.take(3) match {
          case "**=" => (x.drop(3) :: any, exact, left, right)
          case "\"\"=" => (any, x.drop(3) :: exact, left, right)
          case "\"*=" => (any, exact, x.drop(3) :: left, right)
          case "*\"=" => (any, exact, left, x.drop(3) :: right)
        }
    }
    OrExpressionRule(cells, data("Group"), terms._1, terms._2, terms._3, terms._4)
  }
}
