name := """News Nest"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


scalaVersion := "2.13.13"

javaOptions ++= Seq(
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--illegal-access=permit",
)

// Define library dependencies
libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test,
  "org.apache.spark" %% "spark-core" % "3.3.0" exclude("org.scala-lang.modules", "scala-xml_2.13"),
  "org.apache.spark" %% "spark-sql" % "3.3.0",

  // Play Framework
  "com.typesafe.play" %% "play-slick" % "5.2.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.2.0",
  "com.typesafe.play" %% "play-json" % "2.9.2",

  // Kafka
  "org.apache.kafka" %% "kafka" % "3.0.0",
  "org.apache.kafka" % "kafka-clients" % "3.0.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.1",

  // Akka
  "com.typesafe.akka" %% "akka-http" % "10.2.6",
  "com.typesafe.akka" %% "akka-stream" % "2.6.17",
  "com.typesafe.akka" %% "akka-actor" % "2.6.17",

  // Database
  "mysql" % "mysql-connector-java" % "8.0.26",
  "com.datastax.oss" % "java-driver-core" % "4.14.1",
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"

// Override Scala XML dependency to avoid conflicts
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml"%"1.2.0"