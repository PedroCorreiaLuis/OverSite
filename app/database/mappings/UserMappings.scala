package database.mappings

import definedStrings.DatabaseStrings._
import slick.jdbc.MySQLProfile.api._

/**  Case class of Login Table Row */
case class LoginRow(
  username: String,
  token: String,
  validDate: Long,
  active: Boolean)

/** Case class of User Table Row */
case class UserRow(
  username: String,
  password: String)

/**
 * Class that defines the login table, making username a foreign key in the database
 * @param tag slick tag
 */
class LoginTable(tag: Tag) extends Table[LoginRow](tag, LoginsTable) {
  def username = column[String](UsernameRow)
  def token = column[String](TokenRow)
  def validDate = column[Long](ValidDateRow)
  def active = column[Boolean](ActiveRow)

  def * = (username, token, validDate, active) <> (LoginRow.tupled, LoginRow.unapply)
}

/**  Class that defines the user table, making username a primary key in the database */
class UserTable(tag: Tag) extends Table[UserRow](tag, UsersTable) {
  def username = column[String](UsernameRow, O.PrimaryKey)
  def password = column[String](PasswordRow)

  def * = (username, password) <> (UserRow.tupled, UserRow.unapply)
}

/** Queries of user table and login table */
object UserMappings {
  lazy val userTable = TableQuery[UserTable]
  lazy val loginTable = TableQuery[LoginTable]
}
