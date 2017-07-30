package controllers

import actors.StatusProvider
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current

class Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def stream = WebSocket.acceptWithActor[String, JsValue]{ request => out =>
    StatusProvider.props(out)
  }

}
