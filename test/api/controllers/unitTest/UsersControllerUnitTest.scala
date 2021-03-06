package api.controllers.unitTest

import akka.stream.Materializer
import api.controllers.UsersController
import api.controllers.unitTest.UnitControllerTestsAppBuilder._
import api.validators.TokenValidator
import database.repository._
import database.repository.fake.{ FakeUserRepositoryImpl, FakeUserRepositoryImplWithWrongLoginAndLogout }
import definedStrings.ApiStrings._
import definedStrings.testStrings.ControllerStrings.{ LocalHost, TokenKey }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class UsersControllerUnitTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach with Results {

  implicit private val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy implicit private val mat: Materializer = injectorWithValidToken.instanceOf[Materializer]

  private val usersActions: UserRepository = new FakeUserRepositoryImpl()

  "UsersController #signIn" should {
    "send a Ok if JSON body has a valid format" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.signIn.apply(FakeRequest(POST, "/signin")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> "pluis@growin.pt",
          "password" -> EmptyString))))
      status(result) mustBe CREATED
    }
  }

  "UsersController #signIn" should {
    "send a BadRequest if JSON body has an invalid email address" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.signIn.apply(FakeRequest(POST, "/signin")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> "pluis.growin.pt",
          "password" -> EmptyString))))
      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe InvalidEmailAddressStatus

    }
  }

  "UsersController #signIn" should {
    "send a BadRequest if JSON body has an invalid format: case username" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.signIn.apply(FakeRequest(POST, "/signin")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "NOTusername" -> "pluis@growin.pt",
          "password" -> EmptyString))))
      status(result) mustBe BAD_REQUEST
    }
  }

  "UsersController #signIn" should {
    "send a BadRequest if JSON body has an invalid format: case password" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.signIn.apply(FakeRequest(POST, "/signin")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> "pluis@growin.pt",
          "NOTpassword" -> EmptyString))))
      status(result) mustBe BAD_REQUEST
    }
  }

  "UsersController #logIn" should {
    "send a BadRequest if JSON body has an invalid format: case username" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.logIn.apply(FakeRequest(POST, "/login")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "NOTusername" -> "pluis@growin.pt",
          "password" -> EmptyString))))
      status(result) mustBe BAD_REQUEST
    }
  }

  "UsersController #logIn" should {
    "send a BadRequest if JSON body has an invalid format: case password" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.logIn.apply(FakeRequest(POST, "/login")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> "pluis@growin.pt",
          "NOTpassword" -> EmptyString))))
      status(result) mustBe BAD_REQUEST
    }
  }

  "UsersController #logIn" should {
    "send a Ok if JSON body has a valid format" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.logIn.apply(FakeRequest(POST, "/login")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> "pluis@growin.pt",
          "password" -> EmptyString))))
      status(result) mustBe OK
    }
  }

  "UsersController #logIn" should {
    val usersActionsWithWrongLogin = new FakeUserRepositoryImplWithWrongLoginAndLogout()
    "send a Forbidden if password and username doesn´t match" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActionsWithWrongLogin)
      val result = controller.logIn.apply(FakeRequest(POST, "/login")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString)
        .withBody(Json.toJson(Json.obj(
          fields = "username" -> EmptyString,
          "password" -> EmptyString))))
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe PasswordMissMatchStatus
    }
  }

  "UsersController #logOut" should {
    "send a Ok if the user was logged out" in {
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActions)
      val result = controller.logOut.apply(FakeRequest(PATCH, "/logout")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString))
      status(result) mustBe OK
    }
  }

  "UsersController #logOut" should {
    "send a NotModified if the user wasn´t logged out" in {
      val usersActionsWithWrongLogin = new FakeUserRepositoryImplWithWrongLoginAndLogout()
      val controller = new UsersController(
        injectorWithValidToken.instanceOf[TokenValidator],
        ccWithValidToken,
        actorSystemWithValidToken,
        usersActionsWithWrongLogin)
      val result = controller.logOut.apply(FakeRequest(PATCH, "/logout")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString))
      status(result) mustBe NOT_MODIFIED
    }
  }

  "UsersController #logOut" should {
    "send a Forbidden if JSON header has an invalid token" in {
      val usersActionsWithWrongLogin = new FakeUserRepositoryImplWithWrongLoginAndLogout()
      val controller = new UsersController(
        injectorWithInvalidToken.instanceOf[TokenValidator],
        ccWithInvalidToken,
        actorSystemWithInvalidToken,
        usersActionsWithWrongLogin)
      val result = controller.logOut.apply(FakeRequest(PATCH, "/logout")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> EmptyString))
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe VerifyLoginStatus
    }
  }

}