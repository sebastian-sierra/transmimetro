package models

case class MetroCar(id: Int, currentStation: String, capacity: Int)

case class Passenger(time: String, destination: String)

case class Schedule(trainId: Int, departureStation: String, departureTime: String, destination: String)