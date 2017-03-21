package httpsserver

import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{KeyManagerFactory, SSLContext}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

object Server extends App {

  val serverContext: ConnectionContext = {
    val password = "abcdef".toCharArray
    val context = SSLContext.getInstance("TLS")
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(getClass.getClassLoader.getResourceAsStream("keys/server.p12"), password)
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    // start up the web server
    ConnectionContext.https(context)
  }

  val echoService: Flow[Message, Message, _] = Flow[Message].map {
    case TextMessage.Strict(txt) => TextMessage("ECHO: " + txt)
    case _ => TextMessage("Message type unsupported")
  }

  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()
  import system._

  val route: Route = Route(
    path("ws") {
      getFromResource("web/index.html")
    } ~
    path("ws-echo") {
      get {
        handleWebSocketMessages(echoService)
      }
    } ~
    complete("ok"))

  Http().bindAndHandle(route, interface = "0.0.0.0", port = 8443, connectionContext = serverContext)
}
