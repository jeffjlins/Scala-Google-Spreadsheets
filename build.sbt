name := "ScalaSpreadSheets"

version := "0.1"

scalaVersion := "2.13.0"

lazy val `renderer` = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "Hello",
    libraryDependencies ++= Seq(
      "com.google.api-client" % "google-api-client" % "1.30.10",
      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.31.0",
      "com.google.apis" % "google-api-services-sheets" % "v4-rev612-1.25.0",
      "com.google.code.gson" % "gson" % "2.8.6",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "org.scalatest" %% "scalatest" % "3.2.2" % Test,
    )
  )

//compile 'com.google.api-client:google-api-client:1.30.4'
//compile 'com.google.oauth-client:google-oauth-client-jetty:1.30.6'
//compile 'com.google.apis:google-api-services-sheets:v4-rev581-1.25.0'