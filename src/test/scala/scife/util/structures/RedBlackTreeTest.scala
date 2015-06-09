package scife.util

import structures._

import scala.collection.mutable.ListBuffer

import org.scalatest._
import org.scalatest.prop._
import org.scalacheck.Gen

import scala.language.implicitConversions
import scala.language.postfixOps

class RedBlackTreeTest extends FunSuite with Matchers with PropertyChecks {
  
  test("generate RB trees by insertion") {
    import RedBlackTreeWithOperations._
    import structures.RedBlackTrees.invariant
    
    val allTrees = ListBuffer[Tree](Leaf)
    
    for {
      el <- Gen.choose(1, 10)
    } yield {
      val newTrees = allTrees map { _.insert(el) }
      
      allTrees ++= newTrees
    }
      
    for (tree <- allTrees) {
      invariant(tree) should be (true) 
    }
  }
  
}