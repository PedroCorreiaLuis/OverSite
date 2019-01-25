package api.controllers
import api.dtos.{ CreateEmailDTO, CreateShareDTO, CreateUserDTO }
import database.mappings.ChatMappings._
import database.mappings.DraftMappings.destinationDraftTable
import database.mappings.EmailMappings._
import database.mappings.UserMappings._
import database.repository.{ FakeChatRepositoryImpl, FakeEmailRepositoryImpl, FakeUserRepositoryImpl }
import definedStrings.testStrings.RepositoryStrings._
import generators.Generator
import org.scalatest.{ Matchers, _ }
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

import akka.actor.ActorSystem
import akka.stream.Materializer
import api.dtos.{ CreateEmailDTO, CreateShareDTO, CreateUserDTO }
import api.validators.TokenValidator
import database.mappings.ChatMappings._
import database.mappings.DraftMappings.destinationDraftTable
import database.mappings.EmailMappings._
import database.mappings.UserMappings._
import database.repository._
import definedStrings.testStrings.RepositoryStrings._
import generators.Generator
import javax.inject.Inject
import org.scalatest.{ Matchers, _ }
import play.api.Mode
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import slick.jdbc.H2Profile.api._
import ActorSystem._
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import play.api.inject.bind

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class DraftsControllerActionTest extends AsyncWordSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers {

  val fakeChatActions = new FakeChatRepositoryImpl
  val fakeEmailActions = new FakeEmailRepositoryImpl
  val fakeUserActions = new FakeUserRepositoryImpl

}
