package scalaboot.repo_indexer

import mainargs.{main, arg, Flag}

@main(name = "scala-boot/repo-indexer")
case class Config(
    @arg(doc = "Github org to find teampltes in")
    org: String,
    @arg(short = 'v', doc = "Enable verbose (really verbose logging)")
    verbose: Flag,
    @arg(doc = "Address of Scala Boot service")
    api: Option[String] = None,

)
