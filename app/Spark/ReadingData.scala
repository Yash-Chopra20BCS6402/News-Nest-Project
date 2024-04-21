package Spark

import KafkaUtils.MyConsumer
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.io.StdIn
import org.json4s._
import org.json4s.jackson.Serialization.write

import java.io.{PrintWriter, StringWriter}

class ReadingData(consumer: MyConsumer)(implicit ec: ExecutionContext) {
  def analyzeDataTopic(topicName: String): Unit = {
    val result = consumer.readFormTopic(topicName)
    if (result.nonEmpty) {
      try {
        val spark = SparkSession.builder()
          .appName("KafkaCode")
          .master("local[*]")
          .getOrCreate()

        import spark.implicits._

        println("Sample Kafka Messages:")
        result.take(5).foreach(println)

        // Creating the  RDD
        val rdd: RDD[String] = spark.sparkContext.parallelize(result)

        // Creating the DataFrame
        val df: DataFrame = spark.read.json(spark.createDataset(result))
        df.printSchema()
        df.show(10,truncate=false)

        // Printing the RDD
        println("RDD:")
        rdd.take(10).foreach(println)

        if (!df.isEmpty) {
          println("The Code is being processed...")
          implicit val formats: Formats = DefaultFormats
          val dataList = write(df.collect().map(row =>
            Map(
              "title" -> row.getAs[String]("title"),
              "content" -> row.getAs[String]("content"),
              "publisher_name" -> row.getAs[String]("publisher_name"),
              "category" -> row.getAs[String]("category"),
              "news_id" -> row.getAs[String]("news_id"),
              "date_time" -> row.getAs[String]("date_time")
            )
          ).toList)

          val pythonScriptPath = "D:\\Users\\DELL\\Documents\\GitHub\\news-nest\\app\\Spark\\testing.py"
          val processBuilder = new ProcessBuilder("python", pythonScriptPath)
          val process = processBuilder.start()
          val outputStream = process.getOutputStream
          val printWriter = new PrintWriter(outputStream)

          printWriter.println(dataList)
          printWriter.flush()
          printWriter.close()

          val exitCode = process.waitFor()

          if (exitCode != 0) {
            val errorStream = process.getErrorStream
            val errorString = new StringWriter()
            val errorPrintWriter = new PrintWriter(errorString)
            errorPrintWriter.println(s"Error: Python script execution failed with exit code $exitCode.")
            scala.io.Source.fromInputStream(errorStream).getLines.foreach(line => errorPrintWriter.println(line))
            errorPrintWriter.flush()
            errorPrintWriter.close()
            println(errorString.toString)
          } else {
            println("Python script executed successfully.")
          }
        } else {
          println("Error: DataFrame is empty.")
        }

        performUserQuery(rdd)

      } catch {
        case ex: Exception =>
          println(s"Error processing Kafka messages: ${ex.getMessage}")
          ex.printStackTrace()
      }
    } else {
      println("No messages fetched from Kafka topic.")
    }
  }

  def analyzeTopicWithUserInput(): Unit = {
    println("Enter the Kafka topic name:")
    val topicName = StdIn.readLine()
    analyzeDataTopic(topicName)
  }

  def performQuery(choice: Int, rdd: RDD[String]): Unit = {
    choice match {
      case 1 =>
        println("Query 1: Filter messages by category")
        val categoryChoice = getCategoryChoice()
        filterByCategory(rdd, categoryChoice)

      case 2 =>
        println("Query 2: Get all titles")
        getAllTitles(rdd)

      case 3 =>
        println("Query 3: Get all Author Names")
        extractAuthorNames(rdd)

      case 4 =>
        println("Query 4: Count of all categories")
        countCategories(rdd)

      case 5 =>
        println("Query 5: Count of all authors")
        countAuthors(rdd)

      case 6 =>
        println("Query 6: Count of all entries")
        countEntries(rdd)

      case _ =>
        println("Invalid query number.")
    }
  }

  def getCategoryChoice(): String = {
    println("Choose a category:")
    println("1. Entertainment")
    println("2. Politics")
    println("3. Technology")
    println("4. Science")
    println("5. Sports")
    println("6. Culture")
    println("7. Art")
    println("8. Health")
    println("9. Food")
    println("10. Fashion")
    println("11. Other Categories")
    StdIn.readLine()
  }

