package ru.leprosorium

import java.io.{File, PrintWriter, FileWriter}
import java.net.URLEncoder
import java.util.concurrent.Executors

import org.apache.http.client.methods.HttpGet
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Success, Failure}

object Main extends App {

  private final val LOG = LoggerFactory.getLogger(Main.getClass)

  object Conf extends ScallopConf(args) {

    val threads = opt[Int]("threads", default = Some(Runtime.getRuntime.availableProcessors()))

    val output = opt[String]("output", default = Some("."))

    val users = opt[Int]("users", default = Some(1000))

  }

  val executor = Executors.newFixedThreadPool(Conf.threads())

  implicit val ctx = ExecutionContext.fromExecutor(executor)

  val baseDir = new File(Conf.output())

  require((baseDir.exists() && baseDir.isDirectory) || baseDir.mkdirs(), s"Can't proceed with ${baseDir.getAbsolutePath}")

  import Datasource._

  val chunkSize = Conf.users() / Conf.threads()

  val processes = Future.sequence(for (chunk <- 1 to Conf.users() by chunkSize) yield {
    val f = Future {
      val writer = new FileWriter(new File(baseDir, s"lepra-$chunk-graph"))
      val profileWriter = new FileWriter(new File(baseDir, s"lepra-$chunk-info"))
      for (i <- chunk to (chunk + chunkSize)) {
        SimpleProfile.getProfile(i) match {
          case None ⇒ Left("")
          case Some(pr) ⇒
            println(s"Get ${pr}")
            profileWriter.write(
              s"""${pr.id}\t${pr.username}
                  |""".stripMargin)
            HTTPClient.withUrl(new HttpGet(HTTPClient.encodeUrl(s"https://leprosorium.ru/users/${URLEncoder.encode(pr.username, "UTF-8")}"))) {
              case is ⇒
                ProfilePageParser.parse(is).right.foreach {
                  prfls ⇒ prfls.foreach {
                    child ⇒
                      writer.write(
                        s""""${pr.username}" -> "${child.username}";
                                                                    |""".stripMargin
                      )
                  }
                }
                Right()
            }
        }
      }
      writer.flush()
      writer.close()
      profileWriter.flush()
      profileWriter.close()
    }
    f onComplete {
      case Success(_) ⇒ Console.err.println(s"Chunk ${chunk} complete")
      case Failure(x) ⇒ LOG.error(s"Can't complete future ${chunk}", x)
    }
    f
  })

  Await.ready(processes, 1 day)

  sys.exit(0)

}