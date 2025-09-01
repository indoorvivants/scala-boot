package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

object FillString:
  def apply(tokenized: Tokenized, settings: Settings) =
    val sb = new java.lang.StringBuilder

    @annotation.tailrec
    def handle(ste: Vector[StringTemplateExpr]): Unit =
      if ste.nonEmpty then
        val rest = ste.tail
        ste.head match
          case StringTemplateExpr.Lit(value) =>
            sb.append(value)
            handle(rest)
          case StringTemplateExpr.Variable(name, formats) =>
            settings.values.get(name) match
              case None =>
                Err.raise(
                  s"Unknown variable [$name] in interpolation"
                )
              case Some(value) =>
                value match
                  case PropertyValue.Str(value) =>
                    sb.append(applyFormats(value, formats))
            end match
            handle(rest)

          case StringTemplateExpr.Comment(_) =>
            handle(rest)
          case StringTemplateExpr.Many(exprs) =>
            handle(exprs.toVector ++ rest)
          case StringTemplateExpr.If(condition, thenExpr, elseIf, elseExpr) =>
            val eval = BoolEvaluator.evaluate(settings.values, _)
            if eval(condition) then handle(thenExpr +: rest)
            else
              val elseBranch =
                elseIf.find(e => eval(e._1)).map(_._2).orElse(elseExpr)

              elseBranch match
                case Some(value) =>
                  handle(value +: rest)
                case None =>
                  handle(rest)
            end if

        end match
    end handle

    handle(tokenized.tokens)
    sb.toString()
  end apply
end FillString