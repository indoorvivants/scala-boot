package scalaboot

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum Interpolation:
  case Variable(name: String, formats: List[Format])
  case Comment

case class Token(fragment: Fragment, pos: Pos)

enum Fragment:
  case Str(content: String)
  case Inject(Interpolation: Interpolation)

enum PropertyValue:
  case Str(value: String)

  def truthy: Boolean =
    val truths = Set("Y", "true", "yes", "foshizzle bruvizzle")
    this match
      case Str(value) if truths(value.toLowerCase()) => true
      case _                                         => false

  def stringValue = this match
    case Str(value) => value
end PropertyValue

case class Tokenized(tokens: Vector[Token], source: Source)
case class Props(properties: Map[String, Tokenized], ordering: Map[String, Int])
case class Settings(
    values: Map[String, PropertyValue],
    ordering: Map[String, Int]
)

enum Format:
  case Lower, Upper, Hyphen, Norm, Capitalize, Decapitalize, Word, Camel,
    CamelLower, Start, Snake

  def apply(value: String): String = this match
    case Lower        => value.toLowerCase()
    case Upper        => value.toUpperCase()
    case Hyphen       => value.replace(' ', '-')
    case Norm         => Lower(Hyphen(value))
    case Capitalize   => value.capitalize
    case Decapitalize => ???
    case Word         => value.filter(c => c.isLetterOrDigit || c == '_')
    case Camel        => Word(Start(value))
    case Start        => value.split(" ").map(_.capitalize).mkString(" ")
    case Snake        => value.replace(" ", "_").replace(".", "_")

end Format

object Format:
  import Format.*
  def from(s: String) = Option {
    s match
      case "lower" | "lowercase"  => Lower
      case "hyphen" | "hyphenate" => Hyphen
      case "snake"                => Snake
      case "norm"                 => Norm

      // TODO: rest of cases
      case _ => null
  }
end Format

def tokenize(s: Source): Tokenized =
  val b = Vector.newBuilder[Token]
  parse(using Context(source = s))(b.addOne)
  Tokenized(b.result(), s)

def readProperties(file: os.Path) =
  val props = java.util.Properties()
  props.load(file.getInputStream)

  val propsBuilder = List.newBuilder[(String, Tokenized)]
  val names = props.stringPropertyNames()
  names.forEach { name =>
    propsBuilder.addOne(name -> tokenize(Source.Str(props.getProperty(name))))
  }
  Props(
    propsBuilder.result().toMap,
    propsBuilder.result().map(_._1).zipWithIndex.toMap
  )
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
      case (cur @ (name, Tokenized(tokens, source))) :: next =>
        tokens match
          case Vector(Token(Fragment.Str(single), _)) =>
            val newAcc = acc.updated(
              name,
              PropertyValue.Str(single)
            )

            go(next, newAcc, Nil, iterations + 1)
          case other =>
            val hasUnresolved = tokens.exists {
              case Token(Fragment.Inject(Interpolation.Variable(interp, _)), _)
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
                tok.fragment match
                  case Fragment.Str(content) => sb.append(content)
                  case Fragment.Inject(
                        Interpolation.Variable(interp, formats)
                      ) =>
                    acc(interp) match
                      case PropertyValue.Str(value) =>
                        sb.append(applyFormats(value, formats))
                  case Fragment.Inject(Interpolation.Comment) =>

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

  Settings(result, props.ordering)
end makeDefaults

def applyFormats(value: String, formats: List[Format]) =
  var valueM = value
  formats.foreach { f => valueM = f(valueM) }
  valueM

def parse(using Context)(tok: Token => Unit): Unit =
  val currentFragment = new java.lang.StringBuilder

  inline def sendStringAndRest(pos: Pos) =
    val last = currentFragment.toString()

    if last.nonEmpty then tok(Token(Fragment.Str(last), pos))
    currentFragment.setLength(0)

  val skipped = traverseStr { (curs, move) =>
    scribe.debug(curs.toString())
    curs.char match
      case '$' if curs.prevChar() != Opt('\\') =>
        move.skipOne()

        val (interp, skip) =
          collectInterpolation(curs.continue)

        sendStringAndRest(curs.pos)
        val interpStart = curs.pos
        move.skipAhead(skip)
        tok(Token(Fragment.Inject(interp), interpStart))

      case other => currentFragment.append(other)
    end match

  }

  sendStringAndRest(Pos(skipped.toInt))

end parse

def collectInterpolation(
    continue: Continue
)(using Context): (Interpolation, Skip) =
  val interpRaw = new java.lang.StringBuilder
  inline def charAllowed(c: Char) =
    c.isLetterOrDigit || c == '_'
  var formats = List.empty[Format]
  val skipped = traverseContinue(continue) { (curs, move) =>
    scribe.debug(curs.toString)
    curs.char match
      case '$' if curs.prevChar() != Opt('\\') =>
        move.abort()
      case ';' =>
        if interpRaw.length() > 0 then
          move.skipOne()
          val ((key, value), skip) = collectKeyValue(curs.continue)
          key match
            case "format" =>
              import Format.*
              formats = value
                .split(",")
                .map { str =>
                  Format.from(str) match
                    case None => raise(curs.pos, s"Unknown format [$str]")
                    case Some(value) =>
                      value

                }
                .toList
            case other => raise(curs.pos, s"Unknown attribute [$other]")
          end match

          move.skipAhead(skip)
        else
          raise(
            curs.pos,
            "[;] can follow the parameter name, but not immediately after $"
          )

      case other =>
        if charAllowed(other) then interpRaw.append(other)
        else raise(curs.pos, s"Unexpected symbol [$other] in interpolation")
    end match
  }

  Interpolation.Variable(interpRaw.toString, formats) -> skipped
end collectInterpolation

def collectKeyValue(continue: Continue)(using Context) =
  val (key, skipKey) = collectAlphaNumeric(continue)
  val skipped = traverseContinue(continue) { (c, move) =>
    move.skipAhead(skipKey)
    c.char match
      case '=' =>
        move.skipOne()
        move.abort()
      case other => raise(c.pos, s"Expected [=] got [${c.char}] instead")
  }
  val (value, skipValue) = collectQuotedString(
    Continue(continue.toInt + skipped.toInt)
  )

  (key.toString -> value) -> (Skip(skipKey.toInt + skipValue.toInt))
end collectKeyValue

def collectAlphaNumeric(continue: Continue)(using Context) =
  val strRaw = new java.lang.StringBuilder
  val skipped = traverseContinue(continue) { (curs, move) =>
    if curs.char.isLetterOrDigit then strRaw.append(curs.char)
    else move.abort()
  }

  strRaw.toString -> skipped
end collectAlphaNumeric

def collectQuotedString(continue: Continue)(using Context) =
  val strRaw = new java.lang.StringBuilder
  enum State:
    case Start, Inside, Close
  var state: State | Null = null
  val skipped = traverseContinue(continue) { (curs, move) =>
    curs.char match
      case '"' =>
        state = state match
          case State.Start => // empty string
            move.abort()
            State.Close

          case State.Inside =>
            if curs.prevChar() == Opt('\\') then
              strRaw.append('"')
              State.Inside
            else
              move.skipOne()
              move.abort()
              State.Close
          case State.Close => raise(curs.pos, "internal error :(")
          case null        => State.Start

      case other if state == State.Start =>
        state = State.Inside
        strRaw.append(other)

      case other if state == State.Inside =>
        strRaw.append(other)

  }
  strRaw.toString() -> skipped
end collectQuotedString
