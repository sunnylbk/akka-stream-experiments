package exp.stream.server

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Flow, ForeachSink, StreamTcp}
import akka.util.ByteString

import scala.util.{Failure, Success}


abstract class TcpServer(address: String = "127.0.0.1",
                          port: Int = 6000,
                          system: ActorSystem = ActorSystem("TcpServer")) {

  implicit val sys = system
  import system.dispatcher
  implicit val materializer = ActorFlowMaterializer()
  val log = system.log
  
  val flow: Flow[ByteString, ByteString]
  
  val serialization = SerializationExtension(system)
  
  val serverAddress = new InetSocketAddress(address, port)
  
  val handler = ForeachSink[StreamTcp.IncomingConnection] { conn => 
    log.debug("Client connected from: " + conn.remoteAddress)
    conn.handleWith(flow)
  }
  
  val binding = StreamTcp(system).bind(serverAddress)
  val materlializedServer = binding.connections.to(handler).run()
  
  def start = {
    val startFuture = binding.localAddress(materlializedServer)
    startFuture.onComplete {
      case Success(address) => 
        log.debug("Server started, listening on: " + address)
      case Failure(e) =>
        log.debug(s"Server could not bind to $serverAddress. Reason: ${e.getMessage}")
        system.shutdown
    }
    startFuture
  }
}
