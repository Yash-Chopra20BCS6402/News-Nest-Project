package controllers

import KafkaUtils.MyProducer
import caseClasses.NewsData

import javax.inject.Inject
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms.{ignored, mapping, text}
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc.MultipartFormData.FilePart

import java.io.InputStream
import scala.io.Source
import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NewsController @Inject()(cc: ControllerComponents, myProducer: MyProducer)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

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

  private val currentDate = LocalDate.now().toString

  def showUploadNews(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.add_post(newsForm))
  }

  def uploadNews: Action[AnyContent] = Action.async { implicit request =>
    val userIdOption: Option[String] = request.session.get("userId")
    val userIdInt: Int = userIdOption.map(_.toInt).getOrElse(-1)

    newsForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.add_post(formWithErrors)))
      },
      newsData => {
        val topicName = s"${userIdInt}_topic"
        val jsonData = s"""{"title": "${newsData.title}", "content": "${newsData.content}", "publisher_name": "${newsData.publisher_name}", "category": "${newsData.category}"}"""
        val modifiedContent = s"[$jsonData]"
        val topicCreationAndInsertResult = myProducer.createTopicAndInsertData(topicName, modifiedContent, userIdInt)
        topicCreationAndInsertResult.flatMap { topicCreationAndInsert =>
          if (topicCreationAndInsert) {
            Future.successful(Redirect(routes.NewsController.showUploadNews)
              .flashing("success" -> "News uploaded successfully"))
          } else {
            Future.successful(Redirect(routes.NewsController.showUploadNews)
              .flashing("error" -> "Failed to create topic"))
          }
        }
      }
    )
  }


  def logout: Action[AnyContent] = Action {
    Redirect(routes.HomeController.showAuthenticationForm)
      .withNewSession
  }

  def goToProfilePage(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.ProfileController.showProfilePage)
  }

  def uploadNewsFromFile: Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { request =>
    val userId = request.session.get("userId").map(_.toInt).getOrElse(0)
    request.body.file("newsFile").map { filePart =>

      val modifiedContent = modifyJsonFile(filePart)
      println(modifiedContent)

      val topicName = s"${userId}_topic"

      val topicCreationAndInsertResult = myProducer.createTopicAndInsertData(topicName.toLowerCase(), modifiedContent, userId)
      topicCreationAndInsertResult.flatMap { topicCreationAndInsert =>
        if (topicCreationAndInsert) {
          Future.successful(Redirect(routes.NewsController.showUploadNews))
        } else {
          Future.successful(Redirect(routes.NewsController.showUploadNews).flashing("error" -> "Failed to create topic"))
        }
      }
    }.getOrElse {
      Future.successful(Redirect(routes.NewsController.showUploadNews).flashing("error" -> "File upload failed!"))
    }
  }

  private def modifyJsonFile(filePart: FilePart[Files.TemporaryFile]): String = {
    Try {
      val inputStream: InputStream = filePart.ref.path.toFile.toURI.toURL.openStream()
      val source = Source.fromInputStream(inputStream)
      val fileContent = try source.getLines().mkString("\n") finally source.close()
      val modifiedContent = if (fileContent.trim().endsWith(",")) {
        fileContent.dropRight(1)
      } else {
        fileContent
      }
      val wrappedContent = s"[$modifiedContent]"
      wrappedContent
    }.getOrElse {
      Console.println(s"Error reading file: ${filePart.filename}")
      ""
    }
  }
}