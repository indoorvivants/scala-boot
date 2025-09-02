import scala.scalanative.build.SourceLevelDebuggingConfig
import scala.scalanative.build.Mode
import bindgen.plugin.BindgenMode
import bindgen.interface.Binding

import java.nio.file.StandardCopyOption
import java.nio.file.CopyOption
import java.nio.file.Files
import com.indoorvivants.detective.*, Platform.*

def NAME = "scala-boot"

def proj(id: String) =
  sbt.Project
    .apply(id, file(s"mod/$id"))
    .settings(scalaVersion := Versions.Scala)
    .settings(scalacOptions += "-Wunused:all")

def projApp(subfolder: String) =
  proj(subfolder)
    .enablePlugins(VcpkgNativePlugin, ScalaNativePlugin)
    .settings(nativeConfig ~= { config =>
      config
        .withIncrementalCompilation(true)
    })

val Versions = new {
  val Scala = "3.7.2"

  val scribe = "3.16.0"
  val osLib = "0.11.3"
  val pprint = "0.9.0"
  val mainargs = "0.7.6"
  val sttp = "4.0.8"
  val roach = "0.1.0"
  val ujson = "3.3.1"
  val snunit = "0.10.3"
  val tapir = "1.11.31+142-54cc665d-SNAPSHOT"
  val munit = "1.1.1"
  val declineDerive = "0.3.1"
  val cue4s = "0.0.9"
}

lazy val root =
  project
    .in(file("."))
    .aggregate(
      cli,
      libgit2Bindings,
      repoIndexer,
      server,
      httpClient
    )
    .aggregate(httpProtocol.projectRefs*)
    .aggregate(scalaTemplate.projectRefs*)

lazy val scalaTemplate = projectMatrix
  .withId("scala-template")
  .in(file("mod/scala-template"))
  .nativePlatform(Seq(Versions.Scala))
  .jvmPlatform(Seq(Versions.Scala))
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.github.j-mie6" %%% "parsley" % "5.0.0-M15",
      "org.scalameta" %%% "munit" % Versions.munit % Test
    ),
    nativeConfig ~= (_.withIncrementalCompilation(true))
  )
  .settings(
    snapshotsPackageName := "scalaboot.template",
    snapshotsIntegrations += SnapshotIntegration.MUnit,
    snapshotsForceOverwrite := !sys.env.contains("CI")
  )
  .enablePlugins(SnapshotsPlugin)
  .jsPlatform(Seq(Versions.Scala))

lazy val httpProtocol = projectMatrix
  .withId("http-protocol")
  .in(file("mod/http-protocol"))
  .nativePlatform(Seq(Versions.Scala))
  .jsPlatform(Seq(Versions.Scala))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-core" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % Versions.tapir
    )
  )

lazy val httpClient = projApp("http-client")
  .dependsOn(httpProtocol.native(Versions.Scala))
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %%% "core" % Versions.sttp,
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % Versions.tapir
    )
  )

lazy val server = projApp("server")
  .dependsOn(httpProtocol.native(Versions.Scala))
  .settings(
    vcpkgDependencies := VcpkgDependencies("libpq", "libidn2"),
    libraryDependencies ++= Seq(
      "com.github.lolgab" %%% "snunit-tapir" % Versions.snunit,
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs,
      "com.indoorvivants.roach" %%% "core" % Versions.roach,
      "com.indoorvivants.roach" %%% "circe" % Versions.roach
    ),
    nativeConfig ~= { _.withEmbedResources(true) }
  )
  .settings(configurePlatform())

lazy val cli = projApp("cli")
  .dependsOn(
    libgit2Bindings,
    mxmlBindings,
    httpClient,
    scalaTemplate.native(Versions.Scala)
  )
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2",
      "mxml",
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= {
      _.addRenamedLibrary("curl", "libcurl").addRenamedLibrary("mxml", "mxml4")
    },
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.indoorvivants" %%% "decline-derive" % Versions.declineDerive,
      "tech.neander" %%% "cue4s" % Versions.cue4s
    ),
    nativeConfig ~= (_.withSourceLevelDebuggingConfig(
      SourceLevelDebuggingConfig.enabled
    ))
  )
  .settings(configurePlatform())

lazy val repoIndexer = projApp("repo-indexer")
  .dependsOn(httpClient)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client4" %%% "circe" % Versions.sttp,
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.indoorvivants" %%% "decline-derive" % Versions.declineDerive
    )
  )
  .settings(configurePlatform())

lazy val libgit2Bindings = proj("libgit2-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2"
    ),
    bindgenBindings +=
      Binding(
        vcpkgConfigurator.value.includes("libgit2") / "git2.h",
        "libgit"
      )
        .addCImport("git2.h")
        .withClangFlags(
          vcpkgConfigurator.value.pkgConfig
            .updateCompilationFlags(List("-fsigned-char"), "libgit2")
            .toList
        ),
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
    )
  )

