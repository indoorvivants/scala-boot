package scalaboot

import mainargs.{main, arg, ParserForClass, Flag}
import mainargs.TokensReader

@main(name = "scala-boot")
case class Config(
    @arg(positional = true)
    template: String,
    @arg(short = 'b', doc = "Resolve a template within a given branch")
    branch: Option[String],
    @arg(short = 't', doc = "Resolve a template within a given tag")
    tag: Option[String],
    @arg(short = 'o', doc = "Output directory")
    out: Option[os.Path],
    @arg(short = 'y', doc = "Use default values")
    yes: Flag
)

given TokensReader.Simple[os.Path] with
  def shortName = "path"
  def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))
