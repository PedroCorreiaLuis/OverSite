package api.controllers.functionalTest

import database.mappings.ChatMappings.{ chatTable, shareTable }
import database.mappings.DraftMappings._
import database.mappings.EmailMappings._
import database.mappings.UserMappings._
import database.properties.TestDBProperties
import definedStrings.testStrings.ControllerStrings._
import generators.Generator
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class GeneralControllerFunctionalTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  lazy implicit val db: Database = TestDBProperties.db

  /* Email 1*/
  private val testGenerator1 = new Generator()
  private val chatIDExample1 = testGenerator1.ID
  private val emailExample1 = testGenerator1.emailAddress
  private val passwordExample1 = testGenerator1.password

  /* Email 2*/
  private val testGenerator2 = new Generator()
  private val chatIDExample2 = testGenerator2.ID
  private val emailExample2 = testGenerator2.emailAddress
  //private val invalidEmailExample2 = testGenerator2.words.head
  private val passwordExample2 = testGenerator2.password
  //private val encryptedPasswordExample2 = new EncryptString(passwordExample2, MD5Algorithm).result.toString
  //private val wrongPasswordExample2 = new Generator().password

  private val tables = Seq(chatTable, draftTable, userTable, emailTable, destinationEmailTable, destinationDraftTable, loginTable, shareTable)

  override def beforeAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.create): _*)), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.drop): _*)), Duration.Inf)
  }

  override def beforeEach(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.delete): _*)), Duration.Inf)
  }

  /** POST /sign end-point */
  "Controller #DraftFunctionaltest" should {
    "create and manage a draft between two users" in {
      /** SignIn of User 1*/
      val fakeRequestSignInUser1 = FakeRequest(POST, "/signin")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample1,
          "password" -> passwordExample1)))
      status(route(app, fakeRequestSignInUser1).get) mustBe CREATED

      /** SignIn of User 2*/
      val fakeRequestSignInUser2 = FakeRequest(POST, "/signin")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample2,
          "password" -> passwordExample2)))
      status(route(app, fakeRequestSignInUser2).get) mustBe CREATED

      /** LogIn of User 1*/
      val fakeRequestLogInUser1 = FakeRequest(POST, "/login")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample1,
          "password" -> passwordExample1)))
      status(route(app, fakeRequestLogInUser1).get) mustBe OK
      val tokenUser1 = contentAsJson(route(app, fakeRequestLogInUser1).get).\("Token:").as[String]

      /** LogIn of User 2*/
      val fakeRequestLogInUser2 = FakeRequest(POST, "/login")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample2,
          "password" -> passwordExample2)))
      status(route(app, fakeRequestLogInUser2).get) mustBe OK
      val tokenUser2 = contentAsJson(route(app, fakeRequestLogInUser2).get).\("Token:").as[String]

      /** Draft is saved by user1*/
      val fakeRequestInsertDraftUser1 = FakeRequest(POST, "/draft")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
        .withBody(Json.toJson(Json.obj(
          "chatID" -> testGenerator1.ID,
          "dateOf" -> testGenerator1.dateOf,
          "header" -> testGenerator1.header,
          "body" -> testGenerator1.body,
          "to" -> (emailExample2 +: testGenerator1.emailAddresses),
          "BCC" -> testGenerator1.emailAddresses,
          "CC" -> testGenerator1.emailAddresses)))
      status(route(app, fakeRequestInsertDraftUser1).get) mustBe OK

      /** Gets draft by user 1 to reach the draftID*/
      val fakeRequestGetDraftsUser1 = FakeRequest(GET, "/drafts?isTrash=false")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
      status(route(app, fakeRequestGetDraftsUser1).get) mustBe OK
      val draftIDUser1 = contentAsJson(route(app, fakeRequestGetDraftsUser1).get).head.\("Id").as[String]

      /** Turns the draft to trash */
      val fakeRequestMoveTrashUser1 = FakeRequest(PATCH, "/draft/" + draftIDUser1)
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
        .withBody(Json.toJson(Json.obj(
          "status" -> "trash")))
      status(route(app, fakeRequestMoveTrashUser1).get) mustBe OK

      /** Gets draft by user 1 to reach the draftID*/
      val fakeRequestGetDraftsTrashedUser1 = FakeRequest(GET, "/drafts?isTrash=true")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
      status(route(app, fakeRequestGetDraftsTrashedUser1).get) mustBe OK
      val draftIDTrashUser1 = contentAsJson(route(app, fakeRequestGetDraftsTrashedUser1).get).head.\("Id").as[String]
      draftIDUser1 mustEqual draftIDTrashUser1
      /*
      /** Gets draft by user 1 to reach the draftID*/
      //PATCH  /draft/:draftID                     api.controllers.DraftsController.toSentOrDraft(draftID: String)
      val fakeRequestGetDraftsToSentUser1 = FakeRequest(PATCH, "/draft/" + draftIDUser1)
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
        .withBody(Json.toJson(Json.obj("status" -> "send")))
      status(route(app, fakeRequestGetDraftsToSentUser1).get) mustBe OK

      //val draftIDTrashUser1 = contentAsJson(route(app, fakeRequestGetDraftsTrashedUser1).get).head.\("Id").as[String]
      //draftIDUser1 mustEqual draftIDTrashUser1
      Await.result(db.run(emailTable.result), Duration.Inf).foreach(println(_))
