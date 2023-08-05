package scalaboot

import Console.*

class Err(msg: String)
    extends Exception(msg)
    with scala.util.control.NoStackTrace:
  final override def toString = Err.render(msg)

object Err:

  def apply(msg: String): Err = new Err(msg)
  def raise(msg: String): Nothing = throw apply(msg)
  def raise(msg: String, source: Source, pos: Option[Pos] = None): Nothing =
    val withContext = source match
      case Source.Str(text) => ""
      case Source.File(path) =>
        val lineAndCol =
          pos.map { p =>

            var line = 0
            var col = 0
            val contents = os.read(path)

            var stop = false
            var i = 0
            while !stop do
              contents(i) match
                case '\n' => line += 1; col = 0
                case _    => col += 1

              i += 1
              stop = i >= p.toInt || i >= contents.length

            s"\n line $line column $col"
          }

        msg + s"\n  at [$path]" + lineAndCol.getOrElse("")

    throw apply(withContext)
  end raise
  def assert(cond: Boolean, msg: => String): Unit =
    if !cond then Err.raise(msg)
  def render(msg: String): String =

    val maxLineLength = msg.linesIterator.map(_.length).max
    val header = "-" * maxLineLength
    val fireMsg = msg.linesIterator.map(_.trim).map("🔥 " + _).mkString("\n")
    val newMsg = header + "\n" + fireMsg + "\n" + header
    redLines("\n " + newMsg)

  def redLines(s: String) =
    if !colors then s
    else s.linesIterator.map(_red).mkString(System.lineSeparator())

  private lazy val colors = true

  def _blue(s: String) = if !colors then s else CYAN + s + RESET
  def _red(s: String) = if !colors then s else RED + s + RESET
end Err
