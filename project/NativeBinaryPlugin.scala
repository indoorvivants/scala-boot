import sbt.Keys.*
import sbt.*
import sbt.plugins.JvmPlugin

import java.nio.file.Files
import java.util.Arrays
import java.util.stream.Collectors
import scala.sys.process
import sjsonnew.JsonFormat
import scala.util.control.NonFatal
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.util.Try
import sjsonnew.support.scalajson.unsafe.Converter
import java.nio.file.StandardCopyOption
import java.nio.file.CopyOption
import java.nio.file.Files
import com.indoorvivants.detective.*, Platform.*

package core {

  class BinConfig(private val params: BinConfig.Params) {
    // getters and setters
    def name: String = params.name
    def withName(n: String): BinConfig = copy(_.copy(name = n))

    private def copy(f: BinConfig.Params => BinConfig.Params): BinConfig =
      new BinConfig(f(params))
  }
  object BinConfig {
    private case class Params(
        name: String
    )

    def default(name: String) = new BinConfig(Params(name = name))
  }
}

object NativeBinaryPlugin extends AutoPlugin {

  object autoImport {
    type BinConfig = core.BinConfig
    val BinConfig = core.BinConfig

    val buildBinaryConfig = settingKey[BinConfig]("")
    val buildBinaryDebug = taskKey[File]("")
    val buildBinaryRelease = taskKey[File]("")
    val buildBinaryPlatformDebug = taskKey[File]("")
    val buildBinaryPlatformRelease = taskKey[File]("")
  }

  override def requires: Plugins = ScalaNativePlugin

  import autoImport.*

  private def writeBinary(
      source: File,
      destinationDir: File,
      log: sbt.Logger,
      platform: Option[Platform.Target],
      debug: Boolean,
      name: String
  ): File = {

    import java.nio.file.*

    val fullName = platform match {
      case None         => name
      case Some(target) =>
        val ext = target.os match {
          case Platform.OS.Windows => ".exe"
          case _                   => ""
        }

        name + "-" + ArtifactNames.coursierString(target) + ext
    }

    val dest = destinationDir / fullName

    Files.createDirectories(destinationDir.toPath())

    Files.copy(
      source.toPath(),
      dest.toPath(),
      StandardCopyOption.COPY_ATTRIBUTES,
      StandardCopyOption.REPLACE_EXISTING
    )

    import scala.sys.process.*

    if (debug && platform.exists(_.os == Platform.OS.MacOS))
      s"dsymutil $dest".!!

    log.info(s"Binary [$name] built in ${dest}")

    dest
  }

  val SN = ScalaNativePlugin.autoImport

  override lazy val projectSettings = Seq(
    buildBinaryConfig := BinConfig.default(name.value),
    buildBinaryDebug :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLink)).value,
        destinationDir = (ThisBuild / baseDirectory).value / "out" / "debug",
        log = sLog.value,
        platform = None,
        debug = true,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryRelease :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
        destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
        log = sLog.value,
        platform = None,
        debug = false,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryPlatformDebug :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLink)).value,
        destinationDir = (ThisBuild / baseDirectory).value / "out" / "debug",
        log = sLog.value,
        platform = Some(Platform.target),
        debug = true,
        name = (buildBinaryConfig.value.name)
      ),
    buildBinaryPlatformRelease :=
      writeBinary(
        source = (ThisProject / Compile / (SN.nativeLinkReleaseFast)).value,
        destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
        log = sLog.value,
        platform = Some(Platform.target),
        debug = false,
        name = (buildBinaryConfig.value.name)
      )
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq()

}
