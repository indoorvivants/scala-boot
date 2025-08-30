package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum BoolExpr:
  case PropCheck(name: String)
  case And(left: BoolExpr, right: BoolExpr)
  case Or(left: BoolExpr, right: BoolExpr)
  case Neg(expr: BoolExpr)