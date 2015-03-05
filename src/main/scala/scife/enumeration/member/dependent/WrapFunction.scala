package scife.enumeration
package member
package dependent

import scife.util._

class WrapFunction[I, O](val producerFunction: (MemberDepend[I, O], I) => Member[O])
	extends MemberDepend[I, O] with HasLogger with Serializable {
  
  override type EnumSort[A] = Member[A]
  
  val partiallyApplied = producerFunction(this, _: I)
  
  def this(producerFunction: I => Member[O]) =
    this( (td: MemberDepend[I, O], i: I) => producerFunction(i) )

  override def getEnum(parameter: I) =
    producerFunction(this, parameter)
  
}

class WrapFunctionFin[I, O](val producerFunction:
  (MemberDependFinite[I, O], I) => MemberFinite[O])
  extends MemberDependFinite[I, O] with HasLogger with Serializable {
  
  override type EnumSort[A] = MemberFinite[A]
  
  val partiallyApplied = producerFunction(this, _: I)
  
  def this(producerFunction: I => MemberFinite[O]) =
    this( (td: MemberDependFinite[I, O], i: I) => producerFunction(i) )

  override def getEnum(parameter: I) =
    producerFunction(this, parameter)
  
}