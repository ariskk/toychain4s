ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.ariskk"
ThisBuild / organizationName := "ariskk"

lazy val zioVersion = "1.0.3"
lazy val rocksDBVersion = "6.11.4"
lazy val http4sVersion = "0.21.8"
lazy val zioJsonVersion = "0.0.1"

lazy val deps = Seq(
  "eu.timepit" %% "refined" % "0.9.19",
  "com.chuusai" %% "shapeless" % "2.4.0-M1",
  // ZIO
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-json" % zioJsonVersion,
  // RocksDB
  "org.rocksdb" % "rocksdbjni" % rocksDBVersion,
  // uzhttp
  "org.polynote" %% "uzhttp" % "0.2.6",
  // sttp
  "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.0.0-RC11"
)

lazy val scalatestVersion = "3.2.2"

lazy val testDeps = Seq(
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "org.scalatest" %% "scalatest-funspec" % scalatestVersion % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "toychain4s",
    libraryDependencies ++= deps ++ testDeps,
    testFrameworks in ThisBuild += new TestFramework("zio.test.sbt.ZTestFramework")
  )
