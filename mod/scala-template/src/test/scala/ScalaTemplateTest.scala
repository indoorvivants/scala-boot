package scalaboot.template

import munit.*
import com.indoorvivants.snapshots.munit_integration.MunitSnapshotsIntegration

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
      |""".stripMargin
    val newValue1 = Map(
      "name" -> PropertyValue.Str("Tony Soprano"),
      "clientName" -> PropertyValue.Str("John Doe"),
      "whackingIsLegal" -> PropertyValue.Str("yes"),
      "isSummer" -> PropertyValue.Str("no"),
      "isWinter" -> PropertyValue.Str("yes")
    )
    assertSnapshot("basic interpolation", interpolateAll(newValue, newValue1))
end ScalaTemplateTest

def interpolateAll(str: String, props: Map[String, PropertyValue]) =
  val tokens = tokenize(Source.Str(str))
  val settings = Settings(props, props.keySet.toSeq.zipWithIndex.toMap)
  fillString(tokens, settings)
