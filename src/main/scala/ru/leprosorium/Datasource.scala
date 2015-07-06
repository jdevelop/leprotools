package ru.leprosorium

import java.io.InputStream
import java.util.concurrent.TimeUnit

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.{HttpClients, LaxRedirectStrategy}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import ru.leprosorium.UserProfile.{LeproUser, ProfileDatasource, ProfileParser}

import scala.collection.JavaConversions._

object Datasource {

  private final val LOG = LoggerFactory.getLogger(Datasource.getClass)

  object Profile extends ProfileDatasource {

    private val TIMEOUT = 20 * 1000

    private val socketConfig = SocketConfig.custom()
      .setSoTimeout(TIMEOUT)
      .setTcpNoDelay(true)
      .build()

    private val requestConfig = RequestConfig.custom()
      .setSocketTimeout(TIMEOUT)
      .setConnectTimeout(TIMEOUT)
      .build()

    private val cm = new PoolingHttpClientConnectionManager(5L, TimeUnit.SECONDS)

    cm.setDefaultSocketConfig(socketConfig)

    val client = HttpClients.custom()
      .setDefaultRequestConfig(requestConfig)
      .setConnectionManager(cm)
      .setRedirectStrategy(new LaxRedirectStrategy())
      .build()

    override def getProfile(id: Int)(implicit ev: ProfileParser) = {

      val req = new HttpPost("https://leprosorium.ru/ajax/user/get/")
      req.setEntity(new UrlEncodedFormEntity(
        Seq(
          new BasicNameValuePair("id", id.toString),
          new BasicNameValuePair("csrf_token", Config.Credentials.csrf_token)
        )
      ))
      val resp = client.execute(req)
      try {
        resp.getStatusLine.getStatusCode match {
          case 200 ⇒ ev.parse(resp.getEntity.getContent)
          case _ ⇒ None
        }
      } finally {
        HttpClientUtils.closeQuietly(resp)
      }
    }
  }

  object Parser extends ProfileParser {
    override def parse(is: InputStream): Option[LeproUser] = {

      import scalaz._, Scalaz._
      import argonaut._, Argonaut._

      val content = io.Source.fromInputStream(is).getLines.mkString("\n")

      Parse.parse(content) match {
        case -\/(errMsg) ⇒
          LOG.error(s"Can't parse JSON: $content ⇒ $errMsg")
          None
        case \/-(json) ⇒ for {
          template <- json.field("template").flatMap(_.string)
          karma <- json.field("karma_vote").flatMap(_.number)
        } yield {
            val doc = Jsoup.parse(template)
            val id = doc.select("div[data-id]").first().attr("data-id").toInt
            val name = doc.select("input[type=hidden]").first().attr("value")
            LeproUser(id, name, karma.toInt.getOrElse(0))
          }
      }
    }
  }

}