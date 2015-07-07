package ru.leprosorium

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSpec, ShouldMatchers}
import ru.leprosorium.UserProfile.LeproUser

@RunWith(classOf[JUnitRunner])
class SimpleProfileParserTest extends FunSpec with ShouldMatchers {

  describe("ProfileParser") {

    it("should parse the profile correctly") {
      val user = Datasource.SimpleProfileParser.parse(classOf[SimpleProfileParserTest].getResourceAsStream("/dump.txt"))
      user should be(Right(LeproUser(56709, "le_big_mac", -2)))
    }

  }

}
