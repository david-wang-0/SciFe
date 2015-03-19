package scife
package enumeration
package common.enumdef

import scife.{ enumeration => e }

import util._
import scife.util.logging._
import scife.util._

import org.scalatest._
import org.scalatest.prop._
import org.scalatest.Matchers._
import org.scalacheck.Gen

import scala.language.postfixOps

object RedBlackTreeEnum {

  import Checks._
  import structures._
  import RedBlackTrees._
  import memoization.MemoizationScope

  // constructs enumerator for "simple" red-black trees

  def constructEnumeratorBenchmarkVersion_1 = {
    import RedBlackTrees._
    import dependent._

    val colorsProducer = Depend.memoized(
      (set: Set[Boolean]) => { e.WrapArray(set.toArray) })

    val treesOfSize: Depend[(Int, Range, Set[Boolean], Int), Tree] = Depend.memoized(
      (self: Depend[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
        else if (size == 0 && blackHeight == 1 && colors.contains(true)) e.Singleton(Leaf)
        //            else if (size == 1 && blackHeight == 1 && colors.contains(false)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, false) })
        //            else if (size == 1 && blackHeight == 2 && colors.contains(true)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        //            else if (size == 1) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        else if (size > 0 && blackHeight >= 1) {
          val roots = e.Enum(range)
          val leftSizes = e.WrapArray(0 until size)
          val rootColors = colorsProducer(colors)

          val rootLeftSizePairs = e.Product(leftSizes, roots)
          val rootLeftSizeColorTuples = e.Product(rootLeftSizePairs, rootColors)

          val leftTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
          })

          val rightTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs: Depend[((Int, Int), Boolean), (Tree, Tree)] =
            Product(leftTrees, rightTrees)

          val allNodes =
            memoization.Chain[((Int, Int), Boolean), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
              (p1: ((Int, Int), Boolean), p2: (Tree, Tree)) => {
                val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = (p1, p2)

                Node(leftTree, currRoot, rightTree, rootColor)
              })

          allNodes
        } else e.Empty
      })

    treesOfSize
  }

  def constructEnumeratorBenchmarkTest(implicit ms: MemoizationScope) = {
    import dependent._
    import memoization._

    //    val rootProducer = Depend(
    //      (range: Range) => {
    //        e.WrapArray(range)
    //      })

    val colorsProducer = Depend.memoized(
      (set: Set[Boolean]) => { e.WrapArray(set.toArray) })

    //    val sizeProducer = Depend(
    //      (size: Int) => {
    //        e.WrapArray(0 until size)
    //      })

    val treesOfSize: Depend[(Int, Range, Set[Boolean], Int), Tree] = Depend.memoized(
      (self: Depend[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
        else if (size == 0 && blackHeight == 1 && colors.contains(true)) e.Singleton(Leaf)
        //        else if (size == 1 && blackHeight == 1 && colors.contains(false)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, false) })
        //        else if (size == 1 && blackHeight == 2 && colors.contains(true)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        //        else if (size == 1) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        else if (size > 0 && blackHeight >= 1) {
          val roots = e.Enum(range)
          val leftSizes = e.WrapArray(0 until size)
          val rootColors = colorsProducer(colors)

          val rootLeftSizePairs = e.Product(leftSizes, roots)
          val rootLeftSizeColorTuples = e.Product(rootLeftSizePairs, rootColors)

          val leftTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
          })

          val rightTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs: Depend[((Int, Int), Boolean), (Tree, Tree)] =
            Product(leftTrees, rightTrees)

          val allNodes =
            memoization.Chain[((Int, Int), Boolean), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
              (p1: ((Int, Int), Boolean), p2: (Tree, Tree)) => {
                val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = (p1, p2)

                assert(!(size >= 2 && leftSize == 0 && size - leftSize - 1 == 0))
                assert(!(size >= 2 && leftTree == Leaf && rightTree == Leaf))
                assert(!(leftSize > 0 && leftTree == Leaf), "leftSize=%d, leftTree=Leaf".format(leftSize))
                Node(leftTree, currRoot, rightTree, rootColor)
              })

          allNodes
        } else e.Empty
      })

    treesOfSize
  }

  def constructEnumerator_currentBenchmark(implicit ms: MemoizationScope) = {
    import e.dependent._

    val colorsProducer = Depend.memoized(
      (set: Set[Boolean]) => { e.WrapArray(set.toArray) })

    val treesOfSize: Depend[(Int, Range, Set[Boolean], Int), Tree] = Depend.memoized(
      (self: Depend[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
        else if (size == 0 && blackHeight == 1 && colors.contains(true)) e.Singleton(Leaf)
        //            else if (size == 1 && blackHeight == 1 && colors.contains(false)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, false) })
        //            else if (size == 1 && blackHeight == 2 && colors.contains(true)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        //            else if (size == 1) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
        else if (size > 0 && blackHeight >= 1) {
          val roots = e.Enum(range)
          val leftSizes = e.WrapArray(0 until size)
          val rootColors = colorsProducer(colors)

          val rootLeftSizePairs = e.Product(leftSizes, roots)
          val rootLeftSizeColorTuples = e.Product(rootLeftSizePairs, rootColors)

          val leftTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
          })

          val rightTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor) Set(true, false) else Set(true)
            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs: Depend[((Int, Int), Boolean), (Tree, Tree)] =
            Product(leftTrees, rightTrees)

          val allNodes =
            memoization.Chain[((Int, Int), Boolean), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
              (p1: ((Int, Int), Boolean), p2: (Tree, Tree)) => {
                val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = (p1, p2)

                Node(leftTree, currRoot, rightTree, rootColor)
              })

          allNodes
        } else e.Empty
      })

    treesOfSize
  }

  def constructEnumerator_new(implicit ms: MemoizationScope) = {
    import e.dependent._

    val treesOfSize: Depend[(Int, Range, Range, Int), Tree] = Depend.memoized(
      (self: Depend[(Int, Range, Range, Int), Tree], pair: (Int, Range, Range, Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
        else if (size == 0 && blackHeight == 1 && colors.end >= 1) e.Singleton(Leaf)
        else if (size > 0 && blackHeight >= 1) {
          val roots: Finite[Int] = e.Enum(range)
          val leftSizes: Finite[Int] = e.WrapArray(0 until size)
          val rootColors: Finite[Int] = e.WrapArray(colors.toArray)

          val rootLeftSizePairs = e.Product(leftSizes, roots)
          val rootLeftSizeColorTuples: Finite[((Int, Int), Int)] = e.Product(rootLeftSizePairs, rootColors)

          val leftTrees: Depend[((Int, Int), Int), Tree] = InMap(self, { (par: ((Int, Int), Int)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor == 1) 0 to 1 else 1 to 1
            val childBlackHeight = if (rootColor == 1) blackHeight - 1 else blackHeight
            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
          })

          val rightTrees: Depend[((Int, Int), Int), Tree] = InMap(self, { (par: ((Int, Int), Int)) =>
            val ((leftSize, median), rootColor) = par
            val childColors = if (rootColor == 1) 0 to 1 else 1 to 1
            val childBlackHeight = if (rootColor == 1) blackHeight - 1 else blackHeight
            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs: Depend[((Int, Int), Int), (Tree, Tree)] =
            Product(leftTrees, rightTrees)

          val allNodes =
            memoization.Chain[((Int, Int), Int), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
              (p1: ((Int, Int), Int), p2: (Tree, Tree)) => {
                val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = (p1, p2)

                Node(leftTree, currRoot, rightTree, rootColor == 1)
              })

          allNodes
        } else e.Empty
      })

    treesOfSize
  }
  
  
  def constructEnumerator_concise(implicit ms: MemoizationScope) = {
    import e.dependent._

    val treesOfSize: Depend[(Int, Range, Range, Int), Tree] = Depend.memoized(
      (self: Depend[(Int, Range, Range, Int), Tree], pair: (Int, Range, Range, Int)) => {
        val (size, range, colors, blackHeight) = pair

        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
        else if (size == 0 && blackHeight == 1 && colors.end >= 1) e.Singleton(Leaf)
        else if (size > 0 && blackHeight >= 1) {
          val roots = e.Enum(range)
          val leftSizes = e.WrapArray(0 until size)
          val rootColors = e.WrapArray(colors.toArray)

          val rootLeftSizeColorTuples = e.Product(e.Product(leftSizes, roots), rootColors)

          val leftTrees = InMap(self, { (par: ((Int, Int), Int)) =>
            val childColors = if (par._2 == 1) 0 to 1 else 1 to 1
            val childBlackHeight = if (par._2 == 1) blackHeight - 1 else blackHeight
            (par._1._1, range.start to (par._1._2 - 1), childColors, childBlackHeight)
          })

          val rightTrees = InMap(self, { (par: ((Int, Int), Int)) =>
            val childColors = if (par._2 == 1) 0 to 1 else 1 to 1
            val childBlackHeight = if (par._2 == 1) blackHeight - 1 else blackHeight
            (size - par._1._1 - 1, (par._1._2 + 1) to range.end, childColors, childBlackHeight)
          })

          val leftRightPairs = Product(leftTrees, rightTrees)

          memoization.Chain[((Int, Int), Int), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
            (p1: ((Int, Int), Int), p2: (Tree, Tree)) => Node(p2._1, p1._1._2, p2._2, p1._2 == 1)
          )

        } else e.Empty
      })

    treesOfSize
  }

  //
  //  // constructs enumerator for red-black trees with operations
  //  def constructEnumeratorOtherType = {
  //    import RedBlackTreeWithOperations._
  //
  //    val colorsProducer = new WrapFunctionFin(
  //      (set: Set[Boolean]) => { new WrapArray(set.toArray) })
  //
  //    val treesOfSize = new WrapFunctionFin(
  //      (self: MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
  //        val (size, range, colors, blackHeight) = pair
  //
  //        if (range.size >= size && range.size < 0 || blackHeight < 0) new Empty: MemberFinite[Tree]
  //        else if (size == 0 && blackHeight == 1 && colors.contains(true)) new Singleton(Leaf): MemberFinite[Tree]
  //        else if (size > 0 && blackHeight >= 1) {
  //          val roots = new WrapRange(range)
  //          val leftSizes = new WrapArray(0 until size toArray)
  //          val rootColors = colorsProducer(colors)
  //
  //          val rootLeftSizePairs = new member.ProductFinite(leftSizes, roots)
  //          val rootLeftSizeColorTuples = new member.ProductFinite(rootLeftSizePairs, rootColors)
  //
  //          val leftTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
  //          })
  //
  //          val rightTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
  //          })
  //
  //          val leftRightPairs =
  //            Product(leftTrees, rightTrees)
  //
  //          val allNodes = new ChainFinite(rootLeftSizeColorTuples, leftRightPairs)
  //
  //          val makeTree =
  //            (p: (((Int, Int), Boolean), (Tree, Tree))) => {
  //              val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = p
  //
  //              assert(!(size >= 2 && leftSize == 0 && size - leftSize - 1 == 0))
  //              assert(!(size >= 2 && leftTree == Leaf && rightTree == Leaf))
  //              assert(!(leftSize > 0 && leftTree == Leaf), "leftSize=%d, leftTree=Leaf".format(leftSize))
  //              Node(rootColor, leftTree, currRoot, rightTree)
  //            }
  //
  //          val invertTree = {
  //            (p: Tree) =>
  //              {
  //                val Node(rootColor, leftTree, currRoot, rightTree) = p.asInstanceOf[Node]
  //
  //                (((RedBlackTrees.size(leftTree), currRoot), rootColor), (leftTree, rightTree))
  //              }
  //          }
  //
  //          new Map[(((Int, Int), Boolean), (Tree, Tree)), Tree](allNodes, makeTree, invertTree) with MemberFinite[Tree]: MemberFinite[Tree]
  //        } else new Empty: MemberFinite[Tree]
  //      })
  //
  //    treesOfSize
  //  }
  //
  //  def constructEnumeratorOtherTypeMemoized = {
  //    import RedBlackTreeWithOperations._
  //    import dependent._
  //
  //    val colorsProducer = new WrapFunctionFin(
  //      (set: Set[Boolean]) => { new WrapArray(set.toArray) })
  //
  //    val treesOfSize = new WrapFunctionFin(
  //      (self: MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree],
  //        pair: (Int, Range, Set[Boolean], Int)) => {
  //        val (size, range, colors, blackHeight) = pair
  //
  //        if (range.size >= size && range.size < 0 || blackHeight < 0) new Empty: MemberFinite[Tree]
  //        else if (size == 0 && blackHeight == 1 && colors.contains(true)) new Singleton(Leaf): MemberFinite[Tree]
  //        else if (size > 0 && blackHeight >= 1) {
  //          val roots = new WrapRange(range)
  //          val leftSizes = new WrapArray(0 until size toArray)
  //          val rootColors = colorsProducer(colors)
  //
  //          val rootLeftSizePairs = new member.ProductFinite(leftSizes, roots)
  //          val rootLeftSizeColorTuples = new member.ProductFinite(rootLeftSizePairs, rootColors)
  //
  //          val leftTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
  //          })
  //
  //          val rightTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
  //          })
  //
  //          val leftRightPairs =
  //            Product(leftTrees, rightTrees)
  //
  //          val allNodes = new ChainFinite(rootLeftSizeColorTuples, leftRightPairs)
  //
  //          val makeTree =
  //            (p: (((Int, Int), Boolean), (Tree, Tree))) => {
  //              val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = p
  //
  //              assert(!(size >= 2 && leftSize == 0 && size - leftSize - 1 == 0))
  //              assert(!(size >= 2 && leftTree == Leaf && rightTree == Leaf))
  //              assert(!(leftSize > 0 && leftTree == Leaf), "leftSize=%d, leftTree=Leaf".format(leftSize))
  //              Node(rootColor, leftTree, currRoot, rightTree)
  //            }
  //
  //          val invertTree = {
  //            (p: Tree) =>
  //              {
  //                val Node(rootColor, leftTree, currRoot, rightTree) = p.asInstanceOf[Node]
  //
  //                (((RedBlackTrees.size(leftTree), currRoot), rootColor), (leftTree, rightTree))
  //              }
  //          }
  //
  //          new Map[(((Int, Int), Boolean), (Tree, Tree)), Tree](allNodes, makeTree, invertTree) with member.memoization.Memoized[Tree] with e.memoization.Memoized[Tree] with MemberFinite[Tree]: MemberFinite[Tree]
  //        } else new Empty: MemberFinite[Tree]
  //      }) with e.memoization.dependent.Memoized[(Int, Range, Set[Boolean], Int), Tree]
  //
  //    treesOfSize
  //  }

  //  def constructEnumeratorOtherTypeMemoizedBlackHeight = {
  //    import RedBlackTreeWithOperations._
  //
  //    val colorsProducer = new WrapFunctionFin(
  //      (set: Set[Boolean]) => { new WrapArray(set.toArray) })
  //
  //    val treesOfSize = new WrapFunctionFin(
  //      (self: MemberDependFinite[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
  //        val (size, range, colors, blackHeight) = pair
  //
  //        if (range.size >= size && range.size < 0 || blackHeight < 0) new Empty: MemberFinite[Tree]
  //        else if (size == 0 && blackHeight == 1 && colors.contains(true)) new Singleton(Leaf): MemberFinite[Tree]
  //        else if (size > 0 && blackHeight >= 1) {
  //          val roots = new WrapRange(range)
  //          val leftSizes = new WrapArray(0 until size toArray)
  //          val rootColors = colorsProducer(colors)
  //
  //          val rootLeftSizePairs = new member.ProductFinite(leftSizes, roots)
  //          val rootLeftSizeColorTuples = new member.ProductFinite(rootLeftSizePairs, rootColors)
  //
  //          val leftTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
  //          })
  //
  //          val rightTrees = new InMapFin(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
  //          })
  //
  //          val leftRightPairs =
  //            Product(leftTrees, rightTrees)
  //
  //          val allNodes = new ChainFinite(rootLeftSizeColorTuples, leftRightPairs)
  //
  //          val makeTree =
  //            (p: (((Int, Int), Boolean), (Tree, Tree))) => {
  //              val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = p
  //
  //              assert(!(size >= 2 && leftSize == 0 && size - leftSize - 1 == 0))
  //              assert(!(size >= 2 && leftTree == Leaf && rightTree == Leaf))
  //              assert(!(leftSize > 0 && leftTree == Leaf), "leftSize=%d, leftTree=Leaf".format(leftSize))
  //              Node(rootColor, leftTree, currRoot, rightTree)
  //            }
  //
  //          val invertTree = {
  //            (p: Tree) =>
  //              {
  //                val Node(rootColor, leftTree, currRoot, rightTree) = p.asInstanceOf[Node]
  //
  //                (((RedBlackTrees.size(leftTree), currRoot), rootColor), (leftTree, rightTree))
  //              }
  //          }
  //
  //          new Map[(((Int, Int), Boolean), (Tree, Tree)), Tree](allNodes, makeTree, invertTree) with MemberFinite[Tree] with e.memoization.Memoized[Tree] with Memoized[Tree]: MemberFinite[Tree]
  //        } else new Empty: MemberFinite[Tree]
  //      }) with e.memoization.dependent.Memoized[(Int, Range, Set[Boolean], Int), Tree]
  //
  //    treesOfSize
  //  }
  //
  //  def constructEnumeratorNormal = {
  //    import e.dependent._
  //
  //    val colorsProducer = Depend(
  //      (set: Set[Boolean]) => { e.WrapArray(set.toArray) })
  //
  //    val treesOfSize: Depend[(Int, Range, Set[Boolean], Int), Tree] = Depend(
  //      (self: Depend[(Int, Range, Set[Boolean], Int), Tree], pair: (Int, Range, Set[Boolean], Int)) => {
  //        val (size, range, colors, blackHeight) = pair
  //
  //        if (range.size >= size && range.size < 0 || blackHeight < 0) e.Empty
  //        else if (size == 0 && blackHeight == 1 && colors.contains(true)) e.Singleton(Leaf)
  //        //        else if (size == 1 && blackHeight == 1 && colors.contains(false)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, false) })
  //        //        else if (size == 1 && blackHeight == 2 && colors.contains(true)) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
  //        //        else if (size == 1) e.WrapArray(range map { v => Node(Leaf, v, Leaf, true) })
  //        else if (size > 0 && blackHeight >= 1) {
  //          val roots = e.Enum(range)
  //          val leftSizes = e.WrapArray(0 until size)
  //          val rootColors = colorsProducer(colors)
  //
  //          val rootLeftSizePairs = e.Product(leftSizes, roots)
  //          val rootLeftSizeColorTuples = e.Product(rootLeftSizePairs, rootColors)
  //
  //          val leftTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (leftSize, range.start to (median - 1), childColors, childBlackHeight)
  //          })
  //
  //          val rightTrees: Depend[((Int, Int), Boolean), Tree] = InMap(self, { (par: ((Int, Int), Boolean)) =>
  //            val ((leftSize, median), rootColor) = par
  //            val childColors = if (rootColor) Set(true, false) else Set(true)
  //            val childBlackHeight = if (rootColor) blackHeight - 1 else blackHeight
  //            (size - leftSize - 1, (median + 1) to range.end, childColors, childBlackHeight)
  //          })
  //
  //          val leftRightPairs: Depend[((Int, Int), Boolean), (Tree, Tree)] =
  //            Product(leftTrees, rightTrees)
  //
  //          val allNodes =
  //            Chain[((Int, Int), Boolean), (Tree, Tree), Node](rootLeftSizeColorTuples, leftRightPairs,
  //              (p1: ((Int, Int), Boolean), p2: (Tree, Tree)) => {
  //                val (((leftSize, currRoot), rootColor), (leftTree, rightTree)) = (p1, p2)
  //
  //                assert(!(size >= 2 && leftSize == 0 && size - leftSize - 1 == 0))
  //                assert(!(size >= 2 && leftTree == Leaf && rightTree == Leaf))
  //                assert(!(leftSize > 0 && leftTree == Leaf), "leftSize=%d, leftTree=Leaf".format(leftSize))
  //                Node(leftTree, currRoot, rightTree, rootColor)
  //              })
  //
  //          allNodes
  //        } else e.Empty
  //      })
  //
  //    treesOfSize
  //  }

}