*/
    }
  }

  /** POST /sign end-point */
  "Controller #EmailFunctionaltest" should {
    "sent and manage an email between two users" in {
      /** SignIn of User 1*/
      val fakeRequestSignInUser1 = FakeRequest(POST, "/signin")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample1,
          "password" -> passwordExample1)))
      status(route(app, fakeRequestSignInUser1).get) mustBe CREATED

      /** SignIn of User 2*/
      val fakeRequestSignInUser2 = FakeRequest(POST, "/signin")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample2,
          "password" -> passwordExample2)))
      status(route(app, fakeRequestSignInUser2).get) mustBe CREATED

      /** LogIn of User 1*/
      val fakeRequestLogInUser1 = FakeRequest(POST, "/login")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample1,
          "password" -> passwordExample1)))
      status(route(app, fakeRequestLogInUser1).get) mustBe OK
      val tokenUser1 = contentAsJson(route(app, fakeRequestLogInUser1).get).\("Token:").as[String]

      /** LogIn of User 2*/
      val fakeRequestLogInUser2 = FakeRequest(POST, "/login")
        .withHeaders(HOST -> LocalHost)
        .withBody(Json.toJson(Json.obj(
          "username" -> emailExample2,
          "password" -> passwordExample2)))
      status(route(app, fakeRequestLogInUser2).get) mustBe OK
      val tokenUser2 = contentAsJson(route(app, fakeRequestLogInUser2).get).\("Token:").as[String]

      /** Email is sent by user1 to user 2*/
      val fakeRequestInsertEmailUser1 = FakeRequest(POST, "/email")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
        .withBody(Json.toJson(Json.obj(
          "chatID" -> testGenerator1.ID,
          "dateOf" -> testGenerator1.dateOf,
          "header" -> testGenerator1.header,
          "body" -> testGenerator1.body,
          "to" -> (emailExample2 +: testGenerator1.emailAddresses),
          "BCC" -> testGenerator1.emailAddresses,
          "CC" -> testGenerator1.emailAddresses)))
      status(route(app, fakeRequestInsertEmailUser1).get) mustBe OK

      /** Gets emails by user 1 to reach the email*/
      val fakeRequestGetEmailsUser1 = FakeRequest(GET, "/emails?=sent")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
      status(route(app, fakeRequestGetEmailsUser1).get) mustBe OK
      val emailIDUser1 = contentAsJson(route(app, fakeRequestGetEmailsUser1).get).head.\("Id").as[String]

      /** Gets emails by user 2 to reach the email*/
      val fakeRequestGetEmailsUser2 = FakeRequest(GET, "/emails?=received")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
      status(route(app, fakeRequestGetEmailsUser2).get) mustBe OK
      val emailIDUser2 = contentAsJson(route(app, fakeRequestGetEmailsUser2).get).head.\("Id").as[String]
      /** Verify if the emailIDs are the same*/
      emailIDUser1 mustEqual emailIDUser2

      /** Send email to trash by user 1*/
      val fakeRequestMoveInOutTrash2 = FakeRequest(PATCH, "/emails/" + emailIDUser1)
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
        .withBody(Json.toJson(Json.obj(
          "toTrash" -> true)))
      status(route(app, fakeRequestMoveInOutTrash2).get) mustBe OK

      /** Gets emails by user 1 to reach the email*/
      val fakeRequestGetEmailsSentUser1 = FakeRequest(GET, "/emails?=sent")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser1)
      status(route(app, fakeRequestGetEmailsSentUser1).get) mustBe OK
      val emailIDUser1Aux = contentAsJson(route(app, fakeRequestGetEmailsSentUser1).get).as[Set[String]]
      /** Since the mail was moved to trash, the GET /emails should return an empty set*/
      emailIDUser1Aux.isEmpty mustBe true

      /** Get email by user 2 to reach the specified email*/
      val fakeRequestGetEmailUser2 = FakeRequest(GET, "/emails/" + emailIDUser2 + "?=received")
        .withHeaders(CONTENT_TYPE -> JSON, HOST -> LocalHost, TokenKey -> tokenUser2)
      status(route(app, fakeRequestGetEmailUser2).get) mustBe OK
      val emailIDUser2Aux = contentAsJson(route(app, fakeRequestGetEmailUser2).get).head.\("emailID").as[String]
      /** BVerify if the User 2 still have access to the email*/
      emailIDUser2Aux mustEqual emailIDUser2
    }
  }

}
