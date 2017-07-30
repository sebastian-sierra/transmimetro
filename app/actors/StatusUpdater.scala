package actors

import java.io.File
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import akka.actor._
import messages._
import models._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration._

class StatusUpdater(stationStatusActor: ActorRef, out: ActorRef) extends Actor with ActorLogging {

  var metroCars = Map.empty[Int, MetroCar]
  var metroCarsCapacities = Map.empty[Int, List[Passenger]]
  var passengersThatBoarded = Map.empty[String, Int]
  var schedules = List.empty[Schedule]


  implicit val executionContext = context.dispatcher
  val tick = context.system.scheduler.schedule(500 millis, 60 seconds, self, Tick)


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

    val time = currentTime()

    schedules
      .filter {
        _.departureTime == time
      }
      .foreach { schedule =>

        val capacity = if (schedule.trainId <= 12) 1800 else 900

        metroCars = metroCars.updated(schedule.trainId,
          MetroCar(schedule.trainId, schedule.departureStation, capacity))
      }
    log.info("Metro Car Locations Updated")
  }

  def updateCapacities(stationsStatus: Map[String, List[Passenger]]): Unit = {
    metroCarsCapacities = metroCarsCapacities.map { case (metroCarId, currentMetroPassengers) =>
      val metroCar = metroCars(metroCarId)
      val stationPassengers = stationsStatus(metroCar.currentStation)
      val passengersTaken = stationPassengers.take(metroCar.capacity - currentMetroPassengers.size)

      passengersThatBoarded = passengersThatBoarded.updated(metroCar.currentStation, passengersTaken.size)

      (metroCarId, currentMetroPassengers ::: passengersTaken)
    }

    metroCarsCapacities = metroCarsCapacities.map { case (metroCarId, passengers) =>
      val metroCar = metroCars(metroCarId)
      val currentStation = metroCar.currentStation
      (metroCarId, passengers.filter(_.destination != currentStation))
    }
    log.info("Metro Car Capacities updated {}", metroCarsCapacities.mapValues(_.size))
    log.info("Passengers that boarded {}", passengersThatBoarded)
  }

  def makeJsonResponse(stationCapacities: Map[String, Int]): JsValue = {
    val metroCarLocations = metroCars.map { case (id, metroCar) =>
      (id.toString, metroCar.currentStation)
    }

    val metroCarCapacities = metroCarsCapacities.map { case(metroCarId, passengers) =>
      (metroCarId.toString, passengers.size)
    }

    Json.obj(
      "metroCarsLocations" -> Json.toJson(metroCarLocations),
      "metroCarsCapacities" -> Json.toJson(metroCarCapacities),
      "stationCapacities" -> Json.toJson(stationCapacities)
    )
  }

  def loadSchedules(): Unit = {
    val file = new File("schedules.csv")
    val time = currentTime()

    schedules = for (
      line: String <- scala.io.Source.fromFile(file).getLines().toList.drop(1)
    ) yield scheduleFromLine(line)

    metroCars = Map(schedules
      .filter { schedule =>
        val now: Date = stringToTime(time)
        val scheduleTime: Date = stringToTime(schedule.departureTime)
        val upperBoundTime = addMinutesToDate(scheduleTime)

        (scheduleTime.after(now) || scheduleTime==now) &&  scheduleTime.before(upperBoundTime)
      }
      .map { schedule =>
        val capacity = if (schedule.trainId <= 12) 1800 else 900
        (schedule.trainId, MetroCar(schedule.trainId, schedule.departureStation, capacity))
      }: _*)

    metroCarsCapacities = metroCars.mapValues{ _ => List.empty[Passenger] }

  }


  def currentTime(): String = {
    val now = Calendar.getInstance.getTime
    val format = new SimpleDateFormat("HHmm")
    format.format(now)
  }

  def stringToTime(time: String): Date = {
    val format = new SimpleDateFormat("HHmm")
    format.parse(time)
  }

  def addMinutesToDate(date: Date): Date = {
    val calendar = Calendar.getInstance
    calendar.setTime(date)
    calendar.add(Calendar.MINUTE, 4)
    calendar.getTime
  }

  def scheduleFromLine(line: String): Schedule = {
    val values = line.split(';')
    Schedule(values(0).toInt, values(1), values(2), values(3))
  }

}

object StatusUpdater {
  def props(stationStatus: ActorRef, out: ActorRef) = Props(classOf[StatusUpdater], stationStatus, out)
}
