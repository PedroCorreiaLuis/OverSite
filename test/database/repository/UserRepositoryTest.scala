package database.repository

import api.dtos.CreateUserDTO
import database.mappings.ChatMappings.{ chatTable, shareTable }
import database.mappings.DraftMappings.destinationDraftTable
import database.mappings.EmailMappings._
import database.mappings.UserMappings.{ loginTable, userTable }
import database.properties.TestDBProperties
import definedStrings.AlgorithmStrings.MD5Algorithm
import definedStrings.testStrings.RepositoryStrings._
import encryption.EncryptString
import generators._
import org.scalatest.{ Matchers, _ }
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class UserRepositoryTest extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {

  implicit private val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  lazy private val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().in(Mode.Test)
  lazy private val injector: Injector = appBuilder.injector()
  lazy implicit private val db: Database = TestDBProperties.db

  private val userActions = injector.instanceOf[UserRepository]
  private val userGenerator = new Generator()
  private val userCreation = CreateUserDTO(userGenerator.username, userGenerator.password)
  private val userCreationWrongPassword = new CreateUserDTO(userCreation.username, new Generator().password)
  private val userCreationWrongUser = new CreateUserDTO(new Generator().username, userCreation.password)

  private val tables = Seq(chatTable, userTable, emailTable, destinationEmailTable, destinationDraftTable, loginTable, shareTable)

  override def beforeAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.create): _*)), Duration.Inf)
  }

  override def afterAll(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.schema.drop): _*)), Duration.Inf)
  }

  override def beforeEach(): Unit = {
    Await.result(db.run(DBIO.seq(tables.map(_.delete): _*)), Duration.Inf)
  }

  /** Verify if an user has signed in into database */
  UserRepository + LoginTableFunction should {
    "check if the correct user is inserted in login table in database" in {
      userActions.insertUser(userCreation)
      val encrypt = new EncryptString(userCreation.password, MD5Algorithm)
      val resultUserTable = db.run(userTable.result)

      resultUserTable.map { seqUserRow =>
        seqUserRow.forall(_.username === userCreation.username) shouldBe true
        seqUserRow.forall(_.password === encrypt.result.toString) shouldBe true
      }
    }
  }

  /** Test the insertion of an user into login database */
  UserRepository + InsertUserFunction should {
    "insert a correct user in database" in {
      userActions.insertUser(userCreation)
      val encrypt = new EncryptString(userCreation.password, MD5Algorithm)
      val resultLoginUser = userActions.loginUser(userCreation)
      /** Verify if user is inserted in login table correctly */
      resultLoginUser.map { seqUserDTO =>
        seqUserDTO.forall(_.username === userCreation.username) shouldBe true
        seqUserDTO.forall(_.password === encrypt.result.toString) shouldBe true
      }
    }
  }

  /** Test the login of a available user */
  UserRepository + LoginUserFunction should {
    "login with a available user in database" in {

      val result = for {
        _ <- userActions.insertUser(userCreation)
        _ <- userActions.insertLogin(userCreation)
        resultLoginTable <- db.run(loginTable.result)
      } yield resultLoginTable

      /** Verify if user is inserted in login table correctly */
      result.map(seqLoginRow =>
        seqLoginRow.forall(_.username === userCreation.username) shouldBe true)
    }
  }

  /** Test the login of an user with a wrong username*/
  UserRepository + LoginUserFunction should {
    "login with an unavailable username in database" in {

      val result = for {
        _ <- userActions.insertUser(userCreation)
        _ <- userActions.insertLogin(userCreation)
        resultLoginUser <- userActions.loginUser(userCreationWrongUser)
      } yield resultLoginUser

      /** Verify if user is inserted in login table correctly */
      result.map(_.isEmpty shouldBe true)
    }
  }

  /** Test the login of an user with a wrong password */
  UserRepository + LoginUserFunction should {
    "login with an unavailable password in database" in {

      val result = for {
        _ <- userActions.insertUser(userCreation)
        _ <- userActions.insertLogin(userCreation)
        resultLoginUser <- userActions.loginUser(userCreationWrongPassword)
      } yield resultLoginUser

      /** Verify if user is inserted in login table correctly */
      result.map(_.isEmpty shouldBe true)
    }
  }

  /** Test the logout of an user into database */
  UserRepository + LogoutUserFunction should {
    "logout with an available user in database" in {

      val result = for {
        _ <- userActions.insertUser(userCreation)
        token <- userActions.insertLogin(userCreation)
        _ <- userActions.insertLogout(token)
        resultLoginTable <- db.run(loginTable.result)
      } yield resultLoginTable

      /** Verify if the logout is processed correctly*/
      result.map(seqLoginRow =>
        seqLoginRow.forall(_.active === false) shouldEqual true)
    }
  }

  /** Test the logout of an user into database with a wrong token*/
  UserRepository + LogoutUserFunction should {
    "logout with an available user in database with wrong token" in {

      val result = for {
        _ <- userActions.insertUser(userCreation)
        _ <- userActions.insertLogin(userCreation)
        _ <- userActions.insertLogout(new Generator().token)
        resultLoginTable <- db.run(loginTable.result)
      } yield resultLoginTable

      /** Verify if the logout is processed correctly*/
      result.map(seqLoginRow =>
        seqLoginRow.forall(_.active === true) shouldEqual true)

    }

  }

}