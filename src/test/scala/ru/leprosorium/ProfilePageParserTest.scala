package ru.leprosorium

import org.junit.runner.RunWith
import org.scalatest.{ShouldMatchers, FunSpec}
import org.scalatest.junit.JUnitRunner
import ru.leprosorium.Datasource.ProfilePageParser
import ru.leprosorium.UserProfile.LeproUser

@RunWith(classOf[JUnitRunner])
class ProfilePageParserTest extends FunSpec with ShouldMatchers {

  describe("ProfilePageParser") {
    it("should parse profile") {
      ProfilePageParser.karmaProvider = x  => x match {
        case 66812 => 1
        case 67599 => 2
        case 69844 => 3
        case 73871 => 4
      }
      val resp = Datasource.ProfilePageParser.parse(classOf[ProfilePageParserTest].getResourceAsStream("/karatist.htm"))
      resp should be(Some(List(LeproUser(66812, "Kuderro", 1), LeproUser(67599, "Dzigha", 2), LeproUser(69844, "Kurt-IX", 3), LeproUser(73871, "Nik-Faber", 4))))
    }
  }

}
