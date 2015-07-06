package ru.leprosorium

import java.io.{FileFilter, File}

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

object Config {

  private final val LOG = LoggerFactory.getLogger(Config.getClass)

  private[this] def configDir =
    sys.props.get("LEPRA_CONFIG_HOME").map {
      fname ⇒ new File(fname)
    } getOrElse {
      val home = sys.env("HOME")
      new File(new File(home), ".leprotools")
    }

  lazy val cfg = {
    val c = {
      val dir = configDir
      LOG.trace("Reading files from {}", configDir)
      dir.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean =
          pathname.getName.endsWith(".conf") || pathname.getName.endsWith(".properties")
      }).foldLeft(ConfigFactory.empty()) {
        case (lc, src) ⇒
          LOG.trace("Loading configuration from '{}'", src)
          lc.withFallback(ConfigFactory.parseFile(src))
      }
    }.withFallback(ConfigFactory.load("leproconfig"))
      .withFallback(ConfigFactory.load("leproconfig_reference"))
      .withFallback(ConfigFactory.load())
    if (LOG.isTraceEnabled)
      LOG.trace("Config rendered as {}", c.root().render())
    c
  }

  object Credentials {

    lazy val uid = cfg.getInt("lepra.credentials.uid")

    lazy val session = cfg.getString("lepra.credentials.session")

    lazy val csrf_token = cfg.getString("lepra.credentials.csrf_token")

  }


}