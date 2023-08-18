import scalanative.unsafe.*
import curl.all.*
import CURLoption.*
import CURLINFO.*
import scala.collection.mutable.ArrayBuilder
import scala.scalanative.libc.string
import scala.util.Using.Releasable

enum Result[T]:
  case Ok(body: T)
  case Failed(code: Long, body: T)

  def getOrThrow() =
    this match
      case Ok(body) => body
      case Failed(code, body) =>
        sys.error(s"Request failed with code [$code]: [$body]")
end Result

import CurlClient.ResponseHandler

trait CurlClient:
  def get[T](
      url: String,
      headers: Map[String, String] = Map.empty,
      response: ResponseHandler[T] = ResponseHandler.Void,
      codeCheck: Long => Boolean = code => code >= 200 && code <= 300
  )(using
      Zone
  ): Result[T]
end CurlClient

class CurlClientImpl(curl: Ptr[CURL]) extends CurlClient:
  private var dead = false
  def get[T](
      url: String,
      headers: Map[String, String] = Map.empty,
      response: ResponseHandler[T] = ResponseHandler.Void,
      codeCheck: Long => Boolean = code => code >= 200 && code <= 300
  )(using
      Zone
  ): Result[T] =
    assert(!dead, "This client was shut down!")
    curl_easy_setopt(curl, CURLOPT_URL, toCString(url))
    val (before, after) = setup(response)
    val teardownHeaders = setHeaders(headers)
    val interm = before(curl)
    val res = curl_easy_perform(curl)
    val result = after(interm)
    teardownHeaders()
    assert(
      res == CURLcode.CURLE_OK,
      "Expected request to succeed: " + fromCString(curl_easy_strerror(res))
    )

    val code = stackalloc[Long]()
    check(curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, code))

    curl_easy_reset(curl)

    if !codeCheck(!code) then Result.Failed(!code, result)
    else Result.Ok(result)
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

  private def setup[T](handler: ResponseHandler[T])(using
      Zone
  ): (Setup[T], Teardown[T]) =
    handler match
      case ResponseHandler.ToString =>
        val before: Setup[T] = curl =>
          val builder = Array.newBuilder[Byte]
          val (ptr, memory) = Captured.unsafe(builder)

          val write_data_callback = CFuncPtr4.fromScalaFunction {
            (ptr: Ptr[Byte], size: CSize, nmemb: CSize, userdata: Ptr[Byte]) =>
              val vec = !userdata.asInstanceOf[Ptr[ArrayBuilder[Byte]]]

              val newArr = new Array[Byte](nmemb.toInt)

              string.memcpy(newArr.at(0), ptr, nmemb)

              vec.addAll(newArr)

              nmemb * size
          }

          check(
            curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data_callback)
          )
          check(curl_easy_setopt(curl, CURLOPT_WRITEDATA, ptr))

          (builder, memory)

        val after: Teardown[T] = any =>
          val (b, mem) = any.asInstanceOf[(ArrayBuilder[Byte], Memory)]
          val res = new String(b.result())
          mem.deallocate()

          res

        before -> after

      case ResponseHandler.ToJson =>
        val (b, a) = setup(ResponseHandler.ToString)
        val set: Setup[ujson.Value] = c => b(c)
        val tear: Teardown[ujson.Value] = v => ujson.read(a(v))

        set -> tear

      case ResponseHandler.Void =>
        val void = [a] => (_: a) => ()
        val before: Setup[T] = _ => ()
        val tear: Teardown[T] = _ => ()

        before -> tear
    end match
  end setup

  def _cleanup() = if !dead then
    curl_easy_cleanup(curl)
    dead = true
  else sys.error("Double clean up on an already shut down client!")

  export CurlClient.ResponseHandler
end CurlClientImpl

object CurlClient:
  def apply(): CurlClient =
    val curl = curl_easy_init()
    assert(curl != null, "Expected curl init to succeed")
    new CurlClientImpl(curl)

  given Releasable[CurlClient] with
    def release(resource: CurlClient): Unit = resource match
      case c: CurlClientImpl => c._cleanup()

  enum ResponseHandler[T]:
    case ToString extends ResponseHandler[String]
    case ToJson extends ResponseHandler[ujson.Value]
    case Void extends ResponseHandler[Unit]

end CurlClient
