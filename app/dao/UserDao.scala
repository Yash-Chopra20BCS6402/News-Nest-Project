package dao

import models.Admin.admins
import models.Users.users

import javax.inject.Inject
import models.User
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  def addUser(user: User): Future[Int] = {
    db.run(users += user)
  }

  def getUserByCredentials(userName: String, password: String): Future[Option[User]] = {
    val query = users.filter(user =>
      user.userName === userName
    ).filter(_.pwd === password).result.headOption

    db.run(query)
  }

  def getUserById(userId: Long): Future[Option[User]] = {
    db.run(users.filter(_.id === userId).result.headOption)
  }

  def getAllUsers(): Future[Seq[User]] = {
    db.run(users.result)
  }

  def loginAdmin(userName: String, password: String): Future[Option[User]] = {
    val query = admins.filter(user =>
      user.userName === userName
    ).filter(_.pwd === password).result.headOption

    db.run(query)
  }

  def getAllUserIds(): Future[Seq[Long]] = {
    val query = users.map(_.id).result
    db.run(query)
  }
}
