package models

case class MetroCar(id: Int, departureStation: String, destination: String, minutesFromDeparture: Double, capacity: Int)

case class Passenger(time: String, destination: String) {
  override def toString: String = {
    s"$time; $destination;"
  }
}

case class Schedule(trainId: Int, departureStation: String, departureTime: String, destination: String)

object DestinationChecker {
  val line1: List[String] = List(
    "Portal americas",
    "Calle 42 sur",
    "Carrera 80",
    "Kennedy",
    "Avenida Boyaca",
    "Carrera 68",
    "Carrera 50",
    "NQS",
    "Narino",
    "Calle 1"
  )

  val line2: List[String] = List(
    "Calle 1",
    "Calle 10",
    "Calle 26",
    "Calle 45",
    "Calle 63",
    "Calle 72"
  )

  val line3 = (line1:::line2).distinct

  val lines = List(line1, line2, line1.reverse, line2.reverse, line3, line3.reverse)

  def currentLine(currentStation: String, destination: String): List[String] = {
    lines
      .find { line =>
        line.last == destination && line.contains(currentStation)
      }.getOrElse(List.empty)
      .dropWhile(_ != currentStation)
  }

  def isGoodForPassenger(currentStation: String, destination: String, passengerDestination: String): Boolean = {
    currentLine(currentStation, destination).contains(passengerDestination)
  }


}