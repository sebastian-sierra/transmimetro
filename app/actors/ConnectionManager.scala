package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import messages.{Subscribe, Unsubscribe}

class ConnectionManager(out: ActorRef, statusUpdater: ActorRef) extends Actor with ActorLogging{

  implicit val ec = context.dispatcher

  override def preStart(): Unit =  {
    log.info("Starting StatusProvider")
    statusUpdater ! Subscribe(out)
  }

  def receive: Receive = {
    case _ =>
  }

  override def postStop():Unit = {
    statusUpdater ! Unsubscribe(out)
  }

}

object ConnectionManager {
  def props(out: ActorRef, statusUpdater: ActorRef) = Props(classOf[ConnectionManager], out, statusUpdater)
}
