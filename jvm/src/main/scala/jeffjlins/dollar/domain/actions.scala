package jeffjlins.dollar.domain

import java.time.LocalDate

import com.google.api.services.sheets.v4.model.{CellData, CellFormat, ExtendedValue}
import jeffjlins.dollar.util.Utils


trait Action {
  val cells: List[CellData]
  val group: String
}

trait TransMutateAction extends Action {
  def operation(t: Transaction): Transaction
}

trait TransQualifyAction extends Action

case class SetFieldsAction(cells: List[CellData], group: String, column: String, value: ExtendedValue, format: Option[CellFormat]) extends TransMutateAction {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())
  def operation(t: Transaction): Transaction = t.modifyField(Transaction.Fields.withName(column)) { (cd: CellData) =>
    format.map(cf => cd.setUserEnteredFormat(cf)).getOrElse(cd).setUserEnteredValue(value)
  }
}

object SetFieldsAction {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())
  def apply(cols: List[String], cells: List[CellData], format: Option[CellFormat]): SetFieldsAction = {
    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue)).toMap
    SetFieldsAction(cells, data("Group"), data("Param 1"), data("Param 2"), format)
  }
}

case class RecurringAction(cells: List[CellData], group: String, recurringName: String) extends TransQualifyAction

object RecurringAction {
  import scala.language.implicitConversions
  implicit def extToString(e: ExtendedValue): String = e.getStringValue
  implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue())
  def apply(cols: List[String], cells: List[CellData]): RecurringAction = {
    val data: Map[String, ExtendedValue] = cols.zip(cells.map(_.getUserEnteredValue)).toMap
    RecurringAction(cells, data("Group"), data("Param 1"))
  }
}
