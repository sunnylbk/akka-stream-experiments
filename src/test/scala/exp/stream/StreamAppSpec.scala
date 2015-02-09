package exp.stream

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.{OverflowStrategy, ActorFlowMaterializer}
import akka.stream.scaladsl.{FlowGraph, Flow, StreamTcp, Source}
import akka.util.ByteString
import exp.stream.messages.Person
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import exp.stream.flows.ReusableFlows._

import scala.util.{Failure, Success}

class StreamAppSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val streamApp = new StreamApp
  val startFuture = streamApp.start

  implicit val system = ActorSystem("test")
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()
  val log = system.log

  val serialization = SerializationExtension(system)
  val requestSerializer = serialization.serializerFor(classOf[Person])

  val testInput: Source[Person] = Source(List(
    Person("Bruce", 32),
    Person("Clark", 33),
    Person("Stark", 35))
  )
  
  "Stream App" should "serialize/deserialize messages successfully" in {
    startFuture.onSuccess {
      case address =>
        val result = testInput
          .via(debug("system is sending"))
          .buffer(10, OverflowStrategy.backpressure)
          .map(requestSerializer.toBinary(_))
          .map(ByteString(_))
          .via(debug("Serialized representation being sent"))
          .via(StreamTcp().outgoingConnection(address).flow)
          .map(_.toArray)
          .map(requestSerializer.fromBinary(_))
          .map(_.asInstanceOf[Person])
          .runFold(Set[Person]()) { case (aggr, next) => aggr + next}

        result.onComplete {
          case Success(result) =>
            log.debug(s"Result: " + result)
            result should be(Set(Person("Bruce", 32), Person("Clark", 33), Person("Stark", 35)))
          case Failure(e) =>
            log.debug("Failure: " + e.getMessage)
            assert(false)
        }
    }
  }

  override def afterAll = {
    super.afterAll
    Thread.sleep(3000)
    log.debug("Shutting down system and server.")
    system.shutdown
    streamApp.sys.shutdown
  }
}
