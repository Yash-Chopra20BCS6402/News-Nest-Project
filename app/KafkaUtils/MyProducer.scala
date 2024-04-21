package KafkaUtils

import java.util.Properties
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.convert.ImplicitConversions.`collection asJava`

class MyProducer {

  def createTopicAndInsertData(topicName: String, jsonData: String, userId: Int)(implicit ec: ExecutionContext): Future[Boolean] = Future {
    var adminClient: AdminClient = null
    var producer: KafkaProducer[String, String] = null

    try {
      val props = new Properties()
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      adminClient = AdminClient.create(props)

      val existingTopics = adminClient.listTopics().names().get().asScala
      if (!existingTopics.contains(topicName)) {
        val newTopic = new NewTopic(topicName, 200, 1.toShort)
        adminClient.createTopics(List(newTopic).asJavaCollection).all().get()
      }

      val producerProps = new Properties()
      producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
      producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
      producer = new KafkaProducer[String, String](producerProps)

      val currentDateTime = LocalDateTime.now()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")
      val formattedDateTime = currentDateTime.format(formatter)

      // Define additional data
      val additionalData = Map(
        "news_id" -> s"${topicName}_$formattedDateTime",
        "date_time" -> formattedDateTime,
        "publisher_id" -> userId
      )

      // Convert additional data to JSON object
      implicit val formats: DefaultFormats.type = DefaultFormats
      val additionalJson = JObject(additionalData.map { case (key, value) => key -> Extraction.decompose(value) }.toList)

      // Parse JSON data
      val jsonList = parse(jsonData).extract[List[JObject]]

      // Merge additional data with JSON objects and send to Kafka
      jsonList.foreach { json =>
        val mergedJson = json merge additionalJson
        val value = compact(render(mergedJson))
        val record = new ProducerRecord[String, String](topicName, value)
        producer.send(record)
      }

      true

    } catch {
      case ex: Exception =>
        Console.println(s"Error: ${ex.getMessage}")
        false
    } finally {
      if (adminClient != null) adminClient.close()
      if (producer != null) producer.close()
    }
  }
}