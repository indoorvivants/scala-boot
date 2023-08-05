package scalaboot

import util.chaining.*
import scala.util.boundary, boundary.break

enum Interpolation:
  case Variable(name: String)
  case Comment

enum Token:
  case StringFragment(content: String)
  case Inject(Interpolation: Interpolation)


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
          raise(curs, "Failed to capture interpolation")
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
