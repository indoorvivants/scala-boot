package scalaboot.protocol

import sttp.tapir.*
import sttp.tapir.json.upickle.*

val helloWorldEndpoint = endpoint.get
  .in("get-repo")
  .in(query[String]("name"))
  .out(stringBody)
