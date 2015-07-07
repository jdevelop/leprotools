package ru.leprosorium

import java.io.InputStream

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.message.BasicNameValuePair
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import ru.leprosorium.UserProfile.{LeproUser, ProfileDatasource, ProfileParser}

import scala.collection.JavaConversions._

object Datasource {


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
      HTTPClient.withUrl(req) {
        case is ⇒ ev.parse(is)
      } match {
        case Right(p) ⇒ Some(p)
        case Left(err) ⇒
          LOG.error(s"Can't get page ${req.getURI} with userid [$id] ⇒ $err")
          None
      }
    }
  }

  implicit object SimpleProfileParser extends ProfileParser[LeproUser] {
    override def parse(is: InputStream): Either[String, LeproUser] = {

      import argonaut._

      import scalaz._

      val content = io.Source.fromInputStream(is).getLines.mkString("\n")

      Parse.parse(content) match {
        case -\/(errMsg) ⇒
          Left(s"Can't parse JSON: $content ⇒ $errMsg")
        case \/-(json) ⇒
          (for {
            template <- json.field("template").flatMap(_.string)
            karma <- json.field("karma_vote")
          } yield {
              val doc = Jsoup.parse(template)
              val id = doc.select("div[data-id]").first().attr("data-id").toInt
              val name = doc.select("input[type=hidden]").first().attr("value")
              LeproUser(id, name, karma.number.flatMap(_.toInt).getOrElse(0))
            }).toRight(s"Something is wrong with the ${content}")
      }
    }
  }


  implicit object ProfilePageParser extends ProfileParser[Iterable[LeproUser]] {

    override def parse(is: InputStream): Either[String, Iterable[LeproUser]] = {
      val doc = Jsoup.parse(is, "UTF-8", "http://leprosorium.ru")
      val elems = doc.select("div[class=b-user_children] > nobr > a[class=c_user]")
      Right(elems.map {
        usr ⇒ LeproUser(usr.attr("data-user_id").toInt, usr.text(), 0)
      })
    }

  }

}