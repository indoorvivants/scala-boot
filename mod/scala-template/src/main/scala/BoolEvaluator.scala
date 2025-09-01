package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

object BoolEvaluator:
  def evaluate(
      props: Map[String, PropertyValue],
      expr: BoolExpr
  ): Boolean =
    expr match
      case BoolExpr.PropCheck(name) =>
        props
          .get(name) // TODO: handlem missing props
          .map(_.truthy)
          .getOrElse(false)
      case BoolExpr.And(left, right) =>
        evaluate(props, left) && evaluate(props, right)
      case BoolExpr.Or(left, right) =>
        evaluate(props, left) || evaluate(props, right)
      case BoolExpr.Neg(expr) =>
        !evaluate(props, expr)
end BoolEvaluator