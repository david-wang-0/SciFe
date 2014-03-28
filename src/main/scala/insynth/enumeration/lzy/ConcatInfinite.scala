package insynth.enumeration
package lzy

import scala.collection.mutable

import insynth.util.logging._

object ConcatInfinite {

  def apply[T](streams: Seq[Enum[T]]) =
    new ConcatInfinite(streams)

}

class ConcatInfinite[T] (streams: Seq[Enum[T]])
  extends Infinite[T] with HasLogger {
  assert(streams.forall(_.size == -1), "RoundRobbinInfinite should be constructed " +
		"with infinite streams. " +
    "(sizes are %s)".format(streams.map(_.size).distinct))
  
  val streamsArray = streams.toArray
  
  override def apply(ind: Int) = {
    val arrInd = ind % streamsArray.size
    val elInd = ind / streamsArray.size
    streamsArray(arrInd)(elInd)
  }
    
}