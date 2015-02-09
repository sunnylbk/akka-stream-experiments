package exp.stream.flows

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow

object ReusableFlows {

  def debug[G](description: String = "")(implicit system: ActorSystem): Flow[G, G] = Flow[G]
    .map { e =>
    system.log.debug(description + ": " + e)
    e
  }
}
