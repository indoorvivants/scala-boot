package scalaboot.repo_indexer

import mainargs.ParserForClass
import scalaboot.client.Client
import scalaboot.client.Retries
import scalaboot.protocol
import scribe.Level
import sttp.model.Uri

import scala.concurrent.duration.*
import scala.util.control.NonFatal

import protocol.*
import scalanative.unsafe.*
import sttp.client4.curl.*
import sttp.client4.*
import decline_derive.CommandApplication

@main def repoIndexer(args: String*) =
  init(CommandApplication.parseOrExit[Config](args))
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
  Zone:
    if config.verbose.value then
      scribe.Logger.root.withMinimumLevel(Level.Debug).replace()

    val backend = CurlBackend()
    val apiKey = sys.env.get("SCALABOOT_API_KEY")
    if apiKey.isEmpty then
      scribe.warn(
        "SCALABOOT_API_KEY is not set, requests will run unauthorized"
      )
    val retries = Retries.exponential(5, 30.millis)
    val baseClient =
      Client.create(config.api.getOrElse(protocol.SCALABOOT_PRODUCTION), apiKey)
    val client = Client.stabilise(
      baseClient,
      retries,
      (label, attempt) =>
        scribe.debug(
          s"[$label] failed, retrying in ${attempt.action.sleep}ms (${attempt.action.remaining} more attempts left)"
        )
    )

    val token = sys.env.get("SCALABOOT_GITHUB_TOKEN").map(Token(_))
    val github = GithubApi(backend, token)

    val discoveredRepos = github
      .templateRepos(config.org)
      .map { repo =>
        val sha = github.latestCommit(repo.slug)
        val readmeContents =
          github.readFile(repo.slug, sha, "README.md").getOrElse("")

        GithubRepoSnapshot(repo, RepoRevision(lastCommit = sha, readmeContents))
      }
      .toList

    printRepos("Discovered template repos", discoveredRepos.map(_.repo.slug))

    val onServer = client.all()

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

    val infoMismatch =
      discoveredReposByName.flatMap { case (name, snap) =>
        onServerByName.get(name).flatMap { saved =>
          val update =
            UpdateRepository(
              id = saved.id,
              last_commit = Option(snap.revision.lastCommit)
                .filter(_ != saved.info.last_commit),
              stars = Option(snap.repo.stars).filter(_ != saved.info.stars),
              readme_markdown = Option(snap.revision.markdown)
                .filter(_ != saved.info.readme_markdown)
            )

          val needUpdating = update != UpdateRepository(id = saved.id)

          Option.when(needUpdating)(name -> update)
        }
      }.toList

    printRepos("Need updating on server", infoMismatch.map(_._1))

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

        client.create(repo = repoInfo)
        scribe.info(s"✅ Created ${missingRepo.slug}")
      catch
        case NonFatal(exc) =>
          scribe.error(
            s"❌ Failed to create ${missingRepo.slug}, exception will be printed at the end of the run"
          )
          repoErrors += (missingRepo.slug -> exc)
    }

    deletedOnGithub.foreach { repo =>
      try
        client.delete(repo.id)
        scribe.info(s"✅ Deleted ${repo.info.name}")
      catch
        case NonFatal(exc) =>
          scribe.error(
            s"❌ Failed to delete ${repo.info.name}, exception will be printed at the end of the run"
          )
          repoErrors += (repo.info.name -> exc)
    }

    infoMismatch.foreach { case (name, update) =>
      try
        client.update(update)
        scribe.info(s"✅ Updated ${name}")
      catch
        case NonFatal(exc) =>
          scribe.error(
            s"❌ Failed to update ${name}, exception will be printed at the end of the run"
          )
          repoErrors += (name -> exc)
    }

    repoErrors.result().foreach { case (repo, exc) =>
      scribe.error(
        s"❌ Failed to process $repo",
        exc
      )
    }

def printRepos(msg: String, repos: Seq[String]) =
  scribe.info(
    msg + ": " + repos
      .map(fansi.Color.Green(_))
      .mkString(", ")
  )

class Token(val value: String):
  override def toString(): String = "Token[redacted]"

class GithubApi(
    client: SyncBackend,
    token: Option[Token]
):
  val tokHeader = token.map { token =>
    s"Authorization" -> s"Bearer ${token.value}"
  }.toMap

  val github = Map(
    "Accept" -> "application/vnd.github+json",
    "User-agent" -> "Scala Boot Repo Indexer"
  ) ++ tokHeader

  import sttp.client4.*
  import sttp.client4.circe.*

  case class GithubRepoAPI(full_name: String, stargazers_count: Int)
      derives io.circe.Codec.AsObject

  def templateRepos(org: String)(using Zone) =
    def go(url: String, result: List[GithubRepo]): List[GithubRepo] =
      scribe.debug(s"Fetching github repos from $url")
      val response = basicRequest
        .get(Uri.unsafeParse(url))
        .headers(github)
        .response(asJson[List[GithubRepoAPI]])
        .send(client)

      val nextPageUrl =
        response.headers
          .find(_.name == "Link")
          .map(_.value)
          .map(extractLinks(_))
          .flatMap(_.get("next"))

      val reposList = response.body
        .fold(throw _, identity)
        .map(r =>
          GithubRepo(
            slug = r.full_name,
            stars = r.stargazers_count
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

  case class HasSha(sha: String) derives io.circe.Codec.AsObject

  def latestCommit(repo: String)(using Zone) =
    basicRequest
      .get(
        Uri.unsafeParse(
          s"https://api.github.com/repos/$repo/commits?per_page=1"
        )
      )
      .headers(github)
      .response(asJson[List[HasSha]])
      .send(client)
      .body
      .fold(throw _, identity)
      .map(_.sha)
      .head

  def readFile(repo: String, sha: String, file: String)(using
      Zone
  ): Option[String] =
    val url = s"https://raw.githubusercontent.com/$repo/$sha/$file"

    val response = basicRequest
      .get(Uri.unsafeParse(url))
      .headers(github)
      .response(asString)
      .send(client)

    if response.code.code == 404 then None
    else response.body.fold(sys.error(_), Option.apply)
  end readFile

  private def extractLinks(linkHeader: String): Map[String, String] =
    val singleLink = "<([^>]+)>; rel=\"(.*?)\"".r
    val all = singleLink.findAllMatchIn(linkHeader)
    all.map { m =>
      m.group(2) -> m.group(1)
    }.toMap

end GithubApi
