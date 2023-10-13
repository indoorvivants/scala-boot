package scalaboot.client

import sttp.tapir.client.sttp.SttpClientInterpreter
import scalaboot.protocol.*
import scalaboot.curl.CurlBackend
import sttp.model.Uri
import sttp.client3.SttpBackend
import scala.util.control.NonFatal

trait Client:
  self =>
  def search(query: String): List[SearchResult]

  def create(repo: RepositoryInfo): Unit

  def all(): List[SavedRepository]

end Client

object Client:
  def create(uri: String): Client =
    new ClientImpl(CurlBackend(), SttpClientInterpreter(), Uri.unsafeParse(uri))

  def stabilise(
      self: Client,
      policy: Retries,
      logger: (String, Attempt) => Unit
  ): Client =
    new Client:
      val errorHandler: PartialFunction[Throwable, Boolean] = {
        case NonFatal(exc) => true
      }

      def wrap[A](label: String, a: => A) =
        retryable(
          a,
          errors = errorHandler,
          logger = logger(label, _),
          policy = policy
        )

      override def search(query: String): List[SearchResult] =
        wrap(repos.search.showShort, self.search(query))

      override def create(repo: RepositoryInfo): Unit =
        wrap(repos.add.showShort, self.create(repo))

      override def all(): List[SavedRepository] =
        wrap(repos.all.showShort, self.all())

  private class ClientImpl(
      backend: SttpBackend[sttp.client3.Identity, Any],
      interp: SttpClientInterpreter,
      base: Uri
  ) extends Client:
    def search(query: String): List[SearchResult] =
      interp.toClientThrowErrors(repos.search, Some(base), backend).apply(query)

    def create(repo: RepositoryInfo): Unit =
      interp.toClientThrowErrors(repos.add, Some(base), backend).apply(repo)

    def all(): List[SavedRepository] =
      interp.toClientThrowErrors(repos.all, Some(base), backend).apply(())
  end ClientImpl
end Client
