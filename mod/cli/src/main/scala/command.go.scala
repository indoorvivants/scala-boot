package scalaboot

import template.*
import scribe.Level
import os.Path
import scala.util.matching.Regex

private def discoverLayout(cloneDest: Path): (sources: Path, propsFile: Path) =
  val g8Sources = cloneDest / "src" / "main" / "g8"
  val g8Props = g8Sources / "default.properties"
  val props = cloneDest / "default.properties"

  if os.exists(g8Sources) && os.exists(g8Props) then
    scribe.debug("giter8 layout discovered")
    (sources = g8Sources, propsFile = g8Props)
  else if os.exists(props) then
    scribe.debug("root layout discovered")
    (sources = cloneDest, propsFile = props)
  else
    Err.raise(
      s"Cloned template doesn't look like giter8 layout ([$g8Sources] doesn't exist) or root layout ([$props] doesn't exist)"
    )
  end if
end discoverLayout

def commandGo(config: CLI.Go) =
  if config.verbose then
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()

  val BootstrapResult(cloneDest, makeOrigin) = bootstrap(config.template)
  try
    val discovery = discoverLayout(cloneDest)
    val props = readProperties(discovery.propsFile)
    scribe.debug(s"Properties: $props")
    val defaults = MakeDefaults(props, MavenFunc.all)
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

    val verbatim: List[Regex] =
      def read(s: String) =
        s
          .split(" ")
          .toList
          .map(
            _.split('*').map(java.util.regex.Pattern.quote).mkString("(.*)").r
          )
      props.properties.get("verbatim") match
        case Some(Tokenized(Vector(StringTemplateExpr.Lit(value)), source)) =>
          read(value)
        case Some(
              Tokenized(
                Vector(
                  StringTemplateExpr.Many(List(StringTemplateExpr.Lit(value)))
                ),
                source
              )
            ) =>
          read(value)
        case Some(other) =>
          scribe.warn(
            s"verbatim property has unexpected shape (it should be just a literal string with patterns): $other"
          )
          Nil
        case None => Nil
      end match
    end verbatim

    val files =
      val propsFileRelativePath = discovery.propsFile.relativeTo(cloneDest)
      listFiles(cloneDest).filterNot(_ == propsFileRelativePath).toSet

    val results = fillDirectory(
      discovery.sources,
      dest,
      settings,
      overwrite = true,
      makeOrigin = makeOrigin,
      allowedPaths = files,
      verbatimPatterns = verbatim
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
      os.remove.all(cloneDest)
      throw exc
  end try
end commandGo
