package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum BoolExpr:
  case PropCheck(name: String)
  case And(left: BoolExpr, right: BoolExpr)
  case Or(left: BoolExpr, right: BoolExpr)
  case Neg(expr: BoolExpr)

enum Modifier:
  case Format(labels: Seq[Formatter])

  def apply(value: String) = this match
    case Format(labels) => Formatter.applyAll(value, labels)

enum StringTemplateExpr:
  case Lit(value: String)
  case Comment(value: String)
  case If(
      condition: BoolExpr,
      thenExpr: StringTemplateExpr,
      elseIf: List[(BoolExpr, StringTemplateExpr)],
      elseExpr: Option[StringTemplateExpr]
  )
  case Variable(name: String, modifiers: List[Modifier])
  case Many(exprs: List[StringTemplateExpr])
end StringTemplateExpr

object parsers:
  import parsley as p, p.quick.*, p.syntax.character.*
  val word =
    Formatter.values
      .map(_.toString.toLowerCase())
      .map(symbol)
      .reduce(_ | _)
      .mapFilter(Formatter.from)

  val modifierField =
    (stringOfMany(letterOrDigit) <~ '=' <~> ('"' ~> sepBy1(word, ',') <~ '"'))
      .collect:
        case ("format", labels) => Modifier.Format(labels)

  val modifiers = many(';' ~> modifierField)
  private val variable =
    (stringOfMany(letterOrDigit) <~> modifiers)
      .map(StringTemplateExpr.Variable(_, _))

  val interpolate =
    val variable =
      ("$" ~> stringOfMany(letterOrDigit) <~> modifiers <~ "$")
        .map(StringTemplateExpr.Variable(_, _))

    val comment =
      (symbol("$!") ~> manyTill(item, lookAhead("!$")).span <~ "!$")
        .map(StringTemplateExpr.Comment(_))

    comment | variable
  end interpolate

  private val skipWhitespace = many(whitespace.void).void

  private def lexeme[A](p: Parsley[A]): Parsley[A] = p <~ skipWhitespace
  private def token[A](p: Parsley[A]): Parsley[A] = lexeme(atomic(p))
  private def symbol(str: String): Parsley[String] = atomic(string(str))

  val lit = some("\\$" | satisfy(_ != '$')).span.map(StringTemplateExpr.Lit(_))
  val boolExpr =
    import parsley.expr.chain

    val number =
      token(
        (stringOfMany(letterOrDigit) <~ ".truthy").map(BoolExpr.PropCheck(_))
      )

    lazy val expr: p.Parsley[BoolExpr] =
      chain.left1(term)(token("||") as (BoolExpr.Or(_, _)))
    lazy val term = chain.left1(atom)(token("&&") as (BoolExpr.And(_, _)))
    lazy val atom = '(' ~> expr <~ ')' | number

    expr
  end boolExpr

  lazy val ifExpr: p.Parsley[StringTemplateExpr] =
    val elsif = many(symbol("$elseif(") ~> boolExpr <~ ")$" <~> lit)
    val elseExpr = option(symbol("$else$") ~> lit)

    (symbol(
      "$if("
    ) ~> boolExpr <~ ")$" <~> lit <~> elsif <~> elseExpr <~ "$endif$")
      .map:
        case (((bool, lit), elsif), elseExpr) =>
          StringTemplateExpr.If(bool, lit, elsif, elseExpr)
  end ifExpr

  lazy val sq: Parsley[StringTemplateExpr] = many(
    ifExpr | interpolate | lit
  ).map(StringTemplateExpr.Many(_)) <~ eof
end parsers

// @main def stringTemplate =
//   import parsers.*
//   println(interpolate.parse("""$scala213;format="camel"$"""))
//   println(interpolate.parse("""$scala213$"""))
//   println(
//     ifExpr.parse(
//       """$if(a.truthy)$ hello $elseif(b.truthy)$ world $else$ bye $endif$"""
//     )
//   )
//   val expr = sq.parse(STR).fold(sys.error(_), identity)
//   println(
//     Evaluator(
//       Map(
//         "package" -> "com.example",
//         "name"    -> "HELLO",
//         "a"       -> "yes",
//         "b"       -> "yes"
//       )
//     ).evaluate(expr)
//   )
// end stringTemplate

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

enum Formatter:
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
    case CamelLower   => Camel(Lower(value)) // TODO: is it?
    case Start        => value.split(" ").map(_.capitalize).mkString(" ")
    case Snake        => value.replace(" ", "_").replace(".", "_")

end Formatter

object Formatter:
  import Formatter.*

  def applyAll(s: String, seq: Seq[Formatter]) =
    seq.foldLeft(s)((acc, f) => f(acc))

  def from(s: String) = Option {
    s match
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
      case _                      => null
  }
end Formatter

case class Error(message: String) extends Throwable(message)

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

// enum Interpolation:
//   case Variable(name: String, formats: List[Format])
//   case Comment

// case class Token(fragment: Fragment, pos: Pos)

// enum Fragment:
//   case Str(content: String)
//   case Inject(Interpolation: Interpolation)

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

case class Tokenized(tokens: Vector[StringTemplateExpr], source: Source)
case class Props(properties: Map[String, Tokenized], ordering: Map[String, Int])
case class Settings(
    values: Map[String, PropertyValue],
    ordering: Map[String, Int]
)

def tokenize(s: Source): Tokenized =
  val result =
    s match
      case Source.Str(text)  => parsers.sq.parse(text)
      case Source.Stream(is) =>
        parsers.sq.parse(scala.io.Source.fromInputStream(is).mkString)

  Tokenized(Vector(result.fold(Err.raise(_), identity)), s)
end tokenize

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
end makeDefaults

def applyFormats(value: String, formats: List[Modifier]) =
  var valueM = value
  formats.foreach { f => valueM = f(valueM) }
  valueM

def fillString(tokenized: Tokenized, settings: Settings) =
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
end fillString
