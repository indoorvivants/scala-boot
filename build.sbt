import scala.scalanative.build.Mode
import bindgen.plugin.BindgenMode
import bindgen.interface.Binding

val common = Seq(scalaVersion := Versions.Scala)

def bootProject(id: String) =
  sbt.Project
    .apply(id, file(s"mod/$id"))
    .settings(common)

def bootApp(subfolder: String) =
  bootProject(subfolder)
    .enablePlugins(VcpkgNativePlugin, ScalaNativePlugin)
    .settings(nativeConfig ~= { config =>
      config
        .withIncrementalCompilation(true)
    })

val Versions = new {
  val Scala = "3.3.0"

  val scribe = "3.11.9"
  val osLib = "0.9.1"
  val pprint = "0.8.1"
  val mainargs = "0.5.1"
  val sttp = "3.9.0"
  val roach = "0.0.4"
  val ujson = "3.1.0"
  val snunit = "0.7.2"
  val tapir = "1.7.2"
}

lazy val root =
  project
    .in(file("."))
    .aggregate(
      cli,
      `libgit2-bindings`,
      `repo-indexer`,
      `libcurl-bindings`,
      server,
      `curl-sttp-backend`,
      `http-client`
    )
    .aggregate(`http-protocol`.projectRefs*)

lazy val `http-protocol` = projectMatrix
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

lazy val `http-client` = bootApp("http-client")
  .dependsOn(`curl-sttp-backend`, `http-protocol`.native(Versions.Scala))
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

lazy val `curl-sttp-backend` = bootApp("curl-sttp-backend")
  .dependsOn(`libcurl-bindings`)
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

lazy val server = bootApp("server")
  .dependsOn(`http-protocol`.native(Versions.Scala))
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

lazy val cli = bootApp("cli")
  .dependsOn(`libgit2-bindings`, `http-client`)
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

lazy val `repo-indexer` = bootApp("repo-indexer")
  .dependsOn(`http-client`)
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

lazy val `libcurl-bindings` = bootProject("libcurl-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl"
    ),
    bindgenBindings +=
      Binding(
        vcpkgConfigurator.value.includes("curl") / "curl" / "curl.h",
        "curl",
        cImports = List("curl/curl.h")
      ),
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
    )
  )

lazy val `libgit2-bindings` = bootProject("libgit2-bindings")
  .enablePlugins(BindgenPlugin, ScalaNativePlugin, VcpkgPlugin)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2"
    ),
    bindgenBindings := Seq(
      Binding(
        vcpkgConfigurator.value.includes("libgit2") / "git2.h",
        "libgit",
        // linkName = Some("git2"),
        cImports = List("git2.h"),
        clangFlags = vcpkgConfigurator.value.pkgConfig
          .updateCompilationFlags(List("-fsigned-char"), "libgit2")
          .toList
      )
    ),
    bindgenMode := BindgenMode.Manual(
      sourceDirectory.value / "main" / "scala" / "generated",
      (Compile / resourceDirectory).value / "scala-native"
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

buildServer := {
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

  val cliDestination = dest / "scala-boot"
  IO.copyFile(cliBinary, cliDestination)

  cliDestination
}

lazy val buildRepoIndexer = taskKey[File]("")

buildRepoIndexer := {
  val dest = (ThisBuild / baseDirectory).value / "build"
  IO.createDirectory(dest)
  val cliBinary = (`repo-indexer` / Compile / nativeLink).value

  val cliDestination = dest / "scala-boot-repo-indexer"
  IO.copyFile(cliBinary, cliDestination)

  cliDestination
}

lazy val buildAll = taskKey[File]("")

buildAll := {
  buildCli.value
  buildRepoIndexer.value
  buildServer.value
}

lazy val runServer = taskKey[Unit]("")

runServer := {
  val dest = buildServer.value

  import sys.process.*

  val cmd =
    s"unitd --statedir statedir --log /dev/stderr --no-daemon --control 127.0.0.1:9000"
  val proc = Process(cmd, cwd = dest)

  proc.!
}
