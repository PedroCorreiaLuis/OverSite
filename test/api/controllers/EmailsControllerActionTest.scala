package api.controllers

import database.mappings.ChatMappings._
import database.mappings.DraftMappings.destinationDraftTable
import database.mappings.EmailMappings._
import database.mappings.UserMappings._
import database.mappings._
import database.repository.ChatRepositoryImpl
import definedStrings.AlgorithmStrings.MD5Algorithm
import definedStrings.testStrings.ControllerStrings._
import encryption.EncryptString
import generators.Generator
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.parse
import play.api.test.FakeRequest
import play.api.test.Helpers.{ route, status, _ }
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class EmailsControllerActionTest extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy val injector: Injector = appBuilder.injector()
  lazy implicit val db: Database = injector.instanceOf[Database]
  lazy implicit val rep: ChatRepositoryImpl = new ChatRepositoryImpl()

  private val testGenerator = new Generator()
  private val chatIDExample = testGenerator.ID
  private val emailIDExample = new Generator().ID
  private val emailExample = testGenerator.emailAddress
  private val wrongTokenExample = new Generator().token
  private val passwordExample = testGenerator.password
  private val dateExample = testGenerator.dateOf
  private val headerExample = testGenerator.header
  private val bodyExample = testGenerator.body

  private val toAddressesJsonExample = testGenerator.emailAddresses.mkString("\" , \"")
  private val bccJsonExample = new Generator().emailAddresses.mkString("\" , \"")
  private val ccJsonExample = new Generator().emailAddresses.mkString("\" , \"")

  private val encryptedPasswordExample = new EncryptString(passwordExample, MD5Algorithm).result.toString

  private val tables = Seq(chatTable, userTable, emailTable, destinationEmailTable, destinationDraftTable, loginTable, shareTable)

  override def beforeEach(): Unit = {

    Await.result(db.run(DBIO.seq(tables.map(_.delete): _*)), Duration.Inf)
    //encrypted "12345" password
    Await.result(db.run(userTable += UserRow(emailExample, encryptedPasswordExample)), Duration.Inf)
    Await.result(db.run(loginTable +=
      LoginRow(emailExample, testGenerator.token, System.currentTimeMillis() + 360000, active = true)), Duration.Inf)

  }

  override def beforeAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.create): _*)), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.drop): _*)), Duration.Inf)
  }

  /** POST /email end-point */

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case dateOf" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "NOTdateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case header" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "NOTheader" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case body" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "NOTbody" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case sendNow" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "NOTsendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send an OK if JSON body has an valid format:" + " case missing parameter chatID" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case missing parameter dateOf" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case missing parameter header" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case missing parameter body" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send an OK if JSON body has an valid format:" + " case missing parameter to" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#email" should {
    "send an OK if JSON body has an valid format:" + " case missing parameter BCC" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to": ["$toAddressesJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#email" should {
    "send an OK if JSON body has an valid format:" + " case missing parameter CC" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" :"$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#email" should {
    "send a BadRequest if JSON body has an invalid format:" + " case missing parameter sendNow" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"]
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#email" should {
    "send a Forbidden if JSON header has an invalid token" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }

  "EmailsController" + "#email" should {
    "send a OK if JSON header has a valid token" + " and a valid JSON body" in {
      val fakeRequest = FakeRequest(POST, "/email")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))
      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }
  /** ----------------------------------------------- */

  /** GET /emails/:status end-point */

  "EmailsController" + "#getEmails" should {
    "send a OK if JSON header has a valid token" + " and status: " + "draft" in {
      val fakeRequest = FakeRequest(GET, "/emails/draft")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmails" should {
    "send a OK if JSON header has a valid token" + " and status: " + "received" in {
      val fakeRequest = FakeRequest(GET, "/emails/received")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmails" should {
    "send a OK if JSON header has a valid token" + " and status: " + "sent" in {
      val fakeRequest = FakeRequest(GET, "/emails/sent")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmails" should {
    "Ok with undefined status" in {
      val fakeRequest = FakeRequest(GET, "/emails/:status")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmails" should {
    "send a Forbidden if JSON header has an invalid token" in {
      val fakeRequest = FakeRequest(GET, "/emails/:status")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)

      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }
  /** ----------------------------------------------- */

  /**  GET /emails/:status/:emailID  end-point */

  "EmailsController" + "#getEmail" should {
    "send a OK if JSON header has a valid token" + " and status: " + "draft" in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID?status=draft")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmail" should {
    "send a OK if JSON header has a valid token" + " and status: " + "received" in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID?status=received")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#getEmail" should {
    "send a OK if JSON header has a valid token" + " and status: " + "sent" in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID?status=sent")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }
  // /emails/:emailID
  "EmailsController" + "#getEmail" should {
    "Ok with undefined status" in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID?status=:status")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#getEmail" should {
    "send a Forbidden if JSON header has an invalid token" in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)

      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }

  "EmailsController" + "#getEmail" should {
    "send a OK if JSON header has a valid token" + "case empty optionalStatus = " in {
      val fakeRequest = FakeRequest(GET, "/emails/:emailID?status=")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }
  /** ----------------------------------------------- */

  "EmailsController" + "#toSent" should {
    "send a OK if JSON header has a valid token" + " and status: " + "draft" + " and target email has to address" in {
      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample,
        emailExample, dateExample, headerExample, bodyExample, isTrash = false)), Duration.Inf)

      Await.result(db.run(
        destinationEmailTable += DestinationEmailRow(new Generator().ID, emailExample, Destination.ToAddress, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, "/emails/" + emailIDExample + "/send")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#toSent" should {
    "send a BadRequest if target email has no to address" + " send status" in {

      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample,
        emailExample, dateExample, headerExample, bodyExample, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/send")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#moveInOutTrash" should {
    "send a BadRequest if target email has no to address" + " trash status" in {

      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample,
        emailExample, dateExample, headerExample, bodyExample, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/trash")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#toSent" should {
    "Ok with undefined status" in {

      Await.result(db.run(chatTable += ChatRow(chatIDExample, headerExample)), Duration.Inf)
      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample,
        emailExample, dateExample, headerExample, bodyExample, isTrash = false)), Duration.Inf)
      Await.result(db.run(
        destinationEmailTable += DestinationEmailRow(new Generator().ID, emailExample, Destination.ToAddress, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, "/emails/" + emailIDExample + "/send")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#moveInOutTrash" should {
    "Ok with undefined status" + " trash status" in {

      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample, emailExample, dateExample,
        headerExample, bodyExample, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, s"/emails/$emailIDExample/trash")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }
  "EmailsController" + "#updateDraft" should {
    " update status" in {

      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample, emailExample, dateExample,
        headerExample, bodyExample, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, s"/emails/$emailIDExample/update")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "efe",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : false
          }
        """))

      val result = route(app, fakeRequest)
      status(result.get) mustBe OK
    }
  }

  "EmailsController" + "#updateDraft" should {
    " update status" + " no existing email id" in {

      Await.result(db.run(emailTable += EmailRow(emailIDExample, chatIDExample, emailExample, dateExample,
        headerExample, bodyExample, isTrash = false)), Duration.Inf)

      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/update")
        .withHeaders(HOST -> LocalHost, TokenKey -> testGenerator.token)
        .withJsonBody(parse(
          s"""
          {
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))

      val result = route(app, fakeRequest)
      status(result.get) mustBe BAD_REQUEST
    }
  }

  "EmailsController" + "#toSent" should {
    "send a Forbidden if JSON header has an invalid token" + " send status" in {
      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/send")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)

      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }

  "EmailsController" + "#moveInOutTrash" should {
    "send a Forbidden if JSON header has an invalid token" + " trash status" in {
      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/trash")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)

      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }

  "EmailsController" + "#updateDraft" should {
    "send a Forbidden if JSON header has an invalid token" + " update status" in {
      val fakeRequest = FakeRequest(PATCH, "/emails/:emailID/update")
        .withHeaders(HOST -> LocalHost, TokenKey -> wrongTokenExample)
        .withJsonBody(parse(
          s"""
          {
            "chatID" : "$chatIDExample",
            "dateOf" : "$dateExample",
            "header" : "$headerExample",
            "body" : "$bodyExample",
            "to" : ["$toAddressesJsonExample"],
            "BCC" : ["$bccJsonExample"],
            "CC" : ["$ccJsonExample"],
            "sendNow" : true
          }
        """))

      val result = route(app, fakeRequest)
      status(result.get) mustBe FORBIDDEN
    }
  }
  /** ----------------------------------------------- */

}
