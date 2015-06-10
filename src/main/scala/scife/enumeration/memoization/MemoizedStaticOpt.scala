package scife.enumeration
package memoization

import scala.collection.mutable._

import scife.util._

import scala.reflect._

// TODO maybe later
trait MemoizedStaticOpt[T] extends Enum[T] with Memoizable with HasLogger {
  
  implicit val classTagT: ClassTag[T]// = implicitly[ClassTag[T]]
  assert(classTagT != null)

  private[MemoizedStaticOpt] var memoizedValues = new Array[T](this.size)
  private[MemoizedStaticOpt] var memoizedFlags = new Array[Boolean](this.size)
  
  def isMemoized(ind: Int) = memoizedFlags(ind)

  override abstract def apply(ind: Int) = {
    assert(this.size > ind)
    if (!memoizedFlags(ind)) {
      memoizedValues(ind) = super.apply(ind)
      memoizedFlags(ind) = true
    }

    memoizedValues(ind)
  }

  override def clearMemoization {
    memoizedValues = new Array[T](this.size)
    memoizedFlags = new Array[Boolean](this.size)
  }
  
//  def getMemoizedFlags = this.memoizedFlags
//  def getMemoizedValues = this.memoizedValues
  
  def merge(other: MemoizedStaticOpt[T]) = {
    for (ind <- 0 to other.memoizedFlags.length;
      if other.memoizedFlags(ind) && !this.memoizedFlags(ind)) {
      this.memoizedValues(ind) = other.memoizedValues(ind)
      this.memoizedFlags(ind) = true
    }
  }

}