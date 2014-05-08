package ch.epfl.bigdata.btc.crawler.coins.indicators

import scala.collection.mutable.MutableList

import akka.actor.ActorLogging
import akka.actor.ActorRef
import ch.epfl.bigdata.btc.crawler.coins.types._
import ch.epfl.bigdata.btc.types.Registration._

class EMA(dataSource: ActorRef, watched: MarketPairRegistrationOHLC, period: Int, alpha : Double) extends Indicator(dataSource, watched) {

	var values: List[Double] = Nil

	def recompute() {		
		values = Nil ::: (movingSumExponential(ticks.map(_.close).toList, period, alpha))
	}
	

	
	def exponentialMovingAverage(values: List[Double], period: Int, alpha : Double): List[Double] ={
	  Nil ::: (movingSumExponential(values, period, alpha))
	}
	def movingSumExponential(values: List[Double], period: Int, alpha : Double): List[Double] = period match {
		case 0 => throw new IllegalArgumentException
		case 1 => values
		case odd if odd % 2 == 1 => 
		  
		  var halfPeriod = Math.ceil(period.toDouble/2).toInt
		  val listCoeff = exponentialList(halfPeriod, alpha)
		  var finalList : List[Double] = Nil
		  for(i <- 0 to halfPeriod  )
		   finalList ::= (values.drop(i).take(halfPeriod), listCoeff).zipped.map(_ * _).sum / listCoeff.sum		    
		  
		finalList.reverse 
		
		case even => 
		  
		  var halfPeriod = period/2
		  val listCoeff = exponentialList(halfPeriod, alpha)
		  var finalList : List[Double] = Nil
		  for(i <- 0 to halfPeriod )
		   finalList ::= (values.drop(i).take(halfPeriod), listCoeff).zipped.map(_ * _).sum / listCoeff.sum		    
		  
		finalList.reverse 
		
	}
	def exponentialList (period : Int, alpha : Double ): List[Double] = period match{
	  case 0 => throw new IllegalArgumentException
	  case 1 => List(1)
	  case _  => 
	    
	    var list : List[Double]= Nil
	    val element = 1 - alpha
	    var toAdd =0.0
	    for(i <- 0 to (period -1 )){
	      toAdd = Math.pow(element, i)
	      list::= toAdd
	    }
	    
	   list  
	}
	
	
	
}