package ru.leprosorium

import java.io.InputStream
import java.util.concurrent.TimeUnit

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.{BasicCookieStore, HttpClients, LaxRedirectStrategy}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.message.BasicNameValuePair
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import ru.leprosorium.UserProfile.{LeproUser, ProfileDatasource, ProfileParser}

import scala.collection.JavaConversions._

object Datasource {

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

  val cookieStore = new BasicCookieStore()

  def mkCookie(name: String, value: String): Unit = {
    val cookie = new BasicClientCookie(name, value)
    cookie.setDomain(".leprosorium.ru")
    cookie.setPath("/")
    cookie.setExpiryDate(DateTime.now().plusDays(30).toDate)
    cookieStore.addCookie(cookie)
  }

  mkCookie("uid", Config.Credentials.uid.toString)
  mkCookie("sid", Config.Credentials.session)

  val client = HttpClients.custom()
    .setDefaultRequestConfig(requestConfig)
    .setConnectionManager(cm)
    .setRedirectStrategy(new LaxRedirectStrategy())
    .setDefaultCookieStore(cookieStore)
    .build()

  private final val LOG = LoggerFactory.getLogger(Datasource.getClass)

  object SimpleProfile extends ProfileDatasource[LeproUser, Int] {

    override def getProfile(id: Int)(implicit ev: ProfileParser[LeproUser]) = {

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
          case x ⇒
            Console.err.println(s"Can't process response because of ${resp.getStatusLine}")
            None
        }
      } finally {
        HttpClientUtils.closeQuietly(resp)
      }
    }
  }

  implicit object SimpleProfileParser extends ProfileParser[LeproUser] {
    override def parse(is: InputStream): Option[LeproUser] = {

      import argonaut._

      import scalaz._

      val content = io.Source.fromInputStream(is).getLines.mkString("\n")

      Parse.parse(content) match {
        case -\/(errMsg) ⇒
          LOG.error(s"Can't parse JSON: $content ⇒ $errMsg")
          None
        case \/-(json) ⇒
          for {
            template <- json.field("template").flatMap(_.string)
            karma <- json.field("karma_vote")
          } yield {
            val doc = Jsoup.parse(template)
            val id = doc.select("div[data-id]").first().attr("data-id").toInt
            val name = doc.select("input[type=hidden]").first().attr("value")
            LeproUser(id, name, karma.number.flatMap(_.toInt).getOrElse(0))
          }
      }
    }
  }


  implicit object ProfilePageParser extends ProfileParser[Iterable[LeproUser]] {

    override def parse(is: InputStream): Option[Iterable[LeproUser]] = {
      val doc = Jsoup.parse(is, "UTF-8", "http://leprosorium.ru")
      val elems = doc.select("div[class=b-user_children] > nobr > a[class=c_user]")
      Some(elems.map {
        usr ⇒ LeproUser(usr.attr("data-user_id").toInt, usr.text(), 0)
      })
    }

  }

}