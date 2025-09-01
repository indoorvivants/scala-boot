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
