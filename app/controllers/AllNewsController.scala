package controllers

import KafkaUtils.MyConsumer
import caseClasses.NewsData

import javax.inject._
import play.api.mvc._
import dao.UserDao
import models.User
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AllNewsController @Inject()(cc: ControllerComponents, userDao: UserDao, myConsumer: MyConsumer)(implicit ec: ExecutionContext) extends AbstractController(cc) with I18nSupport {

  import play.api.libs.json._

  def userNews(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("id")
    val userIdIntOption: Option[Int] = userIdOption.flatMap(str => Try(str.toInt).toOption)

    userIdIntOption match {
      case Some(userId) =>
        val topicNamesFuture: Future[Seq[String]] = userDao.getAllUserIds().map { userIds =>
          userIds.map(id => s"${id}_topic")
        }

        val combinedNewsDataFuture: Future[Seq[NewsData]] = topicNamesFuture.flatMap { topicNames =>
          Future.traverse(topicNames) { topicName =>
            val rawData = myConsumer.readFormTopic(topicName)
            val dataList = rawData.map(parseJsonString)
            Future.sequence(dataList.map { case (news_id, title, content, category, publisher_name, publisher_id, date) =>
              Future.successful(NewsData(news_id, title, content, category, publisher_name, publisher_id, date))
            })
          }.map(_.flatten)
        }

        val usersFuture = userDao.getAllUsers()

        // Combine both futures using `Future.zip`
        val combinedFuture: Future[(Seq[NewsData], Seq[User])] = combinedNewsDataFuture.zip(usersFuture)

        // Render the view when all data has been fetched and processed
        combinedFuture.map { case (newsDataList, userList) =>
          Ok(views.html.all_news(newsDataList, userList))
        }.recover {
          case e: Exception =>
            InternalServerError("An error occurred while fetching data.")
        }

      case None =>
        Future.successful(Redirect(routes.AdminController.showLoginPage()))
    }
  }

  private def parseJsonString(jsonString: String): (Long, String, String, String, String, Long, String) = {
    val json = Json.parse(jsonString)
    val news_id = (json \ "news_id").asOpt[Long].getOrElse(0L)
    val publisher_id = (json \ "publisher_id").asOpt[Long].getOrElse(-1L)
    val category = (json \ "category").asOpt[String].getOrElse("")
    val publisher_name = (json \ "publisher_name").asOpt[String].getOrElse("")
    val content = (json \ "content").asOpt[String].getOrElse("")
    val title = (json \ "title").asOpt[String].getOrElse("")
    val date = (json \ "date_time").asOpt[String].getOrElse("")
    (news_id, title, content, category, publisher_name, publisher_id, date)
  }
}