lazy val mxmlBindings = proj("mxml-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "mxml"
    ),
    bindgenBindings +=
      Binding(
        vcpkgConfigurator.value.includes("mxml") / "mxml.h",
        "mxml"
      )
        .addCImport("mxml.h")
        .withNoLocation(true)
        .withClangFlags(
          vcpkgConfigurator.value.pkgConfig
            .updateCompilationFlags(List("-fsigned-char"), "mxml4")
            .toList
        ),
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
    )
  )

import org.scalajs.linker.interface.ModuleSplitStyle

lazy val webapp = proj("webapp")
  .enablePlugins(ScalaJSPlugin) // Enable the Scala.js plugin in this project
  .dependsOn(httpProtocol.js(Versions.Scala))
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("scalaboot"))
        )
    },
    libraryDependencies += "com.raquo" %%% "laminar" % "17.2.1",
    libraryDependencies += "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % Versions.tapir
  )

lazy val devServer = proj("dev-server")
  .enablePlugins(RevolverPlugin)
  .settings(
    fork := true,
    envVars ++= Map(
      "SCALABOOT_UNITD_COMMAND" -> UNITD_LOCAL_COMMAND,
      "SCALABOOT_SERVER_CWD" -> (ThisBuild / buildServer).value.toString,
      "PG_DB" -> "scala_boot"
    )
  )

lazy val writeCompileCommands = taskKey[Unit]("")
writeCompileCommands := {
  val dest = (ThisBuild / baseDirectory).value / "compile_flags.txt"
  val flags = nativeConfig.value.compileOptions

  IO.writeLines(dest, flags)

  sLog.value.info(s"Compile flags were written to $dest")
}

lazy val buildServer = taskKey[File]("")
ThisBuild / buildServer := {
  val dest = (ThisBuild / baseDirectory).value / "out" / "debug" / "server"

  val serverBinary = writeBinary(
    source = (server / Compile / nativeLink).value,
    destinationDir = dest,
    log = sLog.value,
    platform = None,
    debug = true,
    name = "scala-boot-server"
  )

  val statedir = dest / "statedir"

  IO.createDirectory(statedir)

  IO.copyFile(
    (ThisBuild / baseDirectory).value / "conf.json",
    statedir / "conf.json"
  )

  dest
}

lazy val buildServerRelease = taskKey[File]("")
ThisBuild / buildServerRelease := {
  val dest = (ThisBuild / baseDirectory).value / "out" / "release" / "server"

  val serverBinary = writeBinary(
    source = (server / Compile / nativeLinkReleaseFast).value,
    destinationDir = dest,
    log = sLog.value,
    platform = None,
    debug = true,
    name = "scala-boot-server"
  )

  val statedir = dest / "statedir"

  IO.createDirectory(statedir)

  IO.copyFile(
    (ThisBuild / baseDirectory).value / "conf.json",
    statedir / "conf.json"
  )

  dest
}

lazy val buildWebappRelease = taskKey[File]("")
ThisBuild / buildWebappRelease := {
  val dest = (ThisBuild / baseDirectory).value / "out" / "release" / "server"

  import scala.sys.process.*

  assert(
    Process("npm install", cwd = file("./mod/webapp")).! == 0,
    "Command [npm install] did not finish successfully"
  )

  assert(
    Process("npm run build", cwd = file("./mod/webapp")).! == 0,
    "Command [npm run build] did not finish successfully"
  )

  IO.copyDirectory(file("./mod/webapp/dist"), dest / "static")

  dest / "static"
}

lazy val buildWebapp = taskKey[File]("")
ThisBuild / buildWebapp := {
  val dest = (ThisBuild / baseDirectory).value / "out" / "debug" / "server"

  import scala.sys.process.*

  assert(
    Process("npm run build", cwd = file("./mod/webapp")).! == 0,
    "Command [npm run build] did not finish successfully"
  )

  IO.copyDirectory(file("./mod/webapp/dist"), dest / "static")

  dest / "static"
}

lazy val buildCLI = taskKey[File]("")
buildCLI := {
  writeBinary(
    source = (cli / Compile / nativeLink).value,
    destinationDir = (ThisBuild / baseDirectory).value / "out" / "debug",
    log = sLog.value,
    platform = None,
    debug = true,
    name = "scala-boot"
  )
}

lazy val buildReleaseCLI = taskKey[File]("")
buildReleaseCLI := {
  writeBinary(
    source = (cli / Compile / nativeLinkReleaseFast).value,
    destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
    log = sLog.value,
    platform = None,
    debug = false,
    name = "scala-boot"
  )
}

lazy val buildPlatformCLI = taskKey[File]("")
buildPlatformCLI := {
  writeBinary(
    source = (cli / Compile / nativeLinkReleaseFast).value,
    destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
    log = sLog.value,
    platform = Some(Platform.target),
    debug = false,
    name = "scala-boot"
  )
}

lazy val buildRepoIndexer = taskKey[File]("")
buildRepoIndexer := {
  writeBinary(
    source = (repoIndexer / Compile / nativeLink).value,
    destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
    log = sLog.value,
    platform = Some(Platform.target),
    debug = true,
    name = "repo-indexer"
  )
}

