package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import sttp.tapir.*
import scalaboot.protocol
import scala.scalanative.unsafe.Zone
import scalaboot.protocol.RepositoryInfo

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
    Db.use(connection_string()) { db =>
      import protocol.*
      val getAll =
        repos.all.serverLogic[Id](name => Right(db.getAllRepos.toList))

      val search = repos.search.serverLogic[Id] { query =>
        Right(db.search(query).toList)
      }

      val add = repos.add.serverLogic[Id] { inp =>
        scribe.info(
          "Adding repository with id : " + db
            .addRepo(
              inp
            )
            .toString
        )

        Right(())

      }

      snunit.SyncServerBuilder
        .setRequestHandler(toHandler(getAll :: search :: add :: Nil))
        .build()
        .listen()

    }
  }
end server
