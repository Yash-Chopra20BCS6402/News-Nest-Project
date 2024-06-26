package KafkaUtils

import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import scala.collection.JavaConverters._
import java.time.Duration

class MyConsumer {
  def readFormTopic(topicName: String): List[String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "my_grou22p")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    var consumer: KafkaConsumer[String, String] = null
    try {
      consumer = new KafkaConsumer[String, String](props)
      consumer.subscribe(List(topicName).asJava)

      val records = consumer.poll(Duration.ofMillis(1000))
      val result = records.iterator().asScala.map(_.value()).toList

      result

    } catch {
      case ex: Exception =>
        println(s"Error fetching data from Kafka topic: ${ex.getMessage}")
        ex.printStackTrace()
        List.empty[String]
    } finally {
      if (consumer != null) consumer.close()
    }
  }
}
