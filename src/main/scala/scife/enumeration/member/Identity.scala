package scife.enumeration
package member

import scife.{ enumeration => e }

trait Identity extends MemberInfinite[Int] {
  
  override def member(ind: Int) = true
  
}

class WrapRange(range: Range) extends e.WrapRange(range) with MemberFinite[Int] {
  
  override def member(el: Int) =
    range.start <= el && el <= range.end
  
}