package scalaboot.client

import sttp.tapir.client.sttp.SttpClientInterpreter
import scalaboot.protocol.*
import scalaboot.curl.CurlBackend
import scalaboot.curl.AbstractCurlBackend
import sttp.model.Uri
import sttp.client3.SttpBackend

class Client(
    backend: SttpBackend[sttp.client3.Identity, Any],
    interp: SttpClientInterpreter,
    base: Uri
):
  def search(query: String): List[SearchResult] =
    interp.toClientThrowErrors(repos.search, Some(base), backend).apply(query)

  def create(repo: RepositoryInfo): Unit =
    interp.toClientThrowErrors(repos.add, Some(base), backend).apply(repo)

  def all(): List[RepositoryInfo] =
    interp.toClientThrowErrors(repos.all, Some(base), backend).apply(())
end Client

object Client:
  def create(uri: String): Client =
    new Client(CurlBackend(), SttpClientInterpreter(), Uri.unsafeParse(uri))

@main def hello =
  println(Client.create("http://localhost:8080").search("howdy"))
