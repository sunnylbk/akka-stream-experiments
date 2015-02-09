package exp.stream

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.{ActorFlowMaterializer}
import akka.stream.scaladsl.StreamTcp
import akka.stream.scaladsl.{Source, Flow, ForeachSink}
import akka.util.ByteString
import exp.stream.flows.ReusableFlows._
import scala.util.{ Failure, Success }

/**
 * Created by bharath on 07/02/15.
 */
case class Triple(s: String, p: String, o: String)
case class AddTriples(ts: Set[Triple])

object TcpEcho extends App {

  val serverSystem = ActorSystem("server")
  val clientSystem = ActorSystem("client")
  val serverAddress = new InetSocketAddress("127.0.0.1", 6000)
  
  server(serverSystem, serverAddress)
  client(clientSystem, serverAddress)

  def server(system: ActorSystem, serverAddress: InetSocketAddress): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer()

    val handler = ForeachSink[StreamTcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn handleWith Flow[ByteString]
    }

    val binding = StreamTcp(sys).bind(serverAddress)
    val materializedServer = binding.connections.to(handler).run()

    binding.localAddress(materializedServer).onComplete {
      case Success(address) =>
        println("Server started, listening on: " + address)
      case Failure(e) =>
        println(s"Server could not bind to $serverAddress: ${e.getMessage}")
        system.shutdown()
    }
  }
  
  def client(system: ActorSystem, serverAddress: InetSocketAddress): Unit = {
    implicit val sys = system
    import system.dispatcher
    import system.log
    implicit val serializer = ActorFlowMaterializer()
    
    //val testInput = ('a' to 'z').map(ByteString(_))

    val serialization = SerializationExtension(system)
    val requestSerializer = serialization.serializerFor(classOf[AddTriples])

    val testInput = List(
      AddTriples(Set(Triple("a", "b", "c"))),
      AddTriples(Set(Triple("d", "e", "f"), Triple("g", "h", "i"))))

    val result = Source(testInput)
      .via(debug("system is sending"))
      .map(requestSerializer.toBinary(_))
      .map(ByteString(_))
      .via(debug("Serialized representation being sent"))
      .via(StreamTcp().outgoingConnection(serverAddress).flow)
      .via(debug("Serialized representation received")(system))
      .map(_.toArray)
      .map(requestSerializer.fromBinary(_))
      .map(_.asInstanceOf[AddTriples])
      .runFold(Set.empty[AddTriples]) { (acc, in) ⇒ acc + in }

    result.onComplete {
      case Success(result) =>
        println(s"Result: " + result)
        println("Shutting down client")
        system.shutdown()
      case Failure(e) =>
        println("Failure: " + e.getMessage)
        system.shutdown()
    }
  }

}
