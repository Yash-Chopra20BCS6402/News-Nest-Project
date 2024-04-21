package models

import slick.jdbc.PostgresProfile.api._

case class User(id: Long = 0, userName: String, pwd: String)

class Users(tag: Tag) extends Table[User](tag, "users_info") {
  def id = column[Long]("user_id", O.PrimaryKey, O.AutoInc)

  def userName = column[String]("username")

  def pwd = column[String]("password")

  def * = (id, userName, pwd) <> (User.tupled, User.unapply)
}

object Users {
  val users = TableQuery[Users]
}