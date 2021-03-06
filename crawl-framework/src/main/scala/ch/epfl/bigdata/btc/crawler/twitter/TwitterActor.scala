package ch.epfl.bigdata.btc.crawler.twitter

import twitter4j._
import akka.actor.ActorSelection
import akka.actor.Props
import akka.actor.Actor
import ch.epfl.bigdata.btc.crawler
import ch.epfl.bigdata.btc.crawler.btc.FetchRunner
import ch.epfl.bigdata.btc.crawler.coins.DataSource
import akka.actor.ActorRef
import org.joda.time.DateTime
import ch.epfl.bigdata.btc.types.Transfer._
import java.io.BufferedReader
import java.io.InputStreamReader


class TwitterActor(dataSource: ActorRef) extends Actor {


	val config = new twitter4j.conf.ConfigurationBuilder()
	.setOAuthConsumerKey("h7HL6oGtIOrCZN53TbWafg")
	.setOAuthConsumerSecret("irg8l38K4DUrqPV638dIfXvK0UjVHKC936IxbaTmqg")
	.setOAuthAccessToken("77774972-eRxDxN3hPfTYgzdVx99k2ZvFjHnRxqEYykD0nQxib")
	.setOAuthAccessTokenSecret("FjI4STStCRFLjZYhRZWzwTaiQnZ7CZ9Zrm831KUWTNZri")
	.build

	def simpleStatusListener = new StatusListener() {
		def onStatus(status: Status) {
			if (status.getUser().getFollowersCount() < 30) {
			    return
			}
			val tweet = status.getText().replace('\n',' ')
			// send stuff to datasource
			val commands = Array("python","sentiment.py",tweet)
			val p = Runtime.getRuntime().exec(commands)
			
			val stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()))
			val stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()))

			val sentiment = stdInput.readLine()
			
			val intSentiment = sentiment match {
			  case "positive" => 1
			  case "negative" => -1
			  case "neutral" => 0
			}
			
			if (intSentiment == 1)  {
			  println(tweet)
			} else if (intSentiment == -1){
			  System.err.println(tweet)
			} 
			var imagesrc = status.getUser().getProfileImageURL()
			var author = status.getUser().getScreenName()

			dataSource ! new Tweet(new DateTime(status.getCreatedAt().getTime()), tweet, intSentiment, imagesrc, author)

		}
		def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}
		def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}
		def onException(ex: Exception) { ex.printStackTrace }
		def onScrubGeo(arg0: Long, arg1: Long) {}
		def onStallWarning(warning: StallWarning) {}
	}

	def receive() = {
		// DataSource receives a transaction from its fetchers.
	case "start" =>
	val twitterStream = new TwitterStreamFactory(config).getInstance
	twitterStream.addListener(simpleStatusListener)
	twitterStream.filter(new FilterQuery().track(Array("bitcoin", "cryptocurrency", "btc", "bitcoins")))

	}


}
