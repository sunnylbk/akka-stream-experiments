package exp.stream

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import exp.stream.messages.Person

import scala.language.postfixOps
import scala.util.{Success, Failure}

/**
* Created by bharath on 08/02/15.
*/
object SinkEnd extends App {
  implicit val system = ActorSystem("SinkEnd")
  implicit val materializer = ActorFlowMaterializer()
  import system.dispatcher
  import system.log
  
  val serialization = SerializationExtension(system)
  val requestSerializer = serialization.serializerFor(classOf[AddTriples])
  

//  val testInput = List(
//    Person("Bruce", 32),
//    Person("Clark", 33),
//    Person("Stark", 35),
//    Person("Peter", 29),
//    Person("Jack", 28))

  val testInput = List(
    AddTriples(Set(Triple("a", "b", "c"))),
    AddTriples(Set(Triple("d", "e", "f"), Triple("g", "h", "i"))))
  

  //val sink: Sink[Int] = ForeachSink[Int](println)
  val result = Source(testInput)
    .map(requestSerializer.toBinary(_))
    .map(ByteString(_))
    .map(_.toArray)
    .map(requestSerializer.fromBinary(_).asInstanceOf[AddTriples])
    .runFold(Set[AddTriples]()) { case (aggr, next) => aggr + next }
  
  result.onComplete {
    case Success(result) =>
      println(s"Result: " + result)
      system.shutdown()
    case Failure(e) =>
      println("Failure: " + e.getMessage)
      system.shutdown()
  }
}
