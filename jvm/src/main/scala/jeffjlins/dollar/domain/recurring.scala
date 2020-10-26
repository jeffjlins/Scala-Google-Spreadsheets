package jeffjlins.dollar.domain

import java.time.temporal.TemporalAdjusters
import java.time.{DayOfWeek, LocalDate, YearMonth}

import cats.data.NonEmptyList
import cats.implicits._
import com.google.api.services.sheets.v4.model.{CellData, ExtendedValue}
import enumeratum._


trait Recurring {
  def name: String
  val start: LocalDate
  val end: Option[LocalDate]
  val when: When
  val range: DayRange
  val min: Double
  val max: Double
  val rowNum: Int
  val months: List[YearMonth]
  def amount(month: YearMonth): Option[Double]
  def adjustedAmount(month: YearMonth): Option[Double]
  def status(month: YearMonth): Option[RecurringStatus]
}

case class When(dayOfWeek: Option[DayOfWeek], nthWeekDay: Option[Int], dayOfMonth: Option[Int], monthOfYear: Option[Int])

case class DayRange(daysBefore: Int, daysAfter: Int, withinMonth: Boolean = false)

sealed trait RecurringStatus extends EnumEntry
object RecurringStatus extends Enum[RecurringStatus] {
  val values = findValues
  case object Pending extends RecurringStatus // blue
  case object Unpaid extends RecurringStatus // red
  case object Fulfilled extends RecurringStatus // green
  case object Late extends RecurringStatus // yellow
  case object Rectified extends RecurringStatus // orange
}

sealed trait RecurringField extends EnumEntry
object RecurringField extends Enum[RecurringField] {
  val values = findValues
  case object Name extends RecurringField
  case object Status extends RecurringField
  case object Start extends RecurringField
  case object End extends RecurringField
  case object When extends RecurringField
  case object Range extends RecurringField
  case object Min extends RecurringField
  case object Max extends RecurringField
}


//=======================

//main fields from sheet or provided
//month fields from sheet or trans

trait RecurringFromSheet extends Recurring {
  val cells: List[DDCell[RecurringField]]

  import scala.language.implicitConversions
  private implicit def extToString(e: ExtendedValue): String = e.getStringValue
  private implicit def extToDecimal(e: ExtendedValue): Double = e.getNumberValue
  private implicit def extToLocalDate(e: ExtendedValue): LocalDate = LocalDate.of(1899, 12, 30).plusDays(e.getNumberValue.longValue()) // LocalDate.of(1899, 12, 30).until(value, ChronoUnit.DAYS).toDouble
  private implicit def extToOptString(e: ExtendedValue): Option[String] = Option(e).map(_.getStringValue)
  private implicit def extToOptDecimal(e: ExtendedValue): Option[Double] = Option(e).map(_.getNumberValue)
  private implicit def extToOptLocalDate(e: ExtendedValue): Option[LocalDate] = Option(e).map(x => LocalDate.of(1899, 12, 30).plusDays(x.getNumberValue.longValue()) )

  private def uev(name: RecurringField): ExtendedValue = cells.find(_.name == name).get.cellData.getUserEnteredValue

  def name: String = uev(RecurringField.Name)
  val start: LocalDate = uev(RecurringField.Start)
  val end: Option[LocalDate] = uev(RecurringField.End)
  val when: When = (uev(RecurringField.When): String).split("/", -1).toList.map(x => if (x.isEmpty) none else x.some) match {
    case (dow :: nwd :: dom :: moy :: Nil) => When(dow.map(x => DayOfWeek.of(x.toInt)), nwd.map(_.toInt), dom.map(_.toInt), moy.map(_.toInt))
    case _ => throw new Exception("Could not parse when expression")
  }
  val range: DayRange = (uev(RecurringField.Range): String).split("/", -1).toList match {
    case (before :: after :: Nil) => DayRange(before.toInt, after.toInt)
    case (before :: after :: within :: Nil) => DayRange(before.toInt, after.toInt, within.toBoolean)
    case _ => throw new Exception("Could not parse day range")
  }
  val min: Double = uev(RecurringField.Min)
  val max: Double = uev(RecurringField.Max)

}

trait RecurringWithTrans extends Recurring {
  val trans: List[Transaction]

