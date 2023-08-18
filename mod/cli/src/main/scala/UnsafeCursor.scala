package scalaboot

case class Context(
    source: Source
):
  lazy val text = source match
    case Source.Str(txt) => txt
    case Source.File(path, _) =>
      os.read(path)

opaque type Pos = Int
object Pos:
  inline def apply(inline i: Int): Pos = i
  extension (c: Pos) inline def toInt: Int = c

opaque type Continue = Int
object Continue:
  inline def apply(inline i: Int): Continue = i
  extension (c: Continue) inline def toInt: Int = c

opaque type Skip = Int
object Skip:
  inline def apply(inline i: Int): Skip = i
  extension (c: Skip) inline def toInt: Int = c

opaque type Back = Int
object Back:
  inline def apply(inline i: Int): Back = i
  extension (c: Back) inline def toInt: Int = c

enum FileOrigin:
  case Local, None
  case FromURL(base: String, relative: os.RelPath)

enum Source:
  case Str(text: String)
  case File(path: os.Path, origin: FileOrigin = FileOrigin.Local)

class UnsafeCursor:
  private var _char: Char = 0
  private var _prevChar: () => Opt[Char] = () => Opt.empty
  private var _nextChar: () => Opt[Char] = () => Opt.empty
  private var _idx: Int = -1

  inline def char: Char = _char
  inline def continue: Continue = Continue(_idx)
  inline def prevChar(): Opt[Char] = _prevChar()
  inline def nextChar(): Opt[Char] = _nextChar()
  inline def pos: Pos = Pos(_idx)
  override def toString(): String =
    inline def red(c: String) = s"${Console.RED}$c${Console.RESET}"
    inline def sanitised(c: Opt[Char]) =
      val raw = c match
        case Opt.empty => red("_")
        case Opt(char) =>
          char match
            case '\n' => red("\\n")
            case _    => s"'${Console.GREEN}$char${Console.RESET}'"
      raw

    s"C[${sanitised(prevChar())} <<    ${sanitised(Opt(char))}   >> ${sanitised(nextChar())}]"
  end toString
end UnsafeCursor

object UnsafeCursor:
  case class Manipulate(
      setChar: Char => Unit,
      setPrevChar: (() => Opt[Char]) => Unit,
      setNextChar: (() => Opt[Char]) => Unit,
      setIndex: Int => Unit,
      inst: UnsafeCursor
  )

  def init: Manipulate =
    val curs = new UnsafeCursor

    Manipulate(
      setChar = c => curs._char = c,
      setPrevChar = c => curs._prevChar = c,
      setNextChar = c => curs._nextChar = c,
      setIndex = c => curs._idx = c,
      inst = curs
    )
  end init
end UnsafeCursor

case class Move(
    skipAhead: SkipAhead,
    goBack: GoBack,
    abort: Abort
):
  inline def skipOne() = skipAhead(Skip(1))
  inline def goBackOne() = goBack(Back(1))

opaque type Abort <: Function0[Unit] = () => Unit
object Abort:
  inline def apply(inline f: () => Unit): Abort = f

opaque type SkipAhead <: Function1[Skip, Unit] = Int => Unit
object SkipAhead:
  inline def apply(inline f: Int => Unit): SkipAhead = f

opaque type GoBack <: Function1[Back, Unit] = Int => Unit
object GoBack:
  inline def apply(inline f: Int => Unit): GoBack = f

def traverse(continue: Opt[Continue] = Opt.empty)(
    f: (UnsafeCursor, Move) => Unit
)(using ctx: Context): Skip =

  val s = ctx.text
  val start = continue.fold(_.toInt)(0)
  val finish = s.length - 1

  var i = start
  var stop = false

  val manip = UnsafeCursor.init

  manip.setPrevChar(() => if i == start then Opt.empty else Opt(s(i - 1)))
  manip.setNextChar(() => if i >= finish then Opt.empty else Opt(s(i + 1)))

  inline def position(p: Int) =
    if p <= finish && p >= start then
      manip.setChar(s(p))
      manip.setIndex(p)
      i = p
    else
      raise(
        manip.inst.pos,
        s"Attempt to move cursor to position [$p] outside of range [$start - $finish]"
      )

  val abort = Abort(() => stop = true)
  val skipAhead = SkipAhead: x =>
    position(i + x)
  val goBack = GoBack: x =>
    position(i - x)

  val move = Move(abort = abort, skipAhead = skipAhead, goBack = goBack)

  while !stop do
    stop = i > finish
    if !stop then
      position(i)

      f(manip.inst, move)

      i += 1
  end while

  Skip(i - start - 1)
end traverse

def raise(curs: Pos, msg: String)(using ctx: Context) =
  val idx = curs.toInt
  val contextSize = 15
  val left = ctx.text.slice((idx - contextSize).max(0), idx)
  val right =
    ctx.text.slice(idx + 1, (idx + contextSize).min(ctx.text.length - 1))
  val newLinesOnTheLeft = left.count(_ == '\n')
  val sanitisedLeft = left.replace("\n", fansi.Color.Red(raw"\n").toString)
  val sanitisedRight = right.replace("\n", fansi.Color.Red(raw"\n").toString)

  val topLine = sanitisedLeft + fansi.Bold
    .On(ctx.text(idx).toString())
    .toString + sanitisedRight
  val midLine = " " * (left.length() + newLinesOnTheLeft) + "^"
  val bottomLine = msg

  Err.raise(
    List("", topLine, midLine, bottomLine).mkString("\n"),
    ctx.source,
    Some(curs)
  )
end raise

inline def traverseStr(inline f: (UnsafeCursor, Move) => Unit)(using
    ctx: Context
) =
  traverse(Opt.empty)(f)

inline def traverseContinue(
    cont: Continue
)(inline f: (UnsafeCursor, Move) => Unit)(using
    ctx: Context
) =
  traverse(Opt(cont))(f)
