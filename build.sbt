ThisBuild / scalaVersion := "3.8.4"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.13.0",
    ),
    name := "radiantfleet",
  )
