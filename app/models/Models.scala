package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MetroCar(id: Int, departureStation: String, minutesFromDeparture: Long, capacity: Int)

case class Passenger(time: String, destination: String)

case class Schedule(trainId: Int, departureStation: String, departureTime: String, destination: String)

