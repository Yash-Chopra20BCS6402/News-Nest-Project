package models

import slick.jdbc.PostgresProfile.api._

class Admin(tag: Tag) extends Table[User](tag, "admin_info") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

  def userName = column[String]("username")

  def pwd = column[String]("password")

  def * = (id, userName, pwd) <> (User.tupled, User.unapply)
}

object Admin{
  val admins = TableQuery[Admin]
}