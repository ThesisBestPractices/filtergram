package info.chinnews.instagram

import java.lang.ProcessBuilder.Redirect
import java.net.URI
import java.nio.file.{Path, _}
import java.nio.file.attribute.BasicFileAttributes
import java.util.Collections
import java.util.concurrent.locks.{ReentrantLock, Lock}

import argonaut.Parse
import com.google.inject.{Injector, Inject}
import com.typesafe.config.Config
import org.http4s.Method
import org.http4s.dsl._
import org.http4s.server.jetty.JettyBuilder
import org.http4s.server.{HttpService, Server}
import org.slf4j.{LoggerFactory, Logger}

import scalaj.http.Http

/**
  * Created by tsarevskiy on 12/11/15.
  */
class InstagramAuth @Inject()(config: Config, injector: Injector) {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val lock: Lock = new ReentrantLock()

  private val client_id = config.getString("chin_news.instagram.client_id")

  private val client_secret = config.getString("chin_news.instagram.client_secret")

  private
  @volatile var currentToken: String = ""

  case class CopyDirVisitor(fromPath: Path, toPath: Path) extends SimpleFileVisitor[Path] {

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.copy(file, toPath.resolve(fromPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
      FileVisitResult.CONTINUE
    }

    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
      val targetPath = toPath.resolve(fromPath.relativize(dir))
      if (!Files.exists(targetPath)) {
        Files.createDirectory(targetPath)
      }
      FileVisitResult.CONTINUE
    }
  }

  def acquireToken(): String = {

    if (currentToken != null) {
      return currentToken
    }

    lock.lock()
    try {
      if (currentToken != null) {
        return currentToken
      }
      logger.info("Starting getting new token")
      auth(config.getString("chin_news.instagram.login"),
        config.getString("chin_news.instagram.password"),
        config, (accessToken, failureListener) => {
          currentToken = accessToken
        })

      while (currentToken == null) {
        Thread.sleep(1000)
        logger.info("Waiting for the new token...")
      }
      currentToken
    } finally {
      lock.unlock()
    }
  }


  def auth(name: String, password: String, config: Config, authenticated: (String, FailureListener) => Unit): Unit = {

    val failureListener = new FailureListener
    var server: Server = null
    val service = HttpService {
      case req@Method.GET -> Root =>
        try {
          logger.info(s"Received a request $req")

          val code = req.params.get("code").get
          val body = Http("https://api.instagram.com/oauth/access_token").postForm(Seq(
            "client_id" -> client_id,
            "client_secret" -> client_secret,
            "grant_type" -> "authorization_code",
            "redirect_uri" -> "http://localhost:8080",
            "code" -> code))
            .asString.body

          val access_token = Parse.parseWith(body, _.field("access_token").flatMap(_.string).get, msg => msg)

          failureListener.listen(e => runSlimerjs(
            config.getString("chin_news.simplerjs"),
            config.getString("chin_news.os"),
            config.getString("chin_news.crawler.host"),
            config.getString("chin_news.crawler.port"),
            name,
            password))
          authenticated(access_token.toString, failureListener)
        }
        catch {
          case e: Exception => println("exception caught: " + e);
        }
        Ok("result")
    }

    println("server started")

    server = JettyBuilder.bindHttp(8080)
      .mountService(service, "/")
      .run

    runSlimerjs(
      config.getString("chin_news.simplerjs"),
      config.getString("chin_news.os"),
      config.getString("chin_news.crawler.host"),
      config.getString("chin_news.crawler.port"),
      name,
      password)
  }

  def runSlimerjs(slimerjs: String, os: String, serverHost: String, serverPort: String, name: String,
                  password: String): Unit = {
    val tempDirPath = Files.createTempDirectory("slimerjs")

    val uri = getClass.getClassLoader.getResource(s"instagram_login.js").toURI

    var instagramLoginJsResourcePath: Path = null

    logger.info("Copying instagram_login.js. Path: " + uri.toString)
    if (uri.toString.contains("!")) {
      val pathParts: Array[String] = uri.toString.split("!/")
      val fs = FileSystems.newFileSystem(URI.create(pathParts(0)), Collections.emptyMap[String, String]())
      instagramLoginJsResourcePath = fs.getPath(pathParts(1))
    } else {
      instagramLoginJsResourcePath = Paths.get(uri)
    }

    val instagramLoginJsPath = Files.copy(instagramLoginJsResourcePath, tempDirPath.resolve("instagram_login.js"))

    logger.info(s"Running slimerjs: '$slimerjs $instagramLoginJsPath $client_id $serverHost $serverPort $name $password'")

    val pb = new ProcessBuilder("xvfb-run",
      slimerjs, instagramLoginJsPath.toString, client_id, serverHost, serverPort, name, password)
    pb.redirectOutput(Redirect.INHERIT)
    pb.redirectError(Redirect.INHERIT)
    pb.start().waitFor()

    logger.info(s"Slimerjs started")

  }

}
