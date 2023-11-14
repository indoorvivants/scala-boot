package scalaboot.client

import sttp.tapir.client.sttp.SttpClientInterpreter
import scalaboot.protocol.*
import scalaboot.curl.CurlBackend
import sttp.model.Uri
import sttp.client3.SttpBackend
import scala.util.control.NonFatal

trait Client:
  def search(query: String): List[SearchResult]

  def create(repo: RepositoryInfo): Unit

  def update(repo: UpdateRepository): Unit

  def delete(id: Int): Unit

  def all(): List[SavedRepository]

end Client

object Client:
  def create(uri: String, token: Option[String] = None): Client =
    new ClientImpl(
      backend = CurlBackend(),
      interp = SttpClientInterpreter(),
      base = Uri.unsafeParse(uri),
      token = token
    )

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

      override def update(repo: UpdateRepository): Unit =
        wrap(repos.update.showShort, self.update(repo))

      override def all(): List[SavedRepository] =
        wrap(repos.all.showShort, self.all())

      override def delete(id: Int): Unit =
        wrap(repos.delete.showShort, self.delete(id))

  private class ClientImpl(
      backend: SttpBackend[sttp.client3.Identity, Any],
      interp: SttpClientInterpreter,
      base: Uri,
      token: Option[String] = None
  ) extends Client:
    def search(query: String): List[SearchResult] =
      interp
        .toClientThrowErrors(repos.search, Some(base), backend)
        .apply(query)

    def create(repo: RepositoryInfo): Unit =
      interp
        .toSecureClientThrowErrors(repos.add, Some(base), backend)
        .apply(token)
        .apply(repo)

    def update(repo: UpdateRepository): Unit =
      interp
        .toSecureClientThrowErrors(repos.update, Some(base), backend)
        .apply(token)
        .apply(repo)

    def all(): List[SavedRepository] =
      interp.toClientThrowErrors(repos.all, Some(base), backend).apply(())

    def delete(id: Int): Unit =
      interp
        .toSecureClientThrowErrors(repos.delete, Some(base), backend)
        .apply(token)
        .apply(DeleteRepository(id))
  end ClientImpl
end Client
