package actors

import java.io.{File, FileWriter}

import akka.actor.{Actor, ActorLogging}
import messages.LogPassengersOut
import models.Passenger

import scala.concurrent._

class PassengersOutLogger extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

  override def preStart(): Unit = {
    val file = new File("logs/passengers_out")
    if(!file.exists()) {
      file.mkdir()
    }
  }

  def receive: Receive = {
    case LogPassengersOut(station, passengers) => log(station, passengers)
  }

  def log(station: String, passengers: List[Passenger]) = Future {
    val fw = new FileWriter(s"logs/passengers_out/${station.toLowerCase().replaceAll("\\s+","")}.log", true)
    passengers.foreach(passenger => {
      blocking {
        fw.write(s"$passenger\n")
      }
    })
    fw.close()
  }
}
