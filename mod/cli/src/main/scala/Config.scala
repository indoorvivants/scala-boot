package scalaboot

import mainargs.{main, arg, ParserForClass, Flag}
import mainargs.TokensReader

@main
case class Config(
    @arg(positional = true)
    template: String,
    @arg(short = 'b', doc = "Resolve a template within a given branch")
    branch: Option[String] = None,
    @arg(short = 't', doc = "Resolve a template within a given tag")
    tag: Option[String] = None,
    @arg(short = 'o', doc = "Output directory")
    out: Option[os.Path] = None,
    @arg(short = 'y', doc = "Use default values")
    yes: Flag
)
object Config:
  given ParserForClass[Config] = ParserForClass[Config]

given TokensReader.Simple[os.Path] with
  def shortName = "path"
  def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))

@main
case class SearchConfig(
    @arg(positional = true, doc = "search query")
    query: String,
    @arg(doc = "URI of the Scala Boot server")
    api: Option[String] = None,
    @arg(short = 'i', doc = "Interactively select a template to apply")
    interactive: Flag,
    @arg(short = 'a', doc = "Show all results instead of the top 5")
    all: Flag,
    @arg(short = 'y', doc = "When template is selected in interactive mode, apply it with defaults")
    yes: Flag
)
object SearchConfig:
  given ParserForClass[SearchConfig] = ParserForClass[SearchConfig]
