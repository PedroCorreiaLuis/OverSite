package database.repository.fake

import api.dtos.CreateUserDTO
import database.repository.UserRepository
import definedStrings.testStrings.RepositoryStrings.EmptyString

import scala.concurrent.Future

class FakeUserRepositoryImplWithWrongLoginAndLogout extends UserRepository {

  def insertUser(user: CreateUserDTO): Future[Int] = {
    Future.successful(1)
  }
  def loginUser(user: CreateUserDTO): Future[Seq[CreateUserDTO]] = {
    Future.successful(Seq())
  }
  def insertLogin(user: CreateUserDTO): Future[String] = {
    Future.successful(EmptyString)
  }
  def insertLogout(token: String): Future[Int] = {
    Future.successful(0)
  }

}