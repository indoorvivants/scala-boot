package scalaboot

import Console.*

class Err(msg: String)
    extends Exception(msg)
    with scala.util.control.NoStackTrace:
  final override def toString = Err.render(msg)

object Err:

  def apply(msg: String) = new Err(msg)
  def raise(msg: String) = throw apply(msg)
  def render(msg: String) =

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
