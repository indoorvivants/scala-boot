package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

object Parsers:
  import parsley as p, p.quick.*, p.syntax.character.*
  val formatterName =
    Formatter.values
      .flatMap(s => Array(s.toString, s.toString.toLowerCase()))
      .sortBy(_.length)
      .reverse
      .map(symbol)
      .reduce(_ | _)
      .mapFilter(Formatter.from)

  val modifierField =
    (stringOfMany(letterOrDigit) <~ '=' <~> ('"' ~> sepBy1(
      formatterName,
      ','
    ) <~ '"'))
      .collect:
        case ("format", labels) => Modifier.Format(labels)

  val modifiers = many(';' ~> modifierField)
  private val variable =
    (stringOfMany(letterOrDigit) <~> modifiers)
      .map(StringTemplateExpr.Variable(_, _))

  val underscoreModifiers =
    (symbol("__") ~> formatterName).map(f => List(Modifier.Format(Seq(f))))

  val variableName = stringOfMany(letterOrDigit)

  val variableModifiers = underscoreModifiers | modifiers

  val interpolate =
    val variable =
      (symbol("$") ~> variableName <~> variableModifiers <~ "$")
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

    (symbol("$if(") ~>
      boolExpr <~
      ")$" <~>
      lit <~>
      elsif <~>
      elseExpr <~
      "$endif$")
      .map:
        case (((bool, lit), elsif), elseExpr) =>
          StringTemplateExpr.If(bool, lit, elsif, elseExpr)
  end ifExpr

  lazy val sq: Parsley[StringTemplateExpr] = many(
    ifExpr | interpolate | lit
  ).map(StringTemplateExpr.Many(_)) <~ eof
end Parsers

case class Error(message: String) extends Throwable(message)

case class Tokenized(tokens: Vector[StringTemplateExpr], source: Source)
case class Props(properties: Map[String, Tokenized], ordering: Map[String, Int])
case class Settings(
    values: Map[String, PropertyValue],
    ordering: Map[String, Int]
)

def tokenizeSource(s: Source): Tokenized =
  val result =
    s match
      case Source.Str(text)  => Parsers.sq.parse(text)
      case Source.Stream(is) =>
        Parsers.sq.parse(scala.io.Source.fromInputStream(is).mkString)

  Tokenized(Vector(result.fold(Err.raise(_), identity)), s)
end tokenizeSource

def applyFormats(value: String, formats: List[Modifier]) =
  var valueM = value
  formats.foreach { f => valueM = f(valueM) }
  valueM
