package scalaboot.protocol

import sttp.tapir.*
import sttp.tapir.json.upickle.*
import sttp.tapir.generic.auto.*

case class Metadata() derives upickle.default.ReadWriter

case class RepositoryInfo(
    name: String,
    last_commit: String,
    readme_markdown: String,
    metadata: Metadata,
    headline: Option[String] = None,
    summary: Option[String] = None
) derives upickle.default.ReadWriter

case class SearchResult(repo: RepositoryInfo, rank: Float)
    derives upickle.default.ReadWriter

object repos:

  val all = endpoint
    .in("repos" / "all")
    .out(jsonBody[List[RepositoryInfo]])

  val search = endpoint
    .in("repos" / "search")
    .in(query[String]("query"))
    .out(jsonBody[List[SearchResult]])

  val add =
    endpoint.post.in("repos" / "add").in(jsonBody[RepositoryInfo])
end repos
