package scalaboot.repo_indexer

import decline_derive.*

case class Config(
    @Help("Github org to find templates in")
    org: String,
    @Flag(default = false)
    @Short("v")
    verbose: Boolean,
    @Help("Address of Scala Boot service")
    api: Option[String] = None
) derives CommandApplication

// @main(name = "scala-boot/repo-indexer")
// case class Config(
//     @arg(doc = "Github org to find teampltes in")
//     org: String,
//     @arg(short = 'v', doc = "Enable verbose (really verbose logging)")
//     verbose: Flag,
//     @arg(doc = "Address of Scala Boot service")
//     api: Option[String] = None,

// )
