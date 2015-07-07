package ru.leprosorium

import java.io.InputStream

/**
 * Holds user profile
 */
object UserProfile {

  case class LeproUser(id: Int, username: String, karma: Int)

  trait ProfileDatasource[T, S] {

    def getProfile(id: S)(implicit ev: ProfileParser[T]): Option[T]

  }

  trait ProfileParser[T] {

    def parse(is: InputStream): Either[String, T]

  }

}
