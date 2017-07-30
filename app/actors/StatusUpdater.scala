package actors

import java.io.File
import java.time.{Duration, LocalTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.actor._
import messages._
import models._
import play.api.libs.json.{JsValue, Json, Writes}

import scala.concurrent.duration._

class StatusUpdater(stationStatusActor: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  var metroCars = Map.empty[Int, MetroCar]
  var metroCarsCapacities = Map.empty[Int, List[Passenger]]
  var passengersThatBoarded = Map.empty[String, Int]
  var schedules = List.empty[Schedule]


  implicit val executionContext = context.dispatcher
  val tick = context.system.scheduler.schedule(500 millis, 6 seconds, self, Tick)


  override def preStart(): Unit = {
    log.info("Starting StationStatus")
    //Load schedules from file
    loadSchedules()
    super.preStart()
  }

  override def postStop(): Unit = tick.cancel()

  def receive: Receive = {

    case Tick =>
      updateMetroCarLocations()
      log.info("Fetching station status")
      stationStatusActor ! FetchStationStatus

    case StationsStatus(status) =>
      log.info("Updated station status received {}", status.mapValues(_.size))
      updateCapacities(status)
      stationStatusActor ! UpdateBoardedPassengers(passengersThatBoarded)
      passengersThatBoarded = Map.empty

    case StationsCapacitiesUpdated(stationCapacities) =>
      log.info("Updated station capacities received")
      out ! makeJsonResponse(stationCapacities)
      log.info("Json sent to client")

  }

  def updateMetroCarLocations(): Unit = {

    val now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES) //0401

    schedules
      .filter { schedule =>
        val scheduleTime = LocalTime
          .parse(schedule.departureTime, DateTimeFormatter.ofPattern("HHmm")) //0400
        val lowerBoundTime = now.minusMinutes(4) //0357

        (scheduleTime.isBefore(now) || scheduleTime.equals(now)) && scheduleTime.isAfter(lowerBoundTime)
      }
      .foreach { schedule =>

        val capacity = if (schedule.trainId <= 12) 1800 else 900

        val scheduleTime = LocalTime.parse(schedule.departureTime, DateTimeFormatter.ofPattern("HHmm"))
        val minutes = Duration.between(scheduleTime, now).toMinutes

        metroCars = metroCars.updated(schedule.trainId,
          MetroCar(schedule.trainId, schedule.departureStation, minutes, capacity))
      }
    log.info("Metro Car Locations Updated")
  }

  def updateCapacities(stationsStatus: Map[String, List[Passenger]]): Unit = {
    metroCarsCapacities = metroCarsCapacities.map { case (metroCarId, currentMetroPassengers) =>
      val metroCar = metroCars(metroCarId)
      val stationPassengers = stationsStatus(metroCar.departureStation)

      val passengersTaken = if (metroCar.minutesFromDeparture == 0)
        stationPassengers.take(metroCar.capacity - currentMetroPassengers.size)
      else List.empty

      val previousPassengers = passengersThatBoarded.getOrElse(metroCar.departureStation, 0)
      passengersThatBoarded = passengersThatBoarded
        .updated(metroCar.departureStation, previousPassengers+passengersTaken.size)

      (metroCarId, currentMetroPassengers ::: passengersTaken)
    }

    metroCarsCapacities = metroCarsCapacities.map { case (metroCarId, passengers) =>
      val metroCar = metroCars(metroCarId)
      val currentStation = metroCar.departureStation

      val passengersOut = if (metroCar.minutesFromDeparture == 0)
        passengers.filter(_.destination != currentStation)
      else passengers

      (metroCarId, passengersOut)
    }
    log.info("Metro Car Capacities updated {}", metroCarsCapacities.mapValues(_.size))
    log.info("Passengers that boarded {}", passengersThatBoarded)
  }

  def makeJsonResponse(stationCapacities: Map[String, Int]): JsValue = {
    val metroCarLocations = metroCars.map { case (id, metroCar) =>
      (id.toString, metroCar)
    }

    val metroCarCapacities = metroCarsCapacities.map { case (metroCarId, passengers) =>
      (metroCarId.toString, passengers.size)
    }

    Json.obj(
      "metroCars" -> Json.toJson(metroCarLocations),
      "metroCarsCapacities" -> Json.toJson(metroCarCapacities),
      "stationCapacities" -> Json.toJson(stationCapacities)
    )
  }

  def loadSchedules(): Unit = {
    val file = new File("schedules.csv")
    val now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)

    schedules = for (
      line: String <- scala.io.Source.fromFile(file).getLines().toList.drop(1)
    ) yield scheduleFromLine(line)

    metroCars = Map(schedules
      .filter { schedule =>
        val scheduleTime = LocalTime.parse(schedule.departureTime, DateTimeFormatter.ofPattern("HHmm"))
        val lowerBoundTime = now.minusMinutes(4) //0357

        (scheduleTime.isBefore(now) || scheduleTime.equals(now)) && scheduleTime.isAfter(lowerBoundTime)
      }
      .map { schedule =>
        val capacity = if (schedule.trainId <= 12) 1800 else 900

        val scheduleTime = LocalTime.parse(schedule.departureTime, DateTimeFormatter.ofPattern("HHmm"))
        val minutes = Duration.between(scheduleTime, now).toMinutes

        (schedule.trainId, MetroCar(schedule.trainId, schedule.departureStation, minutes, capacity))
      }: _*)

    metroCarsCapacities = metroCars.mapValues { _ => List.empty[Passenger] }

  }


  def scheduleFromLine(line: String): Schedule = {
    val values = line.split(';')
    Schedule(values(0).toInt, values(1), values(2), values(3))
  }

  implicit val metroCarWriter = new Writes[MetroCar] {
    def writes(metroCar: MetroCar) = Json.obj(
      "id" -> metroCar.id,
      "departureStation" -> metroCar.departureStation,
      "minutesFromDeparture" -> metroCar.minutesFromDeparture,
      "capacity" -> metroCar.capacity
    )
  }

}

object StatusUpdater {
  def props(stationStatus: ActorRef, out: ActorRef) = Props(classOf[StatusUpdater], stationStatus, out)
}
