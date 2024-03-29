import scala.scalanative.build.Mode
import bindgen.plugin.BindgenMode
import bindgen.interface.Binding

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
  val Scala = "3.3.2"

  val scribe = "3.12.2"
  val osLib = "0.9.1"
  val pprint = "0.8.1"
  val mainargs = "0.5.4"
  val sttp = "3.9.0"
  val roach = "0.0.6"
  val ujson = "3.1.3"
  val snunit = "0.7.2"
  val tapir = "1.8.1"
}

lazy val root =
  project
    .in(file("."))
    .aggregate(
      cli,
      libgit2Bindings,
      repoIndexer,
      libcurlBindings,
      server,
      curlSttpBackend,
      httpClient
    )
    .aggregate(httpProtocol.projectRefs*)
    .aggregate(scalaTemplate.projectRefs*)

lazy val scalaTemplate = projectMatrix
  .withId("scala-template")
  .in(file("mod/scala-template"))
  .nativePlatform(Seq(Versions.Scala))
  .settings(
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib
    )
  )
// .jsPlatform(Seq(Versions.Scala))

lazy val httpProtocol = projectMatrix
  .withId("http-protocol")
  .in(file("mod/http-protocol"))
  .nativePlatform(Seq(Versions.Scala))
  // .jsPlatform(Seq(Versions.Scala))
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "ujson" % Versions.ujson,
      "com.softwaremill.sttp.tapir" %%% "tapir-core" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-upickle" % Versions.tapir
    )
  )

lazy val httpClient = projApp("http-client")
  .dependsOn(curlSttpBackend, httpProtocol.native(Versions.Scala))
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % Versions.tapir
    )
  )

lazy val curlSttpBackend = projApp("curl-sttp-backend")
  .dependsOn(libcurlBindings)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client3" %%% "core" % Versions.sttp
    )
  )

lazy val server = projApp("server")
  .dependsOn(httpProtocol.native(Versions.Scala))
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      (ThisBuild / baseDirectory).value / "server-vcpkg.json"
    ),
    libraryDependencies ++= Seq(
      "com.github.lolgab" %%% "snunit-tapir" % Versions.snunit,
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs,
      "com.indoorvivants.roach" %%% "core" % Versions.roach,
      "com.indoorvivants.roach" %%% "upickle" % Versions.roach
    ),
    nativeConfig ~= { _.withEmbedResources(true) }
  )

lazy val cli = projApp("cli")
  .dependsOn(libgit2Bindings, httpClient, scalaTemplate.native(Versions.Scala))
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2",
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs
    )
  )

lazy val repoIndexer = projApp("repo-indexer")
  .dependsOn(httpClient)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl",
      "libidn2"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs,
      "com.lihaoyi" %%% "ujson" % Versions.ujson,
      "com.softwaremill.sttp.client3" %%% "core" % Versions.sttp,
      "com.softwaremill.sttp.client3" %%% "upickle" % Versions.sttp
    ),
    libraryDependencySchemes += "com.lihaoyi" % "upickle_native0.4_3" % VersionScheme.Always
  )

lazy val libcurlBindings = proj("libcurl-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl"
    ),
    bindgenBindings +=
      Binding
        .builder(
          vcpkgConfigurator.value.includes("curl") / "curl" / "curl.h",
          "curl"
        )
        .addCImport("curl/curl.h")
        .build,
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
    )
  )

lazy val libgit2Bindings = proj("libgit2-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2"
    ),
    bindgenBindings +=
      Binding
        .builder(
          vcpkgConfigurator.value.includes("libgit2") / "git2.h",
          "libgit"
        )
        .addCImport("git2.h")
        .withClangFlags(
          vcpkgConfigurator.value.pkgConfig
            .updateCompilationFlags(List("-fsigned-char"), "libgit2")
            .toList
        )
        .build,
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
    )
  )

lazy val devServer = proj("dev-server")
  .enablePlugins(RevolverPlugin)
  .settings(
    fork := true,
    envVars ++= Map(
      "SCALABOOT_SERVER_BINARY" -> (ThisBuild / buildServer).value.toString,
      "SCALABOOT_UNITD_COMMAND" -> UNITD_LOCAL_COMMAND,
      "SCALABOOT_SERVER_CWD" -> ((ThisBuild / baseDirectory).value / "build").toString,
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
  val dest = (ThisBuild / baseDirectory).value / "build"
  val statedir = dest / "statedir"
  IO.createDirectory(dest)
  val serverBinary = (server / Compile / nativeLink).value

  IO.copyFile(serverBinary, dest / "server")
  IO.copyFile(dest.getParentFile() / "conf.json", statedir / "conf.json")

  dest
}

lazy val buildCli = taskKey[File]("")
buildCli := {
  val dest = (ThisBuild / baseDirectory).value / "build"
  IO.createDirectory(dest)
  val cliBinary = (cli / Compile / nativeLink).value

  val cliDestination = dest / NAME
  IO.copyFile(cliBinary, cliDestination)

  cliDestination
}

lazy val buildRepoIndexer = taskKey[File]("")
buildRepoIndexer := {
  val dest = (ThisBuild / baseDirectory).value / "build"
  IO.createDirectory(dest)
  val cliBinary = (repoIndexer / Compile / nativeLink).value

  val cliDestination = dest / s"${NAME}-repo-indexer"
  IO.copyFile(cliBinary, cliDestination)

  cliDestination
}

lazy val buildAll = taskKey[File]("")
buildAll := {
  buildCli.value
  buildRepoIndexer.value
  buildServer.value
}

def UNITD_LOCAL_COMMAND =
  "unitd --statedir statedir --log /dev/stderr --no-daemon --control 127.0.0.1:9000"

lazy val runServer = taskKey[Unit]("")
runServer := {
  val dest = buildServer.value

  import sys.process.*

  val proc = Process(UNITD_LOCAL_COMMAND, cwd = dest)

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
    "buildCli",
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
