package models

case class MetroCar(id: Int, departureStation: String, minutesFromDeparture: Long, capacity: Int)

case class Passenger(time: String, destination: String) {
  override def toString: String = {
    s"$time; $destination;"
  }
}

case class Schedule(trainId: Int, departureStation: String, departureTime: String, destination: String)

