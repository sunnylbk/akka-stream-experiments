package exp.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import exp.stream.messages.Person
import exp.stream.server.TcpServer
import exp.stream.flows.ReusableFlows._

/**
 * Created by bharath on 08/02/15.
 */
object StreamApp extends App {
  private[this] val stream = new StreamApp
  stream.start
}

class StreamApp(address: String = "127.0.0.1",
                  port: Int = 6000,
                  system: ActorSystem = ActorSystem("StreamTest")) extends TcpServer(address, port, system) {
  val requestSerializer = serialization.serializerFor(classOf[Person])
  
  val flow = Flow[ByteString]
    .via(debug("Serialized representation received")(system))
    .map(_.toArray)
    .map(requestSerializer.fromBinary(_))
    .map { request =>
      request match {
        case x: Person => log.debug("Got request: " + x); x
      }
    }
    .map(requestSerializer.toBinary(_))
    .map(ByteString(_))
}
