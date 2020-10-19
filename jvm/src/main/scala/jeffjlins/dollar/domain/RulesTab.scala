package jeffjlins.dollar.domain

import java.time.LocalDate

import com.google.api.services.sheets.v4.model.{CellData, CellFormat, GridData, GridRange, Request, RowData, UpdateCellsRequest}
import jeffjlins.dollar.util.Utils

import scala.jdk.CollectionConverters._

case class RulesTab(rulesGridData: GridData) {

  private lazy val (header, rules, actions) = {
    val rulesData = rulesGridData.getRowData.asScala.toList
    val headers = rulesData.head.getValues.asScala.toList.flatMap { cd =>
      Option(cd.getUserEnteredValue).map(_.getStringValue)
    }
    val allRows: List[List[CellData]] = rulesData.tail.filterNot(_.getValues == null).map(_.getValues.asScala.toList)
    val rows = allRows.filter(_(headers.indexOf("Id")).getUserEnteredValue != null)
    val ruleList: List[Rule] = rows.filter(_(headers.indexOf("Type")).getUserEnteredValue.getStringValue == "Rule").map { cells =>
      (cells(headers.indexOf("Form")).getUserEnteredValue.getStringValue) match {
        case "Or Expression" => OrExpressionRule(headers, cells)
        case x => throw new Exception("Rule type of " + x + " not recognized")
      }
    }
    val actionList: List[Action] = rows.filter(_(headers.indexOf("Type")).getUserEnteredValue.getStringValue == "Action").map { cells =>
      (cells(headers.indexOf("Form")).getUserEnteredValue.getStringValue) match {
        case "Set Fields" => SetFieldsAction(headers, cells, Some(new CellFormat().setBackgroundColor(Utils.hexToColor("#eaf1dd"))))
        case "Recurring Entry" => RecurringAction(headers, cells)
        case x => throw new Exception("Rule type of " + x + " not recognized")
      }
    }
    (headers, ruleList, actionList)
  }

  val transMutations: List[TransMutateAction] = actions.flatMap {
    case (x: TransMutateAction) => Some(x)
    case _ => None
  }

  val recurringQualifiers: List[RecurringAction] = actions.flatMap {
    case (x: RecurringAction) => Some(x)
    case _ => None
  }

  val transMutationOps: List[TransMutateRuleAction] = createTransMutationOps(transMutations, rules)
  protected def createTransMutationOps(mutations: List[TransMutateAction], allRules: List[Rule]) = mutations.groupBy(_.group).map(a => TransMutateRuleAction(allRules.find(_.group == a._1).get, a._1, a._2)).toList

  def writerModelTrans(sheetId: Int, trans: List[Transaction]): List[Request] = writerModelTrans(sheetId, trans, transMutationOps)
  protected def writerModelTrans(sheetId: Int, trans: List[Transaction], mutations: List[TransMutateRuleAction]): List[Request] = {
    val changes = mutations.flatMap(_.changeSet(trans)).groupBy(_.id).map(_._2.head).toList
    changes.flatMap { t =>
      t.changedFields.map { dc =>
        val req = new UpdateCellsRequest()
          .setRows(List(new RowData().setValues(List(dc.cellData).asJava)).asJava)
          .setFields(dc.changeTypes.mkString(","))
          .setRange(
            new GridRange()
              .setSheetId(sheetId)
              .setStartColumnIndex(dc.colNum)
              .setEndColumnIndex(dc.colNum + 1)
              .setStartRowIndex(t.rowNum.get)
              .setEndRowIndex(t.rowNum.get + 1)
          )
        val r = new Request().setUpdateCells(req)
        //println(Utils.requestToJson(r))
        r
      }
    }
  }

}
