package scalaboot.template

import java.io.InputStream

case class Context(
    source: Source
):
  lazy val text = source match
    case Source.Str(txt)   => txt
    case Source.Stream(is) =>
      scala.io.Source.fromInputStream(is).mkString

enum Source:
  case Str(text: String)
  case Stream(is: InputStream)
