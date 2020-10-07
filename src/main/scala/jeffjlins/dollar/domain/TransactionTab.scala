package jeffjlins.dollar.domain

import com.google.api.services.sheets.v4.model.GridData

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

case class TransactionTab(transGridData: GridData, superCategories: Map[String, List[String]]) {
//  lazy val transTabApi = {
//    val sheets = transFile.transFileApi.getSheets.asScala.toList
//    sheets.find(_.getProperties.getTitle == TransSheetFile.tabs.transactions).get
//  }

  lazy val (header, rows) = {
    val transData = transGridData.getRowData.asScala.toList
    val headers = transData.head.getValues.asScala.toList.flatMap { cd =>
      Option(cd.getUserEnteredValue).map(_.getStringValue)
    }
    val rows = transData.zip(1 to transData.length).tail.filter(_._1.getValues.asScala.toList(headers.indexOf("Id")).getUserEnteredValue != null).map { case (rd, i) =>
      Transaction(headers, i, rd.getValues.asScala.toList)
    }
    (headers, rows)
  }

  lazy val allCats: List[TransactionCat] = allCats(header, rows)
  protected def allCats(headerNames: List[String], transactions: List[Transaction]): List[TransactionCat] = {
    val superCatIndex: Map[String, String] = superCategories.flatMap(sc => sc._2.map(c => c -> sc._1))
    val catTreeLvl2 = transToCatTree(headerNames, superCatIndex, transactions.groupBy(_.category).map(kv => createCatName(kv._1) -> kv._2))
    val catTreeTopLevel = superCategories.map { case (superCat, catNames) =>
      TransactionCat(superCat, superCat, "", None, headerNames, Nil, catNames.map(c => catTreeLvl2.find(_.name == c).get))
    }.toList
    def untreeSubCats(cat: TransactionCat): List[TransactionCat] = cat :: cat.cats.flatMap(untreeSubCats)
    catTreeTopLevel.flatMap(untreeSubCats)
  }

  @tailrec
  private def transToCatTree(headerNames: List[String], superCatIndex: Map[String, String], trans: Map[CatName, List[Transaction]], cats: List[TransactionCat] = Nil): List[TransactionCat] = {
    if (trans.isEmpty && cats.map(_.parentPath).forall(_.isEmpty)) cats
    else {
      val catsByParent: Map[CatName, List[TransactionCat]] = cats.groupBy(_.parentPath).map { case (op, li) => (createCatName(op.get), li) }
      val allParents: List[CatName] = (catsByParent.keys ++ trans.keys).toList.distinctBy(_.path)
      val depth = allParents.map(_.depth).max

      val parentCats = allParents.filter(_.depth == depth).map { catName =>
        val matchingTrans = trans.find(_._1.path == catName.path).map(_._2).getOrElse(Nil)
        val matchingCats = catsByParent.find(_._1.path == catName.path).map(_._2).getOrElse(Nil)
        val superCat = superCatIndex(superCatIndex.keys.find(k => catName.path.startsWith(k)).get)
        TransactionCat(catName.name, superCat, catName.path, catName.parentPath, headerNames, matchingTrans, matchingCats)
      }

      transToCatTree(headerNames, superCatIndex, trans.filterNot(_._1.depth == depth), parentCats)
    }
  }

  private def createCatName(path: String) = {
    val cleanPath = Option(path).map(p => if (p.startsWith("/")) p.tail else p).map(p => if (p.endsWith("/")) p.dropRight(1) else p).get
    val lastSlash = cleanPath.lastIndexOf("/")
    if (lastSlash < 0) CatName(path, path, None, cleanPath.count(_ == '/'))
    else CatName(cleanPath.drop(lastSlash + 1), cleanPath, Some(cleanPath.take(lastSlash)), cleanPath.count(_ == '/'))
  }
}
