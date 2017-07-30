package messages

import models._

case class Tick()

case class FetchStationStatus(stationName: String)
case class StationsStatus(status: Map[String, List[Passenger]])

case class UpdateBoardedPassengers(status: Map[String, Int])
case class StationsCapacitiesUpdated(status: Map[String, Int])

case class LogPassengersIn(station: String, passengers: List[Passenger])
case class LogPassengersOut(station: String, passengers: List[Passenger])