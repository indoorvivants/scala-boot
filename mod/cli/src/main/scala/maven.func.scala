package scalaboot

import scalaboot.template.*
import mxml.enumerations.mxml_descend_e.MXML_DESCEND_ALL

object MavenFunc:

  import mxml.all.*
  import scalanative.unsafe.*
  import scala.util.Try

  import sttp.client4.*
  val curl = sttp.client4.curl.CurlBackend()

  val func2 = Func
    .builder("maven")
    .withArg(Param.Str)
    .withArg(Param.Str)
    .withApply:
      case (org, artifact) =>
        request(org, artifact, stable = false)

  val func3 = Func
    .builder("maven")
    .withArg(Param.Str)
    .withArg(Param.Str)
    .withArg(Param.Str)
    .withApply:
      case (org, artifact, st) =>
        request(org, artifact, stable = st == "stable")

  val all = Seq(func2, func3)

  def request(org: String, artifact: String, stable: Boolean): Try[String] =
    Try:
      basicRequest
        .get(
          uri"https://repo1.maven.org/maven2/${org.replace('.', '/')}/$artifact/maven-metadata.xml"
        )
        .response(asStringOrFail)
        .send(curl)
        .body
    .flatMap: xml =>
      readXml(stable, xml)

  end request

  def readXml(stable: Boolean, xml: String) =
    Try:
      Zone:
        val tree = mxmlLoadString(null, null, toCString(xml))
        assert(tree != null, "Parsing metadata failed")
        if !stable then
          val value = mxmlFindPath(tree, c"metadata/versioning/latest")
          assert(
            value != null,
            "Grabbing metadata/versioning/latest node failed"
          )
          val str = mxmlGetText(value, null)
          assert(
            str != null,
            "Getting text from the <latest> node returned null"
          )
          fromCString(str)
        else
          val versions = mxmlFindPath(tree, c"metadata/versioning/versions")
          assert(versions != null, "Getting versions")

          val vers = List.newBuilder[VersionNumber]

          var node =
            mxmlFindElement(
              versions,
              tree,
              c"version",
              null,
              null,
              MXML_DESCEND_ALL
            )
          while node != null do
            val raw = fromCString(mxmlGetText(node, null))

            raw match
              case VersionNumber.Stable(ver) => vers += ver
              case _                         =>

            node = mxmlFindElement(
              node,
              tree,
              c"version",
              null,
              null,
              MXML_DESCEND_ALL
            )
          end while

          vers.result.sorted.headOption
            .map(_.toString)
            .getOrElse(Err.raise(s"Could not find latest stable version"))
        end if
end MavenFunc
