package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

import scalanative.unsafe.*
import scalanative.unsigned.*
import mainargs.ParserForClass

given Conversion[UnsafeCursor, LoggableMessage] =
  LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))

given Conversion[Token, LoggableMessage] =
  LoggableMessage[Token](a => new TextOutput(a.toString()))

def bold(s: String) =
  fansi.Bold.On(s)

def init(config: Config) =
  val name = config.template // TODO: handle file and full URLs
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
    val settings = if config.yes.value then defaults else interactive(defaults)
    Err.assert(
      settings.values.contains("name") || config.out.nonEmpty,
      "Settings do not contain a [name] propertty, and --out parameter wasn't specified: cannot infer the directory where to put templated code"
    )
    val dest = config.out.getOrElse {
      val hyphenName =
        Format.Lower(Format.Hyphen(settings.values("name").stringValue))

      os.pwd / hyphenName
    }
    val results = fillDirectory(
      g8Sources,
      dest,
      settings,
      overwrite = true,
      makeOrigin =
        rp => FileOrigin.FromURL(githubAddress + "/blob/main/src/main/g8", rp),
      skip = Set(g8Sources / "default.properties")
    )
    println(
      s"\n✅ Template ${fansi.Bold.On(name)} applied in ${fansi.Bold.On(dest.toString)}"
    )
    results.toVector.sorted.foreach { p =>
      println("- " + fansi.Color.Green(p.relativeTo(dest).toString))
    }
  catch
    case exc =>
      os.remove.all(clone_dest)
      throw exc
  end try
end init

def interactive(defaults: Settings) =
  val settings = Map.newBuilder[String, PropertyValue]

  println(fansi.Underlined.On("Customise this template:"))

  defaults.values.toList.sortBy((k, _) => defaults.ordering.apply(k)).foreach {
    case (field, default) =>
      val prompt =
        fansi
          .Str(
            "- ",
            fansi.Bold.On(field),
            " (",
            fansi.Back.White(fansi.Color.Black(default.stringValue)),
            "): "
          )
          .render

      val newValue = io.StdIn.readLine(prompt).trim() match
        case "" =>
          default
        case other => PropertyValue.Str(other)

      settings.addOne(field -> newValue)

  }

  Settings(settings.result(), defaults.ordering)
end interactive

@main def scalaboot(args: String*) =
  // val path =
  //   try os.Path(out)
  //   catch
  //     case exc =>
  //       os.pwd / out

  val config =
    ParserForClass[Config].constructOrExit(args, allowPositional = true)

  init(config)

end scalaboot
