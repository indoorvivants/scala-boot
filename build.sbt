import scala.scalanative.build.Mode
import bindgen.plugin.BindgenMode
import bindgen.interface.Binding

val common = Seq(scalaVersion := "3.3.0")

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
        .withMode(
          sys.env
            .get("SCALABOOT_RELEASE")
            .map(_ => Mode.releaseFast)
            .getOrElse(config.mode)
        )
    })

val Versions = new {
  val scribe = "3.11.9"
  val osLib = "0.9.1"
  val pprint = "0.8.1"
  val mainargs = "0.5.1"
  val sttp = "3.9.0"
  val roach = "0.0.3"
  val ujson = "3.1.0"
}

lazy val root =
  project
    .in(file("."))
    .aggregate(cli, `libgit2-bindings`, `repo-indexer`, `libcurl-bindings`)

lazy val cli = bootApp("cli")
  .dependsOn(`libgit2-bindings`)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "libgit2"
    ),
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs,
      "com.indoorvivants.roach" %%% "core" % Versions.roach
    )
  )

lazy val `repo-indexer` = bootApp("repo-indexer")
  .dependsOn(`libcurl-bindings`)
  .settings(
    vcpkgDependencies := VcpkgDependencies(
      "curl"
    ),
    vcpkgNativeConfig ~= { _.addRenamedLibrary("curl", "libcurl") },
    libraryDependencies ++= Seq(
      "com.outr" %%% "scribe" % Versions.scribe,
      "com.lihaoyi" %%% "pprint" % Versions.pprint,
      "com.lihaoyi" %%% "os-lib" % Versions.osLib,
      "com.lihaoyi" %%% "mainargs" % Versions.mainargs,
      "com.lihaoyi" %%% "ujson" % Versions.ujson,
      "com.softwaremill.sttp.client3" %%% "core" % Versions.sttp
    )
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
