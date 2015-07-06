package ru.leprosorium

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSpec}
import org.scalatest.junit.JUnitRunner
import ru.leprosorium.UserProfile.LeproUser

@RunWith(classOf[JUnitRunner])
class ParserTest extends FunSpec with ShouldMatchers {

  describe("ProfileParser") {

    it("should parse the profile correctly") {
      val user = Datasource.Parser.parse(classOf[ParserTest].getResourceAsStream("/dump.txt"))
      user should be(Some(LeproUser(56709, "le_big_mac", -2)))
    }

  }

}
