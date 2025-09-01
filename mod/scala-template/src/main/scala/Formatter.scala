package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum Formatter:
  case Lower, Upper, Hyphen, Norm, Capitalize, Decapitalize, Word, Camel,
    CamelLower, Start, Snake, Package, Packaged

  def apply(value: String): String = this match
    case Lower        => value.toLowerCase()
    case Upper        => value.toUpperCase()
    case Hyphen       => value.replace(' ', '-')
    case Norm         => Lower(Hyphen(value))
    case Capitalize   => value.capitalize
    case Decapitalize => ???
    case Word         => value.filter(c => c.isLetterOrDigit || c == '_')
    case Camel        => Word(Start(value))
    case CamelLower   => Camel(Lower(value)) // TODO: is it?
    case Start        => value.split(" ").map(_.capitalize).mkString(" ")
    case Snake        => value.replace(" ", "_").replace(".", "_")
    case Package      => value.replace(" ", ".")
    case Packaged     => value.replace(" ", ".").replace(".", "/")
end Formatter

object Formatter:
  import Formatter.*

  def applyAll(s: String, seq: Seq[Formatter]) =
    seq.foldLeft(s)((acc, f) => f(acc))

  def from(s: String) = Option {
    s.toLowerCase() match
      case "lower" | "lowercase"  => Lower
      case "hyphen" | "hyphenate" => Hyphen
      case "snake"                => Snake
      case "norm"                 => Norm
      case "camel"                => Camel
      case "upper" | "uppercase"  => Upper
      case "capitalize"           => Capitalize
      case "decapitalize"         => Decapitalize
      case "word"                 => Word
      case "camellower"           => CamelLower
      case "start"                => Start
      case "packaged"             => Packaged
      case "package"              => Package
      case _                      => null
  }
end Formatter
