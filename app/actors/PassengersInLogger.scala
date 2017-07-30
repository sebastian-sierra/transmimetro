package actors

import java.io.{File, FileWriter}

import akka.actor.{Actor, ActorLogging}
import messages.LogPassengersIn
import models.Passenger

import scala.concurrent._

class PassengersInLogger extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  override def preStart(): Unit = {
    val file = new File("logs/passengers_in")
    if(!file.exists()) {
      file.mkdir()
    }
  }

  def receive: Receive = {
    case LogPassengersIn(station, passengers) => log(station, passengers)
  }

  def log(station: String, passengers: List[Passenger]) = Future {
    val fw = new FileWriter(s"logs/passengers_in/${station.toLowerCase().replaceAll("\\s+","")}.log", true)
    passengers.foreach(passenger => {
      blocking {
        fw.write(s"$passenger\n")
      }
    })
    fw.close()
  }
}
