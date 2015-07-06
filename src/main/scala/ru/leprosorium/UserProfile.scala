package ru.leprosorium

import java.io.InputStream

/**
 * Holds user profile
 */
object UserProfile {

  case class LeproUser(id: Int, username: String, karma: Int)

  trait ProfileDatasource {

    def getProfile(id: Int)(implicit ev: ProfileParser): Option[LeproUser]

  }

  trait ProfileParser {

    def parse(is: InputStream): Option[LeproUser]

  }

}
