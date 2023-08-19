import curl.all.*
import curl.enumerations.CURLoption.*

import scala.collection.mutable.ArrayBuilder
import scala.scalanative.libc.stdio
import scala.scalanative.libc.string

import scalanative.unsafe.*
import curl.enumerations.CURLINFO.CURLINFO_RESPONSE_CODE
import mainargs.ParserForClass
import scribe.Level
import scala.util.Using
import sttp.client3.SttpBackend
import sttp.model.Uri

inline def zone[A](inline f: Zone ?=> A) = Zone { z => f(using z) }

@main def repoIndexer(args: String*) =
  val config: Config =
    ParserForClass[Config].constructEither(
      args,
      allowPositional = true,
      sorted = false
    ) match
      case Left(msg) =>
        System.err.println(msg)
        sys.exit(1)
      case Right(value) =>
        value.asInstanceOf[Config]

  init(config)
end repoIndexer

def init(config: Config) =
  zone {
    val client = scalaboot.curl.CurlBackend()
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
    val token = Token(sys.env("SCALABOOT_GITHUB_TOKEN").trim)
    val github = GithubApi(client, token)

    github.templateRepos(config.org).foreach { repo =>
      val sha = github.latestCommit(repo)
      val readmeContents = github.readFile(repo, sha, "README.md")
      scribe.info(readmeContents)
    }

  }

class Token(val value: String):
  override def toString(): String = "Token[redacted]"

class GithubApi(client: SttpBackend[sttp.client3.Identity, Any], token: Token):
  val github = Map(
    "Accept" -> "application/vnd.github+json",
    s"Authorization" -> s"Bearer ${token.value}",
    "User-agent" -> "Scala Boot Repo Indexer"
  )
  import sttp.client3.*
  import sttp.client3.upicklejson.*
  def templateRepos(org: String)(using Zone) =
    val reposList = basicRequest
      .get(
        Uri.unsafeParse(s"https://api.github.com/orgs/${org}/repos?type=public")
      )
      .headers(github)
      .response(asJson[ujson.Value])
      .send(client)
      .body
      .right
      .get
      .arr
      .map(_.obj("full_name").str)

    val templateRepos = reposList.filter(_.endsWith(".g8"))

    scribe.info(
      "Discovered the following g8 repos: " + templateRepos
        .map(fansi.Color.Green(_))
        .mkString(", ")
    )
    templateRepos
  end templateRepos

  def latestCommit(repo: String)(using Zone) =
    basicRequest
      .get(
        Uri.unsafeParse(
          s"https://api.github.com/repos/$repo/commits?per_page=1"
        )
      )
      .headers(github)
      .response(asJson[ujson.Value])
      .send(client)
      .body
      .right
      .get
      .arr
      .map(_.obj("sha").str)
      .head

  def readFile(repo: String, sha: String, file: String)(using Zone) =
    val url = s"https://raw.githubusercontent.com/$repo/$sha/$file"

    basicRequest
      .get(Uri.unsafeParse(url))
      .headers(github)
      .response(asString)
      .send(client)
      .body
      .right
      .get
  end readFile

end GithubApi
