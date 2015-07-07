package ru.leprosorium

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSpec}
import org.scalatest.junit.JUnitRunner
import ru.leprosorium.UserProfile.LeproUser

@RunWith(classOf[JUnitRunner])
class ProfilePageParserTest extends FunSpec with ShouldMatchers {

  describe("ProfilePageParser") {
    it("should parse profile") {
      val resp = Datasource.ProfilePageParser.parse(classOf[ProfilePageParserTest].getResourceAsStream("/karatist.htm"))
      resp should be(Some(List(LeproUser(66812, "Kuderro", 0), LeproUser(67599, "Dzigha", 0), LeproUser(69844, "Kurt-IX", 0), LeproUser(73871, "Nik-Faber", 0))))
    }
  }

}
