import scala.scalanative.build.Mode
import bindgen.plugin.BindgenMode
scalaVersion := "3.3.0"

libraryDependencies ++= Seq(
  "com.outr" %%% "scribe" % "3.11.9",
  "com.lihaoyi" %%% "pprint" % "0.8.1",
  "com.lihaoyi" %%% "os-lib" % "0.9.1",
  "com.lihaoyi" %%% "mainargs" % "0.5.0"
)

enablePlugins(VcpkgNativePlugin, ScalaNativePlugin, BindgenPlugin)

vcpkgDependencies := VcpkgDependencies(
  "libgit2"
)

import bindgen.interface.Binding

bindgenBindings := Seq(
  Binding(
    vcpkgConfigurator.value.includes("libgit2") / "git2.h",
    "libgit",
    linkName = Some("git2"),
    cImports = List("git2.h"),
    clangFlags = vcpkgConfigurator.value.pkgConfig
      .updateCompilationFlags(List("-fsigned-char"), "libgit2")
      .toList
  )
)

bindgenMode := BindgenMode.Manual(
  sourceDirectory.value / "main" / "scala" / "generated",
  (Compile / resourceDirectory).value / "scala-native"
)

nativeConfig ~= { config =>
  config
    .withIncrementalCompilation(true)
    .withMode(
      sys.env
        .get("SCALABOOT_RELEASE")
        .map(_ => Mode.releaseFast)
        .getOrElse(config.mode)
    )
}
