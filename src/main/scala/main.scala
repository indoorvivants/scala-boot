package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

import scalanative.unsafe.*
import scalanative.unsigned.*

given Conversion[UnsafeCursor, LoggableMessage] =
  LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))

given Conversion[Token, LoggableMessage] =
  LoggableMessage[Token](a => new TextOutput(a.toString()))

def init(name: String, dest: os.Path) =
  val clone_dest = os.temp.dir(prefix = "scala-boot")
  try
    val githubAddress = s"https://github.com/$name"
    val gitAddress = githubAddress + ".git"
    clone(clone_dest, gitAddress)
    val g8Sources = clone_dest / "src" / "main" / "g8"
    Err.assert(
      g8Sources.toIO.isDirectory(),
      s"Path [src/main/g8] doesn't exist in [$githubAddress]. Are you sure it's a Giter8-compatible template?"
    )
    val props = readProperties(g8Sources / "default.properties")
    val defaults = makeDefaults(props)
    val results = fillDirectory(g8Sources, dest, defaults, overwrite = true)
    println("✅ " + Console.BOLD + dest + Console.RESET)
    results.toVector.sorted.foreach { p =>
      println("- " + Console.GREEN + p.relativeTo(dest) + Console.RESET)
    }
  catch
    case exc =>
      os.remove.all(clone_dest)
      throw exc
  end try
end init

@main def scalaboot(repo: String, out: String) =
  val path =
    try os.Path(out)
    catch
      case exc =>
        os.pwd / out

  init(repo, path)
end scalaboot
