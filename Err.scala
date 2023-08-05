package scalaboot

import Console.*

class Err(msg: String)
    extends Exception(msg)
    with scala.util.control.NoStackTrace:
  final override def toString = Err.render(msg)

object Err:

  def apply(msg: String): Err = new Err(msg)
  def raise(msg: String): Nothing = throw apply(msg)
  def raise(msg: String, source: Source): Nothing =
    val withContext = source match
      case Source.Str(text) => ""
      case Source.File(path) =>
        msg + s"\n  at [$path]"

    throw apply(withContext)
  def assert(cond: Boolean, msg: String): Unit =
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
  // System.console() != null && System.getenv().get("TERM") != null

  def _blue(s: String) = if !colors then s else CYAN + s + RESET
  def _red(s: String) = if !colors then s else RED + s + RESET
end Err
