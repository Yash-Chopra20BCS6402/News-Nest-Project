package controllers

import javax.inject.Inject
import play.api.mvc._
import dao.UserDao
import models.User
import play.api.data.Form
import play.api.data.Forms.{ignored, mapping, text}
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}

class AdminController @Inject()(cc: ControllerComponents, userDao: UserDao)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  val registrationForm: Form[User] = Form(
    mapping(
      "id" -> ignored(0L),
      "userName" -> text(),
      "pwd" -> text()
    )(User.apply)(User.unapply)
  )

  def showLoginPage(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.admin_login(registrationForm))
  }

  def login(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    registrationForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.admin_login(formWithErrors)))
      },
      loginData => {
        userDao.loginAdmin(loginData.userName, loginData.pwd).map {
          case Some(user) =>
            Redirect(routes.AllNewsController.userNews())
              .withSession("id" -> user.id.toString)
          case None =>
            val formWithErrors = registrationForm.withGlobalError("Invalid username/email or password")
            BadRequest(views.html.admin_login(formWithErrors))
        }
      }
    )
  }
}
