package jeffjlins.dollar.domain

import java.time.LocalDate

import com.google.api.services.sheets.v4.model.GridData

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import cats.syntax.all._

case class AssetsTab(assetGridData: GridData) {

  private lazy val (header, assetList) = {
    val assetData = assetGridData.getRowData.asScala.toList
    val headers = assetData.head.getValues.asScala.toList.flatMap { cd =>
      Option(cd.getUserEnteredValue).map(_.getStringValue)
    }
    val rows = assetData.zip(1 to assetData.length).tail.filter(_._1.getValues.asScala.toList(headers.indexOf("Id")).getUserEnteredValue != null).map { case (rd, i) =>
      Asset(headers, i, rd.getValues.asScala.toList)
    }
    (headers, rows)
  }

  lazy val allAssets = assetList
  lazy val allAssetCats = createAssetCats(assetList)

  private def createAssetCats(assets: List[Asset]) = {
    assets.groupBy(_.category).map(x => AssetCategory(x._1, x._2, x._2.head.useTransactions)).toList
  }

  def convertAmount(asset: Asset, date: LocalDate) = {
    asset.amount
  }

}
