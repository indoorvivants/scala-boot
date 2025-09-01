package scalaboot

import decline_derive.CommandApplication

import template.*

def readProperties(file: os.Path) =
  val props = java.util.Properties()
  props.load(file.getInputStream)

  val propsBuilder = List.newBuilder[(String, Tokenized)]
  val names = props.stringPropertyNames()
  names.forEach { name =>
    propsBuilder.addOne(
      name -> tokenizeSource(Source.Str(props.getProperty(name)))
    )
  }
  Props(
    propsBuilder.result().toMap,
    propsBuilder.result().map(_._1).zipWithIndex.toMap
  )
end readProperties

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

def interactive(defaults: Settings) =
  val settings = Map.newBuilder[String, PropertyValue]

  println(fansi.Underlined.On("Customise this template:"))

  import cue4s.*

  Prompts.sync.use: prompts =>
    defaults.values.toList
      .sortBy((k, _) => defaults.ordering.apply(k))
      .foreach { case (field, default) =>
        val value =
          prompts.text(s"$field", _.default(default.stringValue)).getOrThrow
        settings.addOne(field -> PropertyValue.Str(value))
      }

  Settings(settings.result(), defaults.ordering)
end interactive

@main def hello(args: String*) =
  CommandApplication.parseOrExit[CLI](args) match
    case cgo: CLI.Go      => commandGo(cgo)
    case srch: CLI.Search => commandSearch(srch)
