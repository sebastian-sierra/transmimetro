package controllers

import actors.{StationStatus, ConnectionManager, StatusUpdater}
import akka.actor.{ActorSystem, Props}
import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.ExecutionContext

class Application @Inject()(system: ActorSystem, ec: ExecutionContext) extends Controller {

  implicit val executionContext = ec
  val stationStatus = system.actorOf(Props[StationStatus], name = "stationStatus")
  val statusUpdater = system.actorOf(StatusUpdater.props(stationStatus), name = "statusUpdater")

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def stream = WebSocket.acceptWithActor[String, JsValue]{ request => out =>
    ConnectionManager.props(out, statusUpdater)
  }

}
