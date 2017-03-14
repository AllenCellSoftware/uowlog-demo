package org.uowlog.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.config.{Config,ConfigFactory}
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import org.slf4j.Logger
import org.uowlog._
import org.uowlog.http._
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.xml.Xhtml
import scala.util.Try
import scala.util.control.NonFatal

trait TemplateRoutes extends ProvenanceDirectives { this: UOWLogging =>
  implicit def system: ActorSystem
  def config: Config
  def servicePrefix: String
  def serviceRoute: Route
  def buildInfo: String = "Build info not available"

  private implicit def executionContext = system.dispatcher

  def healthRoute =
    pathPrefix("health")(
      path("ping")(complete("PONG")) ~
      path("selfTest")(complete("Not implemented")) ~
      path("deepTest")(complete("Not implemented")) ~
      pathEndOrSingleSlash(complete(HttpEntity(`text/html(UTF-8)`, Xhtml.toXhtml(
        <html><body><h1>{ config.getString("uowlog.program") }</h1><p>{ buildInfo }</p><ul>
          <li><a href="/health/ping">ping</a></li>
          <li><a href="/health/selfTest">selfTest</a></li>
          <li><a href="/health/deepTest">deepTest</a></li>
        </ul></body></html>
      ))))
    )

  val bindingPromise = Promise[List[ServerBinding]]()

  def adminRoute =
    pathPrefix("admin")(
      path("shutdown")(complete {
        log.info("Server stopping")
        bindingPromise.future
          .flatMap(bindings => Future.sequence(bindings.map(_.unbind()))) // trigger unbinding from the ports
          .onComplete(_ => system.terminate()) // and shutdown when done
        "shutting down"
      }) ~
      pathEndOrSingleSlash(complete(HttpEntity(`text/html(UTF-8)`, Xhtml.toXhtml(
        <html><body><h1>{ config.getString("uowlog.program") }</h1><p>{ buildInfo }</p><ul>
          <li><a href="/admin/shutdown">shutdown</a></li>
        </ul></body></html>
      ))))
    )

  def docRoute = reject

  def baseRoute =
    pathEndOrSingleSlash(complete(HttpEntity(`text/html(UTF-8)`, Xhtml.toXhtml(
      <html><body><h1>{ config.getString("uowlog.program") }</h1><p>{ buildInfo }</p><ul>
        <li><a href="/health">health</a></li>
        <li><a href="/docs">docs</a></li>
        <li><a href="/admin">admin</a></li>
        <li><a href={ "/" + servicePrefix }>{ servicePrefix }</a></li>
      </ul></body></html>
    ))))

  lazy val fullRoute = wrapRequestInUOWs.apply(Route.seal(healthRoute ~ adminRoute ~ docRoute ~ pathPrefix(servicePrefix)(serviceRoute) ~ baseRoute))
}

abstract class WebServer(args: Array[String]) extends TemplateRoutes with UOWLogging {
  val config = ConfigFactory.load
  val httpPort  = Try{ args(0).toInt }.getOrElse(config.getInt("http.port"))

  implicit val system = ActorSystem(config.getString("uowlog.program"), config)
  implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  def start() {
    val httpExt = Http()
    val httpBindingFuture = httpExt.bindAndHandle(fullRoute, "0.0.0.0", httpPort)
    bindingPromise.completeWith(Future.sequence(List(httpBindingFuture)))

    log.info(s"Server started on port $httpPort")
  }
}

object Server {
  def main(args: Array[String]) {
    new WebServer(args) {
      val servicePrefix = "hello"
      val serviceRoute = path("world")(complete("Howdy!\n"))
    }.start()
  }
}
