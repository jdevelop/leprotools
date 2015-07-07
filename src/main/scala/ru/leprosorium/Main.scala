package ru.leprosorium

object Main extends App {

  import Datasource._

  (1 to 100000).toStream.map(SimpleProfile.getProfile(_)).flatten

}
