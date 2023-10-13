package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import scalaboot.protocol
import scala.scalanative.unsafe.Zone
import scribe.Level

inline def zone[A](inline f: Zone ?=> A) = Zone { z => f(using z) }

def connection_string() =
  sys.env.getOrElse(
    "DATABASE_URL", {
      val host = sys.env.getOrElse("PG_HOST", "localhost")
      val port = sys.env.getOrElse("PG_PORT", "5432")
      val password = sys.env.getOrElse("PG_PASSWORD", "mysecretpassword")
      val user = sys.env.getOrElse("PG_USER", "postgres")
      val db = sys.env.getOrElse("PG_DB", "postgres")

      s"postgresql://$user:$password@$host:$port/$db"
    }
  )

@main def server =
  zone {
    scribe.Logger.root
      .clearHandlers()
      .withHandler(
        writer = scribe.writer.SystemErrWriter,
        outputFormat = scribe.output.format.ANSIOutputFormat
      )
      .withMinimumLevel(Level.Debug)
      .replace()

    Db.use(connection_string()) { db =>
      import protocol.*

      lazy val apiKeySet =
        sys.env.get("SCALABOOT_API_KEY") match
          case None =>
            scribe.warn(
              "SCALABOOT_API_KEY variable not set, all requests will be unauthenticated"
            )
            None
          case Some(value) => Some(value)

      val auth = (key: Option[String]) =>
        (apiKeySet, key) match
          case (None, None)      => Right(())
          case (Some(str), None) => Left("API key missing")
          case (Some(expected), Some(provided)) =>
            if expected.trim() == provided.trim() then Right(())
            else Left("Provided API key is incorrect")
          case (None, Some(_)) => Right(())

      val handlers = List(
        repos.all.serverLogic[Id](name => Right(db.getAllRepos.toList)),
        repos.search.serverLogic[Id] { query =>
          Right(db.search(query).toList)
        },
        repos.add.serverSecurityLogic[Unit, Id](auth).serverLogicSuccess { _ =>
          db.addRepo(_)
        },
        repos.delete.serverSecurityLogic[Unit, Id](auth).serverLogicSuccess {
          _ => dr =>
            db.deleteRepo(dr.id)
        },
        repos.update.serverSecurityLogic[Unit, Id](auth).serverLogicSuccess {
          _ => db.updateRepo(_)
        }
      )

      snunit.SyncServerBuilder
        .setRequestHandler(toHandler(handlers))
        .build()
        .listen()

    }
  }
end server
