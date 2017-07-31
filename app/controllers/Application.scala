package controllers

import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import actors.{ConnectionManager, StationStatus, StatusUpdater}
import akka.actor.{ActorSystem, Props}
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.io.Source

class Application @Inject()(system: ActorSystem, ec: ExecutionContext) extends Controller {

  implicit val executionContext = ec
  val stationStatus = system.actorOf(Props[StationStatus], name = "stationStatus")
  val statusUpdater = system.actorOf(StatusUpdater.props(stationStatus), name = "statusUpdater")

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def stream = WebSocket.acceptWithActor[String, JsValue] { request =>
    out =>
      ConnectionManager.props(out, statusUpdater)
  }

  def report(station: String) = Action.async { implicit request =>

    Future {
      blocking {
        val fIn = new File(s"logs/passengers_in/$station.log")
        val incomingPassengersGrouped = groupBy10Minutes(fIn)

        val fOut = new File(s"logs/passengers_out/$station.log")
        val outcomingPassengersGrouped = groupBy10Minutes(fOut)

        val disaggregation = incomingPassengersGrouped
          .map { case (k, in) =>
            (k, in - outcomingPassengersGrouped.getOrElse(k, 0))
          }
        (incomingPassengersGrouped, disaggregation)
      }
    }
      .map { r =>
        Ok(views.html.report(r))
      }
  }

  private def groupBy10Minutes(f: File): Map[LocalTime, Int] = {

    if (!f.exists()) {
      Map.empty
    } else {

      val values = for (l <- Source.fromFile(f).getLines()) yield l.split(';')(0)
      val vList = values.toList

      vList.groupBy { s: String =>
        val time = LocalTime.parse(s, DateTimeFormatter.ofPattern("HHmm"))
        time.plusMinutes((70 - time.getMinute) % 10)
      }.mapValues { timeList =>
        timeList.size
      }
    }
  }

}
