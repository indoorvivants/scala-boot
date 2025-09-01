package scalaboot.protocol

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.Codec as CirceCodec

private[scalaboot] inline def SCALABOOT_PRODUCTION =
  "https://scala-boot.fly.dev"

case class Metadata() derives CirceCodec.AsObject

case class RepositoryInfo(
    name: String,
    last_commit: String,
    readme_markdown: String,
    metadata: Metadata,
    headline: Option[String] = None,
    summary: Option[String] = None,
    stars: Int
) derives CirceCodec.AsObject
case class SearchResult(repo: RepositorySummary, rank: Float) derives CirceCodec.AsObject
case class SavedRepository(id: Int, info: RepositoryInfo) derives CirceCodec.AsObject
case class DeleteRepository(id: Int) derives CirceCodec.AsObject
case class RepositorySummary(
    name: String,
    headline: Option[String],
    summary: Option[String],
    stars: Int
) derives CirceCodec.AsObject

case class UpdateRepository(
    id: Int,
    last_commit: Option[String] = None,
    readme_markdown: Option[String] = None,
    headline: Option[String] = None,
    summary: Option[String] = None,
    stars: Option[Int] = None
) derives CirceCodec.AsObject

object repos:

  val all = endpoint
    .in("repos" / "all")
    .out(jsonBody[List[SavedRepository]])
    .errorOut(stringBody)

  val search = endpoint
    .in("repos" / "search")
    .in(query[String]("query"))
    .out(jsonBody[List[SearchResult]])
    .errorOut(stringBody)

  val add =
    endpoint.post
      .in("repos" / "add")
      .in(jsonBody[RepositoryInfo])
      .securityIn(auth.bearer[Option[String]]())
      .errorOut(stringBody)

  val delete =
    endpoint.delete
      .in("repos" / "delete")
      .in(jsonBody[DeleteRepository])
      .securityIn(auth.bearer[Option[String]]())
      .errorOut(stringBody)

  val update =
    endpoint.post
      .in("repos" / "update")
      .in(jsonBody[UpdateRepository])
      .securityIn(auth.bearer[Option[String]]())
      .errorOut(stringBody)

end repos
