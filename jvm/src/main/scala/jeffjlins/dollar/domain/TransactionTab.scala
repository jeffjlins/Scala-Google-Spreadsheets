package jeffjlins.dollar.domain

import java.time.LocalDate

import com.google.api.services.sheets.v4.model.GridData

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

case class TransactionTab(transGridData: GridData, superCategories: Map[String, List[String]]) {

  lazy val (header, rows, rowsAdjusted) = {
    val transData = transGridData.getRowData.asScala.toList
    val headers = transData.head.getValues.asScala.toList.flatMap { cd =>
      Option(cd.getUserEnteredValue).map(_.getStringValue)
    }
    val rows = transData.zipWithIndex.tail.filter(_._1.getValues.asScala.toList(headers.indexOf("Id")).getUserEnteredValue != null).map { case (rd, i) =>
      Transaction(headers, i, rd.getValues.asScala.toList)
    }
    val finalRows = rows.flatMap(x => adjust(x))
    (headers, rows, finalRows)
  }

  private def adjust(t: Transaction): List[Transaction] = {
    t.adjustedAmount match {
      case None => t :: Nil
      case Some(f) =>
        if (f.startsWith("even")) {
          val sliced = f.slice(5, f.length - 1)
          val dist = sliced.split(",").toList.map(_.trim.toInt)
          val cents = (t.amount * 100).toInt
          val part = ((t.amount * 100) / dist.size).toInt
          val diff = cents - part * dist.size
          val diffDist =
            if (diff == 0)
              (1 to dist.size).map(_ => 0)
            else {
              (1 to diff.abs).map { _ =>
                if (diff < 0) -1 else 1
              }.padTo(dist.size, 0)
            }
          val amts = (1 to dist.size).map(_ => part).zip(diffDist).map(x => x._1 + x._2).map(_.toDouble / 100).zip(dist).map(_.swap)
          val res = amts.map { case (monthOffset, amount) =>
            if (monthOffset == 0) t.reinterpret.modifyField(Transaction.Fields.Amount, amount)
            else t.virtualize.modifyField(Transaction.Fields.Date, t.date.plusMonths(monthOffset)).modifyField(Transaction.Fields.Amount, amount)
          }
          res.toList.filter(_.date.isBefore(LocalDate.now()))
        } else throw new Exception("unsupported adjustment")
    }
  }

  lazy val allCats: List[TransactionCat] = allCats(header, rows)
  lazy val allCatsAdjusted: List[TransactionCat] = allCats(header, rowsAdjusted)
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
