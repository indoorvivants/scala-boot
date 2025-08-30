package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

class Evaluator(props: Map[String, PropertyValue]):
  extension [A, S](t: Either[Error, A])
    inline def raise(using l: boundary.Label[Either[Error, S]]): A =
      t match
        case Left(a)      => boundary.break(Left(a))
        case Right(value) => value

  def evaluate(expr: StringTemplateExpr): Either[Error, String] =
    val builder = new StringBuilder()
    boundary[Either[Error, String]]:
      @tailrec
      def go(e: List[StringTemplateExpr]): Unit =
        e match
          case Nil       =>
          case h :: next =>
            h match
              case StringTemplateExpr.Lit(str) =>
                builder.appendAll(str)
                go(next)
              case StringTemplateExpr.Variable(name, modifiers) =>
                // TODO: modifiers
                props
                  .get(name) match
                  case Some(value) =>
                    builder.appendAll(modifiers.foldLeft(value.stringValue) {
                      (acc, modifier) =>
                        modifier(acc)
                    })
                    go(next)
                  case _ =>
                    boundary.break(
                      Left(Error(s"Variable $name not found"))
                    )
              case StringTemplateExpr.If(cond, thenExpr, elsif, elseExpr) =>
                val truth = BoolEvaluator.evaluate(props, cond)
                if truth then go(thenExpr +: next)
                else if elsif.nonEmpty then
                  val matchingBranch = elsif.find { case (predicate, _) =>
                    BoolEvaluator.evaluate(props, predicate)
                  }
                  matchingBranch match
                    case Some((_, branch)) => go(branch +: next)
                    case None              => go(elseExpr.toList ++ next)
                else go(elseExpr.toList ++ next)
              case StringTemplateExpr.Many(exprs)    => go(exprs ++ next)
              case StringTemplateExpr.Comment(value) =>
                go(next)
            end match
      end go

      go(List(expr))
      Right(builder.toString())
  end evaluate
end Evaluator