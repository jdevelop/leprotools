package ru.leprosorium

import java.io.InputStream
import java.net.{URI, URL}
import java.util.concurrent.TimeUnit

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.{LaxRedirectStrategy, HttpClients, BasicCookieStore}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.impl.cookie.BasicClientCookie
import org.joda.time.DateTime

/**
 * User: Eugene Dzhurinsky
 * Date: 7/6/15
 */
object HTTPClient {

  private val TIMEOUT = 1000

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

  private val cookieStore = new BasicCookieStore()

  private def mkCookie(name: String, value: String): Unit = {
    val cookie = new BasicClientCookie(name, value)
    cookie.setDomain(".leprosorium.ru")
    cookie.setPath("/")
    cookie.setExpiryDate(DateTime.now().plusDays(30).toDate)
    cookieStore.addCookie(cookie)
  }

  mkCookie("uid", Config.Credentials.uid.toString)
  mkCookie("sid", Config.Credentials.session)

  private val client = HttpClients.custom()
    .setDefaultRequestConfig(requestConfig)
    .setConnectionManager(cm)
    .setRedirectStrategy(new LaxRedirectStrategy())
    .setDefaultCookieStore(cookieStore)
    .build()

  def withUrl[T](req: HttpRequestBase)(f: InputStream ⇒ Option[T]): Either[String, T] = {
    val resp = client.execute(req)
    try {
      resp.getStatusLine.getStatusCode match {
        case 200 ⇒
          f(resp.getEntity.getContent).toRight(s"Can't parse content of ${req.getURI}")
        case x ⇒
          Left(resp.getStatusLine.getStatusCode.toString)
      }
    } finally {
      HttpClientUtils.closeQuietly(resp)
    }

  }

  def encodeUrl(url: String) = {
    val validUrl = new URL(url)
    new URI(validUrl.getProtocol, validUrl.getUserInfo, validUrl.getHost, validUrl.getPort, validUrl.getPath, validUrl.getQuery, "")
  }

}