  def amount(month: YearMonth): Option[Double] = {
    months.find(_ == month).flatMap { _ =>
      NonEmptyList.fromList(trans.filter(_.dateMonth == month).filter(_.overlay == false).map(_.amount)).map(_.combineAll)
    }
  }
  def adjustedAmount(month: YearMonth): Option[Double] = {
    months.find(_ == month).flatMap { _ =>
      NonEmptyList.fromList(trans.filter(_.dateMonth == month).filter(_.deprecated == false).map(_.amount)).map(_.combineAll)
    }
  }
  def status(month: YearMonth): Option[RecurringStatus] = {
    def onTime(ts: List[Transaction]) = ts.map(_.date).forall { d =>
      // when day paid is before the range it should go into last month but that should be done at rules time through an overlay
      (when.dayOfWeek, when.nthWeekDay, when.dayOfMonth, range.daysAfter, range.withinMonth) match {
        case (Some(dow), Some(nwd), _, da, wm) =>
          val target = month.atDay(1).`with`(TemporalAdjusters.dayOfWeekInMonth(nwd, dow))
          val max = if (YearMonth.from(target.plusDays(da)) != month && wm) month.atEndOfMonth() else target.plusDays(da)
          ts.forall(_.date.isBefore(max))
        case (_, _, Some(dom), da, wm) =>
          NonEmptyList.fromList(ts.map(_.date)).map(_.reduce[LocalDate]((a, b) => if (a.isAfter(b)) a else b)).forall { latest =>
            val max = if (YearMonth.from(month.atDay(dom).plusDays(da)) != month && wm) month.atEndOfMonth() else month.atDay(dom).plusDays(da)
            ts.forall(_.date.isBefore(max))
          }
        case (_, _, _, _, true) =>
          if (ts.isEmpty) false else true
        case _ =>
          true
      }
    }

    val monthInRange = month.isBefore(months.min)
    val paymentMonth = monthInRange && when.monthOfYear.forall(_ == month.getMonthValue)
    val pendingMonth = months.max == month
    val origTrans = trans.filter(_.dateMonth == month).filter(_.overlay == false)
    val origOnTime = onTime(origTrans)
    val origAmt: Double = NonEmptyList.fromList(origTrans.map(_.amount)).map(_.combineAll).getOrElse(0.0)
    val origPaid = origAmt >= min
    val origOverpaid = origAmt > max
    val adjTrans = trans.filter(_.dateMonth == month).filter(_.deprecated == false)
    val adjOnTime = onTime(adjTrans)
    val adjAmt: Double = NonEmptyList.fromList(adjTrans.map(_.amount)).map(_.combineAll).getOrElse(0.0)
    val adjPaid = origAmt >= min
    val adjOverpaid = origAmt > max

    (monthInRange, pendingMonth, origOnTime, origPaid, adjOnTime, adjPaid) match {
      case (false, _, _, _, _, _) => None
      case (_, _, true, true, _, _) => RecurringStatus.Fulfilled.some
      case (_, _, _, false, _, true) => RecurringStatus.Rectified.some
      case (_, true, _, _, _, _) => RecurringStatus.Pending.some
      case (_, _, _, _, false, false) => RecurringStatus.Unpaid.some
      case (_, _, false, true, _, _) => RecurringStatus.Late.some
      case _ => None
    }

  }
}


case class RecurringFromSheetWithTrans(cells: List[DDCell[RecurringField]], rowNum: Int, months: List[YearMonth], trans: List[Transaction]) extends Recurring with RecurringFromSheet with RecurringWithTrans

case class RecurringDirectWithTrans(name: String, start: LocalDate, end: Option[LocalDate], when: When, range: DayRange, min: Double, max: Double, rowNum: Int, months: List[YearMonth], trans: List[Transaction]) extends Recurring with RecurringWithTrans

//====================

case class DDCell[T <: EnumEntry](name: T, originalCellData: CellData, cellData: CellData, colNum: Int, changed: Boolean, modifications: List[CellData => CellData] = Nil) {
  val changeTypes: List[String] = "userEnteredValue" :: "userEnteredFormat" :: Nil
}

object DDCell {
  def create[T <: EnumEntry, E <: Enum[T]](cols: List[String], cells: List[CellData], enumer: E): List[DDCell[T]] = {
    cols.zipWithIndex.map { case (col, i) =>
      DDCell[T](enumer.withName(col), cells(i), cells(i), i, false)
    }
  }
}