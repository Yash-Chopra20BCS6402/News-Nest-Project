package controllers

import caseClasses.NewsData

import javax.inject.Inject
import play.api.mvc._
import dao.UserDao
import models.User
import play.api.data.Form
import play.api.data.Forms.{email, ignored, mapping, nonEmptyText, text}
import play.api.http.Writeable.wByteArray
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HomeController @Inject()(cc: ControllerComponents, userDao: UserDao)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  val registrationForm: Form[User] = Form(
    mapping(
      "id" -> ignored(0L),
      "userName" -> text(),
      "pwd" -> text()
    )(User.apply)(User.unapply)
  )

  val newsForm: Form[NewsData] = Form(
    mapping(
      "news_id" -> ignored(0L),
      "title" -> text(),
      "content" -> text(),
      "category" -> text(),
      "publisher_name" -> text(),
      "publisher_id" -> ignored(0L),
      "date" -> ignored("")
    )(NewsData.apply)(NewsData.unapply)
  )

  def showAuthenticationForm(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    val userIdIntOption: Option[Int] = userIdOption.flatMap(str => Try(str.toInt).toOption)
    userIdIntOption match {
      case Some(userId) =>
        Redirect(routes.HomeController.index())
      case None =>
        Ok(views.html.auth(registrationForm))
    }
  }

  def login(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    registrationForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.auth(formWithErrors)))
      },
      loginData => {
        userDao.getUserByCredentials(loginData.userName, loginData.pwd).map {
          case Some(user) =>
            Redirect(routes.HomeController.index())
              .withSession("userId" -> user.id.toString)
          case None =>
            val formWithErrors = registrationForm.withGlobalError("Invalid username/email or password")
            BadRequest(views.html.auth(formWithErrors))
        }
      }
    )
  }

  def register: Action[AnyContent] = Action.async { implicit request =>
    registrationForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.auth(formWithErrors)))
      },
      userData => {
        userDao.addUser(userData).map { _ =>
          Redirect(routes.HomeController.showAuthenticationForm)
            .flashing("success" -> "User registered successfully")
        }
      }
    )
  }


  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    val userIdIntOption: Option[Int] = userIdOption.flatMap(str => Try(str.toInt).toOption)
    userIdIntOption match {
      case Some(userId) =>
        Ok(views.html.add_post(newsForm))
      case None =>
        Ok(views.html.auth(registrationForm))
    }
  }

  def goToAdminLogin(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.AdminController.showLoginPage())
  }
}
