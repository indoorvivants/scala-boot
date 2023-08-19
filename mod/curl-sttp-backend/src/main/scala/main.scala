package scalaboot.curl

import concurrent.duration.*

@main def hello =
  import sttp.client3.*

  val backend = scalaboot.curl.CurlBackend(verbose = true)
  val response = basicRequest
    .body("Hello, world!")
    .post(uri"https://httpbun.org/post")
    .readTimeout(2.second)
    .send(backend)

  println(response.body)
end hello
