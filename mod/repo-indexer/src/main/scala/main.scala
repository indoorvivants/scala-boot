import curl.all.*
import curl.enumerations.CURLoption.CURLOPT_URL
import scalanative.unsafe.*
import curl.enumerations.CURLoption.CURLOPT_WRITEFUNCTION
import scala.scalanative.libc.string
import scala.scalanative.libc.stdio
import curl.enumerations.CURLoption.CURLOPT_WRITEDATA
import scala.collection.mutable.ArrayBuilder
import curl.enumerations.CURLoption.CURLOPT_HTTPHEADER

inline def zone[A](inline f: Zone ?=> A) = Zone { z => f(using z) }

@main def hello =
  zone {
    val client = CurlClient()
    println(
      client.get(
        "https://httpbun.org/get",
        response = client.ResponseHandler.ToString,
        headers = Map("Accept" -> "application/vnd.github+json")
      )
    )
  }

class CurlClient private (curl: Ptr[CURL]):
  def get[T](
      url: String,
      headers: Map[String, String] = Map.empty,
      response: ResponseHandler[T]
  )(using
      Zone
  ): T =
    curl_easy_setopt(curl, CURLOPT_URL, toCString(url))
    val (before, after) = setup(response)
    val teardownHeaders = setHeaders(headers)
    val interm = before(curl)
    val res = curl_easy_perform(curl)
    val result = after(interm)
    teardownHeaders()
    assert(res == CURLcode.CURLE_OK, "Expected request to succeed")
    curl_easy_cleanup(curl)
    result
  end get

  private def makeHeaders(hd: Map[String, String])(using Zone) =
    var slist: Ptr[curl_slist] = null
    hd.foreach { case (k, v) =>
      slist = curl_slist_append(slist, toCString(s"$k:$v"))
    }
    slist

  private def setHeaders(hd: Map[String, String])(using Zone) =
    val slist = makeHeaders(hd)
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, slist)
    () => curl_slist_free_all(slist)

  private type Setup[T] = Ptr[CURL] => Any
  private type Teardown[T] = Any => T

  private def setup[T](handler: ResponseHandler[T])(using Zone) =
    val before: Setup[T] = curl =>
      handler match
        case ResponseHandler.ToString =>
          val builder = Array.newBuilder[Byte]
          val (ptr, memory) = Captured.unsafe(builder)

          val write_data_callback = CFuncPtr4.fromScalaFunction {
            (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) =>
              val vec = !userdata.asInstanceOf[Ptr[ArrayBuilder[Byte]]]
              for i <- 0 until nmemb.toInt do vec.addOne(ptr(i))
              nmemb * size
          }

          check(
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data_callback)
          )
          check(curl_easy_setopt(curl, CURLOPT_WRITEDATA, ptr))

          (builder, memory)

        case ResponseHandler.Void =>
          ()

    val after: Teardown[T] = any =>
      handler match
        case ResponseHandler.ToString =>
          val (b, mem) = any.asInstanceOf[(ArrayBuilder[Byte], Memory)]
          val res = new String(b.result())
          mem.deallocate()

          res

        case ResponseHandler.Void => ()

    before -> after

  end setup

  export CurlClient.ResponseHandler
end CurlClient

object CurlClient:
  def apply(): CurlClient =
    val curl = curl_easy_init()
    assert(curl != null, "Expected curl init to succeed")
    new CurlClient(curl)

  enum ResponseHandler[T]:
    case ToString extends ResponseHandler[String]
    case Void extends ResponseHandler[Unit]

end CurlClient
