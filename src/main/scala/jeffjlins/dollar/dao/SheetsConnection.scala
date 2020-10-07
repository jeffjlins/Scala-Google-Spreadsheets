package jeffjlins.dollar.dao

import java.io.{File, InputStreamReader}

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.{BatchUpdateSpreadsheetRequest, GridData, Request, Response, Sheet, Spreadsheet}
import com.google.gson.GsonBuilder
import jeffjlins.dollar.App

import scala.jdk.CollectionConverters._
import cats.syntax.all._

case class SheetsConnection(credsPath: String, tokensDir: String, scopes: List[String]) {
  lazy val sheetsApi: Sheets = {
    val jacksonFactory = JacksonFactory.getDefaultInstance
    val credsFile = App.getClass.getResourceAsStream(credsPath)
    val clientSecrets = GoogleClientSecrets.load(jacksonFactory, new InputStreamReader(credsFile))
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jacksonFactory, clientSecrets, scopes.asJava)
      .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDir))).setAccessType("offline").build
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    val creds = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    new Sheets.Builder(httpTransport, jacksonFactory, creds).setApplicationName("Dollar").build()
  }

}