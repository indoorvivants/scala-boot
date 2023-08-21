package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

import scalanative.unsafe.*
import scalanative.unsigned.*
import mainargs.ParserForClass
import scalaboot.client.Client

given Conversion[UnsafeCursor, LoggableMessage] =
  LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))

given Conversion[Token, LoggableMessage] =
  LoggableMessage[Token](a => new TextOutput(a.toString()))

def bold(s: String) =
  fansi.Bold.On(s)

case class BootstrapResult(localPath: os.Path, origin: os.RelPath => FileOrigin)

def bootstrap(template: String): BootstrapResult =
  def gitClone(gitAddress: String): os.Path =
    val clone_dest = os.temp.dir(prefix = "scala-boot")
    clone(clone_dest, gitAddress)
    clone_dest

  template match
    case s"file://$path" =>
      BootstrapResult(os.Path(path), _ => FileOrigin.Local)
    case full @ s"https://$gitAddress" =>
      BootstrapResult(gitClone(full), _ => FileOrigin.None)
    case full @ s"git://$gitAddress" =>
      BootstrapResult(gitClone(full), _ => FileOrigin.None)
    case name =>
      val githubAddress = s"https://github.com/$name"
      val gitAddress = githubAddress + ".git"
      BootstrapResult(
        gitClone(gitAddress),
        rp => FileOrigin.FromURL(githubAddress + "/blob/main/src/main/g8", rp)
      )
  end match
end bootstrap

def init(config: Config) =
  val BootstrapResult(clone_dest, makeOrigin) = bootstrap(config.template)
  try
    val g8Sources = clone_dest / "src" / "main" / "g8"
    Err.assert(
      g8Sources.toIO.isDirectory(),
      s"Path [src/main/g8] doesn't exist in the template. Are you sure it's a Giter8-compatible template?"
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

def initSearch(config: SearchConfig) =
  val backend = scalaboot.curl.CurlBackend()
  val client =
    Client.create(config.api.getOrElse(protocol.SCALABOOT_PRODUCTION))
  val sorted = client.search(config.query).sortBy(_.rank).reverse
  val maxStars = sorted.maxByOption(_.repo.stars).map(_.repo.stars).getOrElse(0)
  val starsFieldLength = maxStars.toString().length

  def renderStars(stars: Int) =
    val spacer = " " * (starsFieldLength - stars.toString.length)
    s"⭐️ ${fansi.Bold.On(stars.toString)}$spacer"

  val limit = if config.all.value then sorted.size else 5

  sorted.take(limit).zipWithIndex.foreach { case (result, idx) =>
    println(
      s"[${idx + 1}] " + renderStars(result.repo.stars) + "  " + fansi.Color
        .DarkGray("https://github.com/") + fansi.Color
        .Green(result.repo.name)
    )
  }

  if sorted.size > limit then
    println(
      s"Displaying only top 5 results, to see the remaining ${sorted.size - limit}, pass a ${fansi.Bold.On("-a")} flag"
    )

  if config.interactive.value then

    if config.yes.value then
      println(
        s"You've passed ${fansi.Bold.On("-y")} flag, so template will render with defaults automatically"
      )
    val defaultPrompt =
      "Please enter a number or press Enter to choose first one: "

    def go(prompt: String): Int =
      io.StdIn.readLine(prompt).trim() match
        case "" =>
          0
        case other =>
          other.toIntOption match
            case Some(num) if num >= 1 && num <= limit =>
              num - 1
            case _ =>
              go(defaultPrompt)

    val choice = go(defaultPrompt)

    val template = sorted(choice)

    val goConfig = Config(template = template.repo.name, yes = config.yes)

    init(goConfig)
  end if

end initSearch

object Commands:
  import mainargs.{main as entrypoint, arg, ParserForMethods, Flag}

  @entrypoint
  def go(
      config: Config
  ) =
    init(config)

  @entrypoint
  def search(
      config: SearchConfig
  ) =
    initSearch(config)

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
end Commands
