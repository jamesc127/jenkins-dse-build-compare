package com.datastax

import scala.collection.mutable.ArrayBuffer

class BuildMath {

  def meanStd(x: ArrayBuffer[BigDecimal]): (BigDecimal, BigDecimal) ={

    @scala.annotation.tailrec
    def meanStd(
                 x: ArrayBuffer[BigDecimal],
                 mu: BigDecimal,
                 Q: BigDecimal,
                 count: Int): (BigDecimal, BigDecimal) =
      if (count >= x.length) (mu,Math.sqrt(Q.doubleValue()/x.length))
      else {
        val newCount = count + 1
        val newMu = x(count)/newCount + mu * (1.0 - 1.0/newCount)
        val newQ = Q + (x(count) - mu)*(x(count) - newMu)
        meanStd(x, newMu, newQ, newCount)
      }
    meanStd(x, 0.0, 0.0, 0)
  }

  def stdevCheck (stdev: Double, mean: Double, values: ArrayBuffer[BigDecimal], tolerance: Double, abvStdev: Boolean) = {
    var aboveStdev  = 0
    var belowStdev  = 0
    var withinStdev = 0
    val valuesIter  = values.toIterator
    while (valuesIter.hasNext) {
      val vNext = valuesIter.next()
      if (vNext.doubleValue() <= (mean+stdev) && vNext.doubleValue() >= (mean-stdev)) withinStdev += 1
      else if (vNext.doubleValue() < (mean-stdev)) belowStdev += 1
      else if (vNext.doubleValue() > (mean+stdev)) aboveStdev += 1
    }
    def over() = if ((aboveStdev.toFloat/values.length) > tolerance) true else false
    def under() = if ((belowStdev.toFloat/values.length) > tolerance) true else false

    if (abvStdev) Map("failure" -> over(), "value" -> (aboveStdev.toFloat/values.length))
    else Map("failure" -> under(), "value" -> (belowStdev.toFloat/values.length))
  }

}
