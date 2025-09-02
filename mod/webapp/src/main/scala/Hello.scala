package scalaboot.webapp

import com.raquo.laminar.api.L.*
import sttp.client4.fetch.FetchBackend
import sttp.tapir.client.sttp4.SttpClientInterpreter
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

@main def hello =
  val query =
    WebStorageVar
      .localStorage("scala-boot-query", Some(unsafeWindowOwner))
      .text("scala3")

  renderOnDomContentLoaded(
    org.scalajs.dom.document.getElementById("app"),
    div(
      input(
        idAttr := "search-input",
        placeholder := "Search... scala3 for example",
        onInput.mapToValue --> query,
        value <-- query.signal
      ),
      children <-- query.signal
        .composeChanges(_.debounce(300).filter(_.nonEmpty))
        .flatMapSwitch { query =>
          EventStream
            .fromFuture(Client.search(query))
            .map(_.map(SearchResultCard(_)))
            .startWith(Nil)
        }
    )
  )
end hello

import scalaboot.protocol.*
import com.raquo.airstream.web.WebStorageVar

def SearchResultCard(result: SearchResult) =
  val command = "scala-boot go " + result.repo.name
  div(
    cls := "search-card",
    div(
      cls := "search-card-header",
      h3(
        a(
          result.repo.name,
          href := s"https://github.com/${result.repo.name}",
          target := "_blank"
        ),
        cls := "repo-title"
      ),
      span(s"⭐️ ${result.repo.stars}", cls := "repo-stars")
    ),
    pre(
      cls := "repo-go-command",
      a(
        "📋 ",
        onClick.preventDefault.mapTo(value) --> { _ =>
          org.scalajs.dom.window.navigator.clipboard.writeText(command)
        },
        href := "#"
      ),
      code(command)
    )
  )
end SearchResultCard

object Client:
  val backend = FetchBackend()
  val interp = SttpClientInterpreter()

  import scalaboot.protocol.*
  def search(query: String): Future[List[SearchResult]] =
    interp
      .toClientThrowErrors(repos.search, None, backend)
      .apply(query)
