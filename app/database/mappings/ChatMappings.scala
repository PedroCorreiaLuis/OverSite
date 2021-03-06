package database.mappings

import definedStrings.DatabaseStrings._
import slick.jdbc.MySQLProfile.api._

/** Case class of Chat Table Row: */
case class ChatRow(
  chatID: String,
  header: String)

/** Case class of Chat User Table Row: */
case class ChatUserRow(
  chatUserID: String,
  chatID: String,
  username: String)

/** Case class of Share Table Row: */
case class ShareRow(
  shareID: String,
  chatID: String,
  fromUser: String,
  toUser: String)

/** Class that defines the chat table, establishing chatID as primary key in the database */
class ChatTable(tag: Tag) extends Table[ChatRow](tag, ChatsTable) {

  def chatID = column[String](ChatIDRow, O.PrimaryKey)
  def header = column[String](HeaderRow)

  def * = (chatID, header) <> (ChatRow.tupled, ChatRow.unapply)
}

/**
 * Class that defines the share table, establishing shareID as primary key in the database
 * and chatID as foreign key
 */
class ShareTable(tag: Tag) extends Table[ShareRow](tag, SharesTable) {

  def shareID = column[String](ShareIDRow, O.PrimaryKey)
  def chatID = column[String](ChatIDRow)
  def fromUser = column[String](FromUserRow)
  def toUser = column[String](ToUserRow)

  def * = (shareID, chatID, fromUser, toUser) <> (ShareRow.tupled, ShareRow.unapply)
}

/** Queries of user table and login table */
object ChatMappings {
  lazy val chatTable = TableQuery[ChatTable]
  lazy val shareTable = TableQuery[ShareTable]
}