lazy val buildRepoIndexerRelease = taskKey[File]("")
buildRepoIndexerRelease := {
  writeBinary(
    source = (cli / Compile / nativeLinkReleaseFast).value,
    destinationDir = (ThisBuild / baseDirectory).value / "out" / "release",
    log = sLog.value,
    platform = None,
    debug = false,
    name = "repo-indexer"
  )
}

lazy val buildAll = taskKey[File]("")
buildAll := {
  buildCLI.value
  buildRepoIndexer.value
  buildServer.value
}

def UNITD_LOCAL_COMMAND = {
  import sys.process.*
  val proc = Process("which unitd").!!
  s"$proc --statedir statedir --log /dev/stderr --no-daemon --control 127.0.0.1:9000"
}

lazy val runServer = taskKey[Unit]("")
runServer := {
  val dest = buildServer.value

  import sys.process.*

  val proc = Process(UNITD_LOCAL_COMMAND, cwd = dest, "PG_DB" -> "scala_boot")

  proc.!
}

import sbtwelcome.*

logoColor := scala.Console.MAGENTA
welcomeEnabled := !sys.env.contains("CI")
logo :=
  s"""
     |   ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄▄▄▄▄ ▄▄▄     ▄▄▄▄▄▄    ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄
     |  █       █       █      █   █   █      █  █  ▄    █       █       █       █
     |  █  ▄▄▄▄▄█       █  ▄   █   █   █  ▄   █  █ █▄█   █   ▄   █   ▄   █▄     ▄█
     |  █ █▄▄▄▄▄█     ▄▄█ █▄█  █   █   █ █▄█  █  █       █  █ █  █  █ █  █ █   █
     |  █▄▄▄▄▄  █    █  █      █   █▄▄▄█      █  █  ▄   ██  █▄█  █  █▄█  █ █   █
     |   ▄▄▄▄▄█ █    █▄▄█  ▄   █       █  ▄   █  █ █▄█   █       █       █ █   █
     |  █▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄█ █▄▄█▄▄▄▄▄▄▄█▄█ █▄▄█  █▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█ █▄▄▄█
     | (name TBD)
     |
     |${version.value}
     |
     |${scala.Console.YELLOW}Scala ${Versions.Scala} ${scala.Console.RESET}
     |
     |Pre-requisites for building/running:
     | All tools require Clang installed. It's installed by default on MacOS.
     | First run will bootstrap Vcpkg and build a lot of native dependencies
     | from scratch. This will take a while, and then it will become much quicker
     |
     |  Server:
     |    1. Install Unitd: https://unit.nginx.org/installation/#homebrew
     |    2. Postgres running with password "mysecretpassword"
     |       In Docker:
     |       docker run -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
     |
     |Sample commands:
     | ${scala.Console.BOLD}cli/run go softwaremill/tapir.g8${scala.Console.RESET} - template a repository
     | ${scala.Console.BOLD}cli/run search 'akka http' --api http://localhost:8080${scala.Console.RESET} - search against a locally running server
     | ${scala.Console.BOLD}~dev-server/reStart${scala.Console.RESET} - continuously restard the unitd server on changes.
     |    Note: this process won't be interactive, the server binary needs to be relinked if it ever changes
     |          and that takes a relatively long time.
     |          Still, this is useful for running a server in the background while you use the SBT shell to issue commands to it.
     |
     |""".stripMargin

usefulTasks := Seq(
  UsefulTask(
    "buildCLI",
    s"Build ./build/$NAME CLI (search and templating)"
  ),
  UsefulTask(
    "buildServer",
    "Build HTTP server at ./build/server"
  ),
  UsefulTask(
    "buildRepoIndexer",
    s"Build ./build/$NAME-repo-indexer CLI (repo indexing)"
  ),
  UsefulTask(
    "runServer",
    s"(blocking) Run the $NAME server at http://localhost:8080"
  ),
  UsefulTask(
    "runDevServer",
    s"(background) Run the $NAME server at http://localhost:8080"
  ),
  UsefulTask(
    "localRepoIndex",
    "Run repo indexer CLI pointing at http://localhost:8080"
  ),
  UsefulTask(
    "localSearch",
    "Run search CLI pointing at http://localhost:8080"
  ),
  UsefulTask(
    "writeCompileCommands",
    "Write compile_flags.txt file, to use with Clang LSP server (for C source files)"
  )
)

addCommandAlias(
  "localRepoIndex",
  "repo-indexer/run --api http://localhost:8080 "
)
addCommandAlias("localSearch", "cli/run search --api http://localhost:8080 ")
addCommandAlias("runDevServer", "dev-server/reStart")

def configurePlatform(
    rename: String => String = identity
) =
  Seq(
    nativeConfig := {
      import Platform.OS.*
      val conf =
        nativeConfig.value
      val arch64 =
        if (
          Platform.os == MacOS && Platform.arch == Platform.Arch.Arm && Platform.bits == Platform.Bits.x64
        )
          List("-arch", "arm64")
        else Nil

      conf
        .withLinkingOptions(
          conf.linkingOptions ++ arch64
        )
        .withCompileOptions(
          conf.compileOptions ++ arch64
        )
    }
  )

def writeBinary(
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
