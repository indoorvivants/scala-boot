package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum Modifier:
  case Format(labels: Seq[Formatter])

  def apply(value: String) = this match
    case Format(labels) => Formatter.applyAll(value, labels)