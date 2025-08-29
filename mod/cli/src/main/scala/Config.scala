package scalaboot

// import mainargs.{main, arg, ParserForClass, Flag}
// import mainargs.TokensReader

import decline_derive.*
import com.monovore.decline.Argument
import os.Path
import cats.data.ValidatedNel
import cats.data.Validated

enum CLI derives CommandApplication:
  case Go(
      @Positional("template")
      @Help("Github coordinates of the template (e.g. scala/scala3.g8)")
      template: String,
      @Short("b")
      @Help("Resolve a template within a given branch")
      branch: Option[String] = None,
      @Short("t")
      @Help("Resolve a template within a given tag")
      tag: Option[String] = None,
      @Short("o")
      @Help("Output directory")
      out: Option[os.Path] = None,
      @Short("y")
      @Flag(false)
      yes: Boolean,
      @Short("v")
      @Help("Enable verbose (really verbose) logging")
      @Flag(false)
      verbose: Boolean
  )
  case Search(
      @Positional("query")
      query: String,
      @Help("URI of the Scala Boot server")
      api: Option[String] = None,
      @Help("Interactively select a template to apply")
      @Flag(true)
      interactive: Boolean,
      @Short("a")
      @Help("Show all results instead of the top 5")
      all: Boolean,
      @Short("y")
      @Help(
        "When template is selected in interactive mode, apply it with defaults"
      )
      @Flag(false)
      yes: Boolean,
      @Short("v")
      @Help("Enable verbose (really verbose) logging")
      @Flag(false)
      verbose: Boolean
  )
end CLI

object CLI:
  given Argument[os.Path] with
    override def defaultMetavar: String = "path"
    override def read(string: String): ValidatedNel[String, Path] =
      Validated.validNel(os.Path(string, os.pwd))

enum FileOrigin:
  case Local, None
  case FromURL(base: String, relative: os.RelPath)
