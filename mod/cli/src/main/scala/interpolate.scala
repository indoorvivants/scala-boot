package scalaboot

import template.*
import scala.util.matching.Regex

def fillFile(
    file: os.Path,
    origin: FileOrigin,
    destination: os.Path,
    settings: Settings,
    overwrite: Boolean = false
): os.Path =
  Err.assert(file.toIO.isFile(), s"File [$file] doesn't exist")
  if !overwrite && destination.toIO.exists() then
    Err.assert(
      destination.toIO.isFile(),
      s"File [$file] exists and cannot be overwritten"
    )
  scribe.debug(s"Filling file [$destination] using [$file] as template")
  val tokenized = tokenizeSource(Source.Stream(file.getInputStream))
  val filled = FillString(tokenized, settings)

  os.makeDir.all(destination / os.up)

  if overwrite then os.write.over(destination, filled)
  else os.write(destination, filled)

  destination
end fillFile

def fillDirectory(
    input: os.Path,
    output: os.Path,
    settings: Settings,
    makeOrigin: os.RelPath => FileOrigin,
    overwrite: Boolean = false,
    allowedPaths: Set[os.RelPath],
    verbatimPatterns: List[Regex],
    offset: os.RelPath = os.RelPath.rel
): Set[os.Path] =
  scribe.debug("Working on", input.toString, "into", output.toString)
  Err.assert(input.toIO.exists(), s"Directory [$input] doesn't exist")
  Err.assert(input.toIO.isDirectory(), s"Path [$input] is not a directory")

  val processed = collection.mutable.Set.empty[os.Path]

  os.list(input).foreach { path =>
    val newLast = FillString(tokenizeSource(Source.Str(path.last)), settings)
    scribe.debug(
      s"Filling $path, changing last to $newLast, tokens are ${tokenizeSource(Source.Str(path.last))}"
    )
    if newLast.nonEmpty then
      val rp = path.relativeTo(input) / os.up / os.RelPath(newLast)
      val fullRelPath = os.RelPath.fromStringSegments(
        offset.segments.concat(rp.segments).toArray
      )
      if os.isDir(path) then
        processed ++=
          fillDirectory(
            input = path,
            output = output / rp,
            settings = settings,
            makeOrigin = makeOrigin,
            overwrite = overwrite,
            offset = offset / rp,
            allowedPaths = allowedPaths,
            verbatimPatterns = verbatimPatterns
          )
      else if allowedPaths.contains(fullRelPath) then
        if !verbatimPatterns.exists(_.matches(path.last)) then
          processed +=
            fillFile(
              file = path,
              origin = makeOrigin(rp),
              destination = output / rp,
              settings = settings,
              overwrite = overwrite
            )
        else
          scribe.debug(s"Copying [$path] into [${output / rp}] verbatim")
          os.copy.over(path, output / rp, createFolders = true)
          processed +=
            output / rp
        end if
      else scribe.debug(s"Path $path was ignored")
      end if

    end if

  }
  processed.toSet
end fillDirectory
