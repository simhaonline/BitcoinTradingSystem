package ch.epfl.bigdata.btc.crawler.coins.indicators

import scala.collection.mutable.MutableList
import akka.actor.ActorLogging
import akka.actor.ActorRef
import ch.epfl.bigdata.btc.crawler.coins.types._
import ch.epfl.bigdata.btc.types.Registration._
import com.github.nscala_time.time.Imports._
import ch.epfl.bigdata.btc.types.Transfer._
import ch.epfl.bigdata.btc.types.Indicator._
import org.joda.time.DateTime

class EMA(dataSource: ActorRef, watched: MarketPairRegistrationOHLC, period: Int, alpha: Double) extends Indicator[Points](dataSource, watched, 10000) {

  var values: List[Double] = Nil
  var time: List[Long] = Nil

  def recompute() {
    values = Nil ::: (exponentialMovingAverage(ticks.map(_.close).toList, period, alpha))
    time = ticks.map(_.date.getMillis()).toList
  }
  
  def getResult() = Points(EMA, values zip time)
  

  def exponentialMovingAverage(values: List[Double], period: Int, alpha: Double): List[Double] = {
    Nil ::: (movingSumExponential(values, period, alpha))
  }
  def movingSumExponential(values: List[Double], period: Int, alpha: Double): List[Double] = period match {
    case 0 => throw new IllegalArgumentException
    case 1 => values
    case odd if odd % 2 == 1 =>

      var halfPeriod = Math.ceil(period.toDouble / 2).toInt
      val listCoeff = exponentialList(halfPeriod, alpha)
      var finalList: List[Double] = Nil
      for (i <- 0 to halfPeriod)
        finalList ::= (values.drop(i).take(halfPeriod), listCoeff).zipped.map(_ * _).sum / listCoeff.sum

      finalList.reverse

    case even =>

      var halfPeriod = period / 2
      val listCoeff = exponentialList(halfPeriod, alpha)
      var finalList: List[Double] = Nil
      for (i <- 0 to halfPeriod)
        finalList ::= (values.drop(i).take(halfPeriod), listCoeff).zipped.map(_ * _).sum / listCoeff.sum

      finalList.reverse

  }
  def exponentialList(period: Int, alpha: Double): List[Double] = period match {
    case 0 => throw new IllegalArgumentException
    case 1 => List(1)
    case _ =>

      var list: List[Double] = Nil
      val element = 1 - alpha
      var toAdd = 0.0
      for (i <- 0 to (period - 1)) {
        toAdd = Math.pow(element, i)
        list ::= toAdd
      }

      list
  }

  def receiveOther(a: Any, ar: ActorRef) {
    a match {
      case _ => println("Class:EMA, received unknown data")
    }
  }
  def envellopAbove(values: List[Double], percent: Double): List[Double] = {
    values.map(_ * (1 + percent))

  }
  def envellopBelow(values: List[Double], percent: Double): List[Double] = {
    values.map(_ * (1 - percent))

  }
  /*
  *  ind represents the list you want as indicator (i.e. the long-term MA)
  *  The value -1 indicates that the price should go down and
  *  a value of 1 that the price will go up.
  */

  def tradeSignalCO(values: List[Double], ind: List[Double]): Int = {

    if (ind.last >= values.last && ind.take(ind.length - 1).last < values.take(values.length - 1).last) {
      -1
    } else if (ind.last <= values.last && ind.take(ind.length - 1).last > values.take(values.length - 1).last) {
      1
    } else
      0
  }

  /*
	 * For this function the percent is the size of the envellope you want to have,
	 * In this case, a value of 1 would state that the price will go up, as
	 * a value of -1 indicates that the price will go down.	 * 
	 */
  def tradeSignalEnv(values: List[Double], percent: Double): Int = {
    val envA = values.map(_ * (1 + percent))
    val envB = values.map(_ * (1 - percent))

    if (envA.last >= values.last && envA.take(envA.length - 1).last < values.take(values.length - 1).last) {
      1
    } else if (envB.last <= values.last && envB.take(envB.length - 1).last > values.take(values.length - 1).last) {
      -1
    } else
      0
  }

  def trans(ma : List[Double], transactions : List [(Int, Double, Double)], signal : Int, 
	    maxBTC : Double, actualPrice : Double):List[(Int, Double, Double)]={
	  
	  val actual = ma.last
	  var newTrans = transactions
	  val min = ma.min
	  val max = ma.max
	  var price= 0.0
	  var btc = 0.0

	  if(signal == 1 && actual == min){
	    
		 btc = (1- min/max)*maxBTC
	  }
	  else if (signal == -1 && actual == max){
	        
		btc = (1- min/max)*maxBTC 
	  }
	  price = btc * actualPrice
	 (signal, price, btc) :: newTrans
	}
	
}