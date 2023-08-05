package scalaboot

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum Interpolation:
  case Variable(name: String)
  case Comment

enum Token:
  case StringFragment(content: String)
  case Inject(Interpolation: Interpolation)

enum PropertyValue:
  case Str(value: String)

  def truthy: Boolean =
    val truths = Set("Y", "true", "yes", "foshizzle bruvizzle")
    this match
      case Str(value) if truths(value.toLowerCase()) => true
      case _                                         => false

case class Tokenized(tokens: Vector[Token])
case class Props(properties: Map[String, Tokenized])
case class Settings(values: Map[String, PropertyValue])

def fill(tokenized: Tokenized, settings: Settings) =
  val sb = new java.lang.StringBuilder
  tokenized.tokens.foreach { tok =>
    tok match
      case Token.StringFragment(content) => sb.append(content)
      case Token.Inject(interp) =>
        interp match
          case Interpolation.Variable(name) =>
            settings.values.get(name) match
              case None => Err.raise(s"Unknown variable [$name] in interpolation")
              case Some(value) =>
                value match
                  case PropertyValue.Str(value) => sb.append(value)

          case Interpolation.Comment =>

  }
  sb.toString()
end fill

def tokenize(s: String): Tokenized =
  val b = Vector.newBuilder[Token]
  parse(using Context(text = s))(b.addOne)
  Tokenized(b.result())

def readProperties(file: os.Path) =
  val props = java.util.Properties()
  props.load(file.getInputStream)

  val propsBuilder = Map.newBuilder[String, Tokenized]
  val names = props.stringPropertyNames()
  names.forEach { name =>
    propsBuilder.addOne(name -> tokenize(props.getProperty(name)))
  }
  Props(propsBuilder.result())
end readProperties

def makeDefaults(props: Props) =
  val lst = props.properties.toList.sortBy(_._1)

  @tailrec
  def go(
      rem: List[(String, Tokenized)],
      acc: Map[String, PropertyValue],
      visited: List[String],
      iterations: Int
  ): (Map[String, PropertyValue], Int) =
    rem match
      case (cur @ (name, Tokenized(tokens))) :: next =>
        tokens match
          case Vector(Token.StringFragment(single)) =>
            val newAcc = acc.updated(
              name,
              PropertyValue.Str(single)
            )

            go(next, newAcc, Nil, iterations + 1)
          case other =>
            val hasUnresolved = tokens.exists {
              case Token.Inject(Interpolation.Variable(interp))
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
              go(next :+ cur, acc, name +: visited, iterations + 1)
            else
              val sb = new StringBuilder
              tokens.foreach { tok =>
                tok match
                  case Token.StringFragment(content) => sb.append(content)
                  case Token.Inject(Interpolation.Variable(interp)) =>
                    acc(interp) match
                      case PropertyValue.Str(value) => sb.append(value)
                  case Token.Inject(Interpolation.Comment) =>

              }
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
  scribe.debug(s"Interpolating properties took $iterations iterations")

  Settings(result)
end makeDefaults

def parse(using Context)(tok: Token => Unit): Unit =
  val currentFragment = new java.lang.StringBuilder

  inline def sendStringAndRest() =
    val last = currentFragment.toString()

    if last.nonEmpty then tok(Token.StringFragment(last))
    currentFragment.setLength(0)

  val skipped = traverseStr { (curs, move) =>
    scribe.debug(curs.toString())
    curs.char match
      case '$' if curs.prevChar() != Opt('\\') =>
        move.skipOne()

        collectInterpolation(curs.continue).fold { case (interp, skip) =>
          move.skipAhead(skip)
          sendStringAndRest()
          tok(Token.Inject(interp))
        } {
          move.goBackOne()
          raise(curs.pos, "Failed to capture interpolation")
        }

      case other => currentFragment.append(other)
    end match

  }

  sendStringAndRest()

end parse

def collectInterpolation(
    continue: Continue
)(using Context): Opt[(Interpolation, Skip)] =
  val interpRaw = new java.lang.StringBuilder
  inline def charAllowed(c: Char) =
    c.isLetterOrDigit || c == '_'
  boundary:
    val skipped = traverseContinue(continue) { (curs, move) =>
      scribe.debug(curs.toString)
      curs.char match
        case '$' if curs.prevChar() != Opt('\\') =>
          move.abort()
        case other =>
          if charAllowed(other) then interpRaw.append(other)
          else break(Opt.empty)
    }

    Opt(Interpolation.Variable(interpRaw.toString) -> skipped)
end collectInterpolation
