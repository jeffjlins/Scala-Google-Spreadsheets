//name := "ScalaSpreadSheets"
//
//version := "0.1"
//
//scalaVersion := "2.13.0"
//
//lazy val `renderer` = project.in(file("."))
//  //.enablePlugins(ScalaJSPlugin)
//  .settings(
//    name := "Hello",
//    libraryDependencies ++= Seq(
//      "com.google.api-client" % "google-api-client" % "1.30.10",
//      "com.google.oauth-client" % "google-oauth-client-jetty" % "1.31.0",
//      "com.google.apis" % "google-api-services-sheets" % "v4-rev612-1.25.0",
//      "com.google.code.gson" % "gson" % "2.8.6",
//      "org.typelevel" %% "cats-core" % "2.0.0",
//      "org.scalatest" %% "scalatest" % "3.2.2" % Test,
//    )
//  )

val sharedSettings = Seq(
  name := "money-gs",
  version := "0.1",
  scalaVersion := "2.13.0"
)

lazy val gs = crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Full).in(file(".")) //leave of in part to get a subdir for project
    .settings(sharedSettings)
    .jsSettings(

    )
    .jvmSettings(
      //name := "money-jvm",
      libraryDependencies ++= Seq(
        "com.google.api-client" % "google-api-client" % "1.30.10",
        "com.google.oauth-client" % "google-oauth-client-jetty" % "1.31.0",
        "com.google.apis" % "google-api-services-sheets" % "v4-rev612-1.25.0",
        "com.google.code.gson" % "gson" % "2.8.6",
        "org.typelevel" %% "cats-core" % "2.0.0",
        //"io.chrisdavenport" %% "cats-time" % "4.0.0",
        "com.beachape" %% "enumeratum" % "1.6.1",
        "org.scalatest" %% "scalatest" % "3.2.2" % Test,
      )
    )

// Optional in sbt 1.x (mandatory in sbt 0.13.x)
lazy val gsJS     = gs.js
lazy val gsJVM    = gs.jvm