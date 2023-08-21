package scalaboot.protocol

import sttp.tapir.*
import sttp.tapir.json.upickle.*
import sttp.tapir.generic.auto.*

import upickle.default.ReadWriter as JSON

private[scalaboot] inline def SCALABOOT_PRODUCTION = "https://scala-boot.fly.dev"

case class Metadata() derives JSON

case class RepositoryInfo(
    name: String,
    last_commit: String,
    readme_markdown: String,
    metadata: Metadata,
    headline: Option[String] = None,
    summary: Option[String] = None,
    stars: Int
) derives JSON
case class SearchResult(repo: RepositorySummary, rank: Float) derives JSON
case class SavedRepository(id: Int, info: RepositoryInfo) derives JSON
case class DeleteRepository(id: Int) derives JSON
case class RepositorySummary(
    name: String,
    headline: Option[String],
    summary: Option[String],
    stars: Int
) derives JSON

object repos:

  val all = endpoint
    .in("repos" / "all")
    .out(jsonBody[List[SavedRepository]])

  val search = endpoint
    .in("repos" / "search")
    .in(query[String]("query"))
    .out(jsonBody[List[SearchResult]])

  val add =
    endpoint.post.in("repos" / "add").in(jsonBody[RepositoryInfo])

  val delete =
    endpoint.delete.in("repos" / "delete").in(jsonBody[DeleteRepository])

end repos
