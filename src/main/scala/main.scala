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

def clone(local: os.Path, remote: String) =
  import libgit.all.*
  Zone { implicit z =>
    val repo = alloc[Ptr[git_repository]]()
    val opts = alloc[git_clone_options]()

    inline def doOrError(inline f: Int, prefix: String) =
      Err.assert(
        f == 0,
        prefix + ": " + fromCString((!git_error_last()).message)
      )

    git_libgit2_init()

    doOrError(
      git_clone_options_init(opts, 1.toUInt),
      "failed to init git clone options"
    )

    doOrError(
      git_clone(repo, toCString(remote), toCString(local.toString), opts),
      "Failed to clone"
    )
  }
end clone

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
    fillDirectory(g8Sources, dest, defaults, overwrite = true)
  catch
    case exc =>
      os.remove.all(clone_dest)
      throw exc
  end try
end init

@main def scalaboot =
  init("scala-native/scala-native.g8", os.pwd / "test-out")
end scalaboot
