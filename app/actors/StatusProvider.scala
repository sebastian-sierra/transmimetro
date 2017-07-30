package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class StatusProvider(out: ActorRef) extends Actor with ActorLogging{

  var statusUpdater: ActorRef = _
  var stationStatus: ActorRef = _

  implicit val ec = context.dispatcher

  override def preStart(): Unit =  {
    log.info("Starting StatusProvider")

    stationStatus = context.actorOf(Props[StationStatus], name = "stationStatus")
    statusUpdater = context.actorOf(StatusUpdater.props(stationStatus, out), name = "statusUpdater")
  }

  def receive: Receive = {
    case _ =>
  }

}

object StatusProvider {
  def props(out: ActorRef) = Props(classOf[StatusProvider], out)
}
