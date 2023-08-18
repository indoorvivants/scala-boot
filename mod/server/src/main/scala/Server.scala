package scalaboot.server

import snunit.tapir.SNUnitIdServerInterpreter.*
import sttp.tapir.*
import scala.scalanative.unsafe.Zone

inline def zone[A](inline f: Zone ?=> A) = Zone { z => f(using z) }

val helloWorldEndpoint = endpoint.get
  .in("get-repo")
  .in(query[String]("name"))
  .out(stringBody)

@main def server =
  val str = "postgresql://postgres:mysecretpassword@localhost:5432/postgres"
  zone {
    Db.use(str) { db =>
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
