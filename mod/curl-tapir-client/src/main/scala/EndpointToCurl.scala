package scalaboot.tapir

import curl.all.*

import scalanative.unsafe.*

import sttp.tapir.{DecodeResult, Endpoint, PublicEndpoint}

class CurlTapirInterpreter private (
    private var valid: Boolean,
    curlHandle: Ptr[CURL]
):
  def toRequest[I, E, O, R](e: PublicEndpoint[I, E, O, R], baseUri: String): I => (StandaloneWSRequest, StandaloneWSResponse => DecodeResult[Either[E, O]]) =
    new EndpointToCurl(curlHandle).toRequest(e, baseUri).apply(())


class EndpointToCurl(curlHandle: Ptr[CURL]):
  def toRequest[A, I, E, O, R](
      e: Endpoint[A, I, E, O, R],
      baseUri: String
  ): A => I => (StandaloneWSRequest, StandaloneWSResponse => DecodeResult[Either[E, O]]) = { aParams => iParams =>
    val req0 = setInputParams(e.securityInput, ParamsAsAny(aParams), ws.url(baseUri))
    val req = setInputParams(e.input, ParamsAsAny(iParams), req0)
      .withMethod(e.method.getOrElse(Method.GET).method)

    def responseParser(response: StandaloneWSResponse): DecodeResult[Either[E, O]] = {
      parsePlayResponse(e)(response) match {
        case DecodeResult.Error(o, e) =>
          DecodeResult.Error(o, new IllegalArgumentException(s"Cannot decode from: $o, request ${req.method} ${req.uri}", e))
        case other => other
      }
    }

    (req, responseParser)
  }
  
