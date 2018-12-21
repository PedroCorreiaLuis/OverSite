package api.controllers

import akka.actor.ActorSystem
import api.dtos.CreateUserDTO
import api.validators.{ EmailAddressValidator, TokenValidator }
import database.repository.UserRepository
import javax.inject._
import play.api.libs.json.{ JsError, JsValue, Json }
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import slick.jdbc.MySQLProfile.api._
import regex.RegexPatterns.emailAddressPattern
import api.validators.EmailAddressValidator._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

/**
 * Class that is injected with end-points
 */

class UsersController @Inject() (
  cc: ControllerComponents,
  actorSystem: ActorSystem,
  tokenValidator: TokenValidator,
  implicit val db: Database,
  userActions: UserRepository)(implicit exec: ExecutionContext)

  extends AbstractController(cc) {

  /**
   * Sign in action
   *
   * @return When a valid user is inserted, it is added in the database, otherwise an error message is sent
   */
  def signIn: Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val userResult = request.body.validate[CreateUserDTO]

    userResult.fold(
      errors => {
        Future {
          BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))
        }
      },
      user => {
        if (validateEmailAddress(emailAddressPattern, Left(user.username))) {
          userActions.insertUser(user)
          Future {
            Created
          }
        } else Future { BadRequest("Please insert a valid e-mail address") }
      })
  }

  /**
   * Login action
   *
   * @return When a valid login is inserted, it is added in the database
   *         and the generated token is sent to user, otherwise an error message is sent
   */
  def login: Action[JsValue] = Action(parse.json).async { request: Request[JsValue] =>
    val emailResult = request.body.validate[CreateUserDTO]
    // Getting the token from the request API call

    emailResult.fold(
      errors => {
        Future {
          BadRequest(Json.obj("status" -> "Error:", "message" -> JsError.toJson(errors)))
        }
      },
      user => {
        val loggedUser = userActions.loginUser(user)
        loggedUser.map(_.length).map {
          case 1 => Ok("Your token is: " + userActions.insertLogin(user) + "\n The token is valid for 1 hour")
          case x => Forbidden("Username and password doesn´t match" + x)
        }
      })
  }

  /**
   *
   * @return
   */
  def logout: Action[AnyContent] = tokenValidator.async { request =>
    val authToken = request.headers.get("Token").getOrElse("")
    userActions.insertLogout(authToken).map {
      case 1 => Ok
      case _ => NotModified
    }
  }
}
