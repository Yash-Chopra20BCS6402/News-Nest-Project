package controllers

import javax.inject.Inject
import play.api.mvc._
import KafkaUtils.MyConsumer
import caseClasses.NewsData
import play.api.i18n.I18nSupport
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

class ProfileController @Inject()(
                                   cc: ControllerComponents,
                                   myConsumer: MyConsumer
                                 )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport {

  def showProfilePage(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val userIdOption: Option[String] = request.session.get("userId")
    val userIdInt: Int = userIdOption.map(_.toInt).getOrElse(-2)

    val topicName = s"${userIdInt}_topic"
    val rawData = myConsumer.readFormTopic(topicName)
    val dataList = rawData.map(parseJsonString)

    // Convert the raw data into a sequence of News objects
    val newsList: Seq[NewsData] = dataList.map { case (news_id, title, content, category, publisher_name, publisher_id, date) =>
      NewsData(news_id, title, content, category, publisher_name, publisher_id, date)
    }

    Future.successful(Ok(views.html.profile(newsList)))
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
