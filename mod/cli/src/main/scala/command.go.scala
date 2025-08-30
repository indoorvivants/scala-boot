package scalaboot

import template.*
import scribe.Level

def commandGo(config: CLI.Go) =
  if config.verbose then
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()

  val BootstrapResult(clone_dest, makeOrigin) = bootstrap(config.template)
  try
    val g8Sources = clone_dest / "src" / "main" / "g8"
    Err.assert(
      g8Sources.toIO.isDirectory(),
      s"Path [src/main/g8] doesn't exist in the template. Are you sure it's a Giter8-compatible template?"
    )
    scribe.debug(s"G8 sources discovered at [${g8Sources.toString}]")
    val props = readProperties(g8Sources / "default.properties")
    scribe.debug(s"Properties: $props")
    val defaults = MakeDefaults(props)
    scribe.debug(s"Defaults: $defaults")
    val settings = if config.yes then defaults else interactive(defaults)
    Err.assert(
      settings.values.contains("name") || config.out.nonEmpty,
      "Settings do not contain a [name] propertty, and --out parameter wasn't specified: cannot infer the directory where to put templated code"
    )
    val dest = config.out.getOrElse {
      val hyphenName =
        Modifier.Format(Seq(Formatter.Hyphen))(
          settings.values("name").stringValue
        )

      scribe.debug(s"Converted ${settings.values("name")} to $hyphenName")

      os.pwd / hyphenName
    }
    val results = fillDirectory(
      g8Sources,
      dest,
      settings,
      overwrite = true,
      makeOrigin = makeOrigin,
      skip = Set(g8Sources / "default.properties")
    )
    println(
      s"\n✅ Template ${fansi.Bold.On(config.template)} applied in ${fansi.Bold
          .On(dest.toString)}"
    )
    results.toVector.sorted.foreach { p =>
      println("- " + fansi.Color.Green(p.relativeTo(dest).toString))
    }
  catch
    case exc =>
      os.remove.all(clone_dest)
      throw exc
  end try
end commandGo
