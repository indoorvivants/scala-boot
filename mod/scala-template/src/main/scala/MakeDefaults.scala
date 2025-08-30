package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

object MakeDefaults:
  def apply(props: Props) =
    val lst = props.properties.toList.sortBy(_._1)

    @tailrec
    def go(
        rem: List[(String, Tokenized)],
        acc: Map[String, PropertyValue],
        visited: List[String],
        iterations: Int
    ): (Map[String, PropertyValue], Int) =
      rem match
        case (currentKVPair @ (name, Tokenized(tokens, source))) :: next =>
          tokens match
            case Vector(StringTemplateExpr.Lit(single)) =>
              val newAcc = acc.updated(
                name,
                PropertyValue.Str(single)
              )

              go(next, newAcc, Nil, iterations + 1)
            case other =>
              val hasUnresolved = tokens.exists {
                // case Token(Fragment.Inject(Interpolation.Variable(interp, _)), _)
                case StringTemplateExpr.Variable(interp, _)
                    if !acc.contains(interp) =>
                  if props.properties.contains(interp) then true
                  else
                    Err.raise(
                      s"Unknown property in [$name] interpolation: [$interp]"
                    )
                case _ => false
              }

              if hasUnresolved && visited.contains(name) then
                Err.raise(
                  s"Loop detected in interpolation: ${visited.mkString(" -> ")}"
                )
              else if hasUnresolved then
                go(next :+ currentKVPair, acc, name +: visited, iterations + 1)
              else
                val sb = new StringBuilder
                @tailrec
                def handle(tokens: Vector[StringTemplateExpr]): Unit =
                  if tokens.nonEmpty then
                    val h = tokens.head
                    val rest = tokens.tail

                    h match
                      case StringTemplateExpr.Lit(value) =>
                        sb.append(value)
                        handle(rest)
                      case StringTemplateExpr.Comment(value) =>
                        handle(rest)
                      case StringTemplateExpr.If(
                            condition,
                            thenExpr,
                            elseIf,
                            elseExpr
                          ) =>

                        val eval = BoolEvaluator.evaluate(acc, _)
                        if eval(condition) then handle(thenExpr +: rest)
                        else
                          val elseBranch =
                            elseIf
                              .find(e => eval(e._1))
                              .map(_._2)
                              .orElse(elseExpr)

                          elseBranch match
                            case Some(value) =>
                              handle(value +: rest)
                            case None =>
                              handle(rest)
                        end if

                      case StringTemplateExpr.Variable(name, modifiers) =>
                        acc(name) match
                          case PropertyValue.Str(value) =>
                            sb.append(applyFormats(value, modifiers))
                        handle(rest)
                      case StringTemplateExpr.Many(exprs) =>
                        handle(exprs.toVector ++ rest)
                    end match

                end handle

                handle(tokens)

                go(
                  next,
                  acc.updated(name, PropertyValue.Str(sb.toString)),
                  Nil,
                  iterations + 1
                )
              end if

        case Nil => acc -> iterations
      end match
    end go

    val (result, iterations) = go(lst, Map.empty, Nil, 0)

    Settings(result, props.ordering)
  end apply
end MakeDefaults