import mainargs.{main, arg, ParserForClass, Flag}
import mainargs.TokensReader

@main(name = "scala-boot/repo-indexer")
case class Config(
    @arg(doc = "Github org to find teampltes in")
    org: String
)
