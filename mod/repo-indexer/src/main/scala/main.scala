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
    scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
    Using(CurlClient()) { client =>
      val token = sys.env("SCALABOOT_GITHUB_TOKEN").trim
      val github = Map(
        "Accept" -> "application/vnd.github+json",
        s"Authorization" -> s"Bearer $token",
        "User-agent" -> "Scala Boot Repo Indexer"
      )
      val reposList = client
        .get(
          s"https://api.github.com/orgs/${config.org}/repos?type=public",
          response = client.ResponseHandler.ToJson,
          headers = github
        )
        .getOrThrow()
        .arr
        .map(_.obj("full_name").str)
        .toVector

      val templateRepos = reposList.filter(_.endsWith(".g8")).take(2)

      scribe.info(
        "Discovered the following g8 repos: " + templateRepos
          .map(fansi.Color.Green(_))
          .mkString(", ")
      )

      templateRepos.foreach { repo =>
        scribe.info(s"Working on $repo...")
        val commits = client
          .get(
            s"https://api.github.com/repos/$repo/commits?per_page=1",
            response = client.ResponseHandler.ToJson,
            headers = github
          )
          .getOrThrow()
          .arr
          .map(_.obj)
          .map { o =>
            val sha = o("sha").str
            scribe.info(s"Latest commit is $sha")
            val readmeUrl =
              s"https://raw.githubusercontent.com/$repo/$sha/README.md"
            val contents = client
              .get(
                readmeUrl,
                headers = github,
                response = client.ResponseHandler.ToString
              )
              .getOrThrow()
            scribe.info(s"README contents: $contents")
          }

      }

    }
  }
