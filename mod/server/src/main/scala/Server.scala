package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import sttp.tapir.*
import scala.scalanative.unsafe.Zone

inline def zone[A](inline f: Zone ?=> A) = Zone { z => f(using z) }

val helloWorldEndpoint = endpoint.get
  .in("get-repo")
  .in(query[String]("name"))
  .out(stringBody)

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
    Db.use(connection_string()) { db =>
      val helloWorld =
        helloWorldEndpoint.serverLogic[Id](name =>
          Right(db.getAllRepos.filter(_ == name).toString)
        )

      snunit.SyncServerBuilder
        .setRequestHandler(toHandler(helloWorld :: Nil))
        .build()
        .listen()

    }
  }
end server
