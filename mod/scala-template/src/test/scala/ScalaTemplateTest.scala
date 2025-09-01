package scalaboot.template

import munit.*
import com.indoorvivants.snapshots.munit_integration.MunitSnapshotsIntegration
import scala.util.Try
import java.io.StringBufferInputStream

class ScalaTemplateTest extends FunSuite, MunitSnapshotsIntegration:
  test("basic interpolation"):
    val newValue = """
      |
      |asdas
      |Hello, I'm my name is $name;format="lower,hyphen"$
      |I am here representing my client, $clientName$
      |$if(whackingIsLegal.truthy)$Prepare to be whacked $endif$
      |$if(isSummer.truthy)$Prepare to be picnic'd to death$elseif(isWinter.truthy)$Prepare for ice fishing$endif$
      |$if(isSummer.truthy)$Prepare to be picnic'd to death$else$We'll think of a less season specific death$endif$
      |$hello__packaged$ $hello__hyphen,package$
      |$hello__Camel$ $hello__Package$
      |
      |test("HelloWorld returns status code 200") {
      |      assertIO(retHelloWorld.flatMap(_.as[String]), "{\"message\":\"Hello, world\"}")
      |}
      |""".stripMargin
    val newValue1 = Map(
      "name" -> PropertyValue.Str("Tony Soprano"),
      "clientName" -> PropertyValue.Str("John Doe"),
      "whackingIsLegal" -> PropertyValue.Str("yes"),
      "isSummer" -> PropertyValue.Str("no"),
      "isWinter" -> PropertyValue.Str("yes"),
      "hello" -> PropertyValue.Str("my stuff")
    )
    assertSnapshot("basic interpolation", interpolateAll(newValue, newValue1))

  test("reading properties"):
    val maven2Args = Func
      .builder("maven")
      .withArg(Param.Str)
      .withArg(Param.Str)
      .withApply((org, artifact) => Try(s"Resolving $org/$artifact"))

    val maven3Args = Func
      .builder("maven")
      .withArg(Param.Str)
      .withArg(Param.Str)
      .withArg(Param.Str)
      .withApply((org, artifact, stability) =>
        Try(s"Resolving $org/$artifact in $stability")
      )

    val propsFile =
      """
      |hello  = world
      |version  = maven(ws.unfiltered, unfiltered_2.11)
      |version_stable  = maven(ws.unfiltered, unfiltered_2.11, stable)
      """.stripMargin

    val props = ReadProperties(new StringBufferInputStream(propsFile))

    val defaults = MakeDefaults(props, Seq(maven2Args, maven3Args)).values

    assertEquals(
      defaults("version").stringValue,
      "Resolving ws.unfiltered/unfiltered_2.11"
    )
    assertEquals(
      defaults("version_stable").stringValue,
      "Resolving ws.unfiltered/unfiltered_2.11 in stable"
    )
    assertEquals(defaults("hello").stringValue, "world")

end ScalaTemplateTest

def interpolateAll(str: String, props: Map[String, PropertyValue]) =
  val tokens = tokenizeSource(Source.Str(str))
  val settings = Settings(props, props.keySet.toSeq.zipWithIndex.toMap)
  FillString(tokens, settings)
