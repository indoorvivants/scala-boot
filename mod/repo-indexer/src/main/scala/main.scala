package scalaboot.repo_indexer

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

case class GithubRepoSnapshot(repo: GithubRepo, revision: RepoRevision)

case class GithubRepo(
    slug: String,
    stars: Int
)

case class RepoRevision(
    lastCommit: String,
    markdown: String
)

def init(config: Config) =
  zone {
    val backend = scalaboot.curl.CurlBackend()
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
    val token = Token(sys.env("SCALABOOT_GITHUB_TOKEN").trim)
    val github = GithubApi(backend, token)
    val apiClient = Client.create(config.api)

    val discoveredRepos = github
      .templateRepos(config.org)
      .map { repo =>
        val sha = github.latestCommit(repo.slug)
        val readmeContents = github.readFile(repo.slug, sha, "README.md")

        GithubRepoSnapshot(repo, RepoRevision(lastCommit = sha, readmeContents))
      }
      .toList

    printRepos("Discovered template repos", discoveredRepos.map(_.repo.slug))

    val onServer = apiClient.all()

    // pre-calculate to make filtering faster
    val discoveredReposByName = discoveredRepos.map(r => r.repo.slug -> r).toMap
    val discoveredReposNames = discoveredReposByName.keySet
    val onServerByName = onServer.map(s => s.info.name -> s).toMap
    val onServerReposNames = onServerByName.keySet

    val missingOnServer =
      discoveredRepos.filter(repo => !onServerReposNames(repo.repo.slug))

    printRepos("Missing on server", missingOnServer.map(_.repo.slug))

    val deletedOnGithub =
      onServer.filter(repo =>
        !discoveredReposNames(repo.info.name) &&
          repo.info.name.startsWith(
            config.org + "/"
          )
      )

    printRepos("Deleted on Github", deletedOnGithub.map(_.info.name))

    val commitMismatch =
      onServer

    val repoErrors = List.newBuilder[(String, Throwable)]

    missingOnServer.foreach { case GithubRepoSnapshot(missingRepo, revision) =>
      try
        val repoInfo = RepositoryInfo(
          name = missingRepo.slug,
          last_commit = revision.lastCommit,
          readme_markdown = revision.markdown,
          metadata = Metadata(),
          stars = missingRepo.stars
          // TODO: extract headline and summary using cmark
        )

        apiClient.create(repo = repoInfo)
        scribe.info(s"✅ Created ${missingRepo.slug}")
      catch
        case NonFatal(exc) =>
          scribe.error(
            s"❌ Failed to process ${missingRepo.slug}, exception will be printed at the end of the run"
          )
          repoErrors += (missingRepo.slug -> exc)
    }

    repoErrors.result().foreach { case (repo, exc) =>
      scribe.error(
        s"❌ Failed to process $repo",
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
    def go(url: String, result: List[GithubRepo]): List[GithubRepo] =
      scribe.debug(s"Fetching github repos from $url")
      val response = basicRequest
        .get(Uri.unsafeParse(url))
        .headers(github)
        .response(asJson[ujson.Value])
        .send(client)

      val nextPageUrl =
        response.headers
          .find(_.name == "Link")
          .map(_.value)
          .map(extractLinks(_))
          .flatMap(_.get("next"))

      val reposList = response.body.right.get.arr
        .map(_.obj)
        .map(r =>
          GithubRepo(
            slug = r("full_name").str,
            stars = r("stargazers_count").num.toInt
          )
        )

      val next = result ++ reposList

      nextPageUrl match
        case None        => next
        case Some(value) => go(value, next)

    end go

    val startUrl =
      s"https://api.github.com/orgs/${org}/repos?type=public&per_page=100"

    val reposList = go(startUrl, Nil)
    val templateRepos = reposList.filter(_.slug.endsWith(".g8"))

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

  private def extractLinks(linkHeader: String): Map[String, String] =
    val singleLink = "<([^>]+)>; rel=\"(.*?)\"".r
    val all = singleLink.findAllMatchIn(linkHeader)
    all.map { m =>
      m.group(2) -> m.group(1)
    }.toMap

end GithubApi