  def filterByCategory(rdd: RDD[String], categoryChoice: String): Unit = {
    categoryChoice match {
      case "1" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Entertainment"))
        filteredRDD.foreach(println)

      case "2" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Politics"))
        filteredRDD.foreach(println)

      case "3" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Technology"))
        filteredRDD.foreach(println)

      case "4" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Science"))
        filteredRDD.foreach(println)

      case "5" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Sports"))
        filteredRDD.foreach(println)

      case "6" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Culture"))
        filteredRDD.foreach(println)

      case "7" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Art"))
        filteredRDD.foreach(println)

      case "8" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Health"))
        filteredRDD.foreach(println)

      case "9" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Food"))
        filteredRDD.foreach(println)

      case "10" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && msg.contains("Fashion"))
        filteredRDD.foreach(println)

      case "11" =>
        val filteredRDD = rdd.filter(msg => msg.contains("category") && !msg.contains("Entertainment") &&
          !msg.contains("Politics") &&
          !msg.contains("Technology") &&
          !msg.contains("Science") &&
          !msg.contains("Sports") &&
          !msg.contains("Culture") &&
          !msg.contains("Art") &&
          !msg.contains("Health") &&
          !msg.contains("Food") &&
          !msg.contains("Fashion"))
        filteredRDD.foreach(println)

      case _ =>
        println("Invalid category choice.")
    }
  }

  def getUserChoice(): Int = {
    println()
    println("Enter 1 for Categories Filtration :")
    println("Enter 2 to show all the titles :")
    println("Enter 3 to show all Author Names :")
    println("Enter 4 to have count of categories :")
    println("Enter 5 to have count of authors :")
    println("Enter 6 to have count of all entries :")
    println("\nEnter your choice :")
    StdIn.readInt()
  }

  def performUserQuery(rdd: RDD[String]): Unit = {
    var continue = true
    while (continue) {
      val choice = getUserChoice()
      if (choice >= 1 && choice <= 6) {
        performQuery(choice, rdd)
      } else if (choice == 0) {
        println("Exiting...")
        continue = false
      } else {
        println("Invalid choice.")
      }
      if (continue) {
        println()
        println("Press Enter or type 'ok' to continue to another choice, or type 0 to quit:")
        val userInput = StdIn.readLine().trim.toLowerCase
        if (userInput.isEmpty || userInput == "ok") {
          continue = true
        } else {
          println("Exiting...")
          continue = false
        }
      }
    }
  }


  def getAllTitles(rdd: RDD[String]): Unit = {
    val titlesRDD = rdd.flatMap { msg =>
      val json = scala.util.parsing.json.JSON.parseFull(msg)
      json.map(_.asInstanceOf[Map[String, Any]]).flatMap(_.get("title").map(_.toString))
    }
    titlesRDD.foreach(println)
  }

  def extractAuthorNames(rdd: RDD[String]): Unit = {
    val authorNamesRDD = rdd.flatMap { msg =>
      val json = scala.util.parsing.json.JSON.parseFull(msg)
      json.map(_.asInstanceOf[Map[String, Any]]).flatMap(_.get("publisher_name").map(_.toString))
    }
    authorNamesRDD.foreach(println)
  }

  def countCategories(rdd: RDD[String]): Unit = {
    val categoriesRDD = rdd.flatMap { msg =>
      val json = scala.util.parsing.json.JSON.parseFull(msg)
      json.map(_.asInstanceOf[Map[String, Any]]).flatMap(_.get("category").map(_.toString))
    }
    val categoryCounts = categoriesRDD.countByValue()
    categoryCounts.foreach { case (category, count) =>
      println(s"$category : $count")
    }
  }

  def countAuthors(rdd: RDD[String]): Unit = {
    val authorNamesRDD = rdd.flatMap { msg =>
      val json = scala.util.parsing.json.JSON.parseFull(msg)
      json.map(_.asInstanceOf[Map[String, Any]]).flatMap(_.get("publisher_name").map(_.toString))
    }
    val authorCounts = authorNamesRDD.countByValue()
    authorCounts.foreach { case (author, count) =>
      println(s"$author : $count")
    }
  }

  def countEntries(rdd: RDD[String]): Unit = {
    val count = rdd.count()
    println(s"Total number of entries: $count")
  }
}

object MyObject extends App {
  val myConsumerObj = new MyConsumer()
  val readingData = new ReadingData(myConsumerObj)
  readingData.analyzeTopicWithUserInput()
}

