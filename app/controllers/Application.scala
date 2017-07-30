package controllers

import actors.StatusProvider
import play.api._
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.Play.current

class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def stream = WebSocket.acceptWithActor[String, JsValue]{ request => out =>
    StatusProvider.props(out)
  }

}
