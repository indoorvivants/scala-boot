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
import scalaboot.client.Client
import scalaboot.protocol.RepositoryInfo
import scalaboot.protocol.Metadata
import scala.util.control.NonFatal

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

case class GithubRepo(slug: String, lastCommit: String, markdown: String)

def init(config: Config) =
  zone {
    val backend = scalaboot.curl.CurlBackend(verbose = true)
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
    val token = Token(sys.env("SCALABOOT_GITHUB_TOKEN").trim)
    val github = GithubApi(backend, token)
    val apiClient = Client.create(config.api)

    val discoveredRepos = github
      .templateRepos(config.org)
      .map { repo =>
        val sha = github.latestCommit(repo)
        val readmeContents = github.readFile(repo, sha, "README.md")

        GithubRepo(repo, sha, readmeContents)
      }
      .toList

    printRepos("Discovered template repos", discoveredRepos.map(_.slug))

    val onServer = apiClient.all()

    // pre-calculate to make filtering faster
    val discoveredReposByName = discoveredRepos.map(r => r.slug -> r).toMap
    val discoveredReposNames = discoveredReposByName.keySet
    val onServerByName = onServer.map(s => s.name -> s).toMap
    val onServerReposNames = onServerByName.keySet

    val missingOnServer =
      discoveredRepos.filter(repo => !onServerReposNames(repo.slug))

    printRepos("Missing on server", missingOnServer.map(_.slug))

    val deletedOnGithub =
      onServer.filter(repo => !discoveredReposNames(repo.name))

    printRepos("Deleted on Github", deletedOnGithub.map(_.name))

    val commitMismatch =
      onServer

    val repoErrors = List.newBuilder[(String, Throwable)]

    missingOnServer.foreach { missingRepo =>
      try
        val repoInfo = RepositoryInfo(
          name = missingRepo.slug,
          last_commit = missingRepo.lastCommit,
          readme_markdown = missingRepo.markdown,
          metadata = Metadata()
          // TODO: extract headline and summary using cmark
        )

        apiClient.create(repo = repoInfo)
        scribe.info(s"✅ Created ${missingRepo.slug}")
      catch
        case NonFatal(exc) =>
          // repoErrors += (missingRepo.slug -> exc)
          scribe.error(
            s"❌ Failed to process ${missingRepo.slug}",
            exc
          )
    }

  }

def printRepos(msg: String, repos: Seq[String]) =
  scribe.info(
    msg + ": " + repos
      .map(fansi.Color.Green(_))
      .mkString(", ")
  )

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
