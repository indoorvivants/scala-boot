package scalaboot

import template.*

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
  val tokenized = tokenize(Source.Stream(file.getInputStream))
  val filled = fillString(tokenized, settings)

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
    skip: Set[os.Path] = Set.empty
): Set[os.Path] =
  scribe.debug("Working on", input.toString, "into", output.toString)
  Err.assert(input.toIO.exists(), s"Directory [$input] doesn't exist")
  Err.assert(input.toIO.isDirectory(), s"Path [$input] is not a directory")

  val processed = collection.mutable.Set.empty[os.Path]

  os.list(input).filterNot(skip).foreach { path =>
    val newLast = fillString(tokenize(Source.Str(path.last)), settings)
    if newLast.nonEmpty then
      val rp = path.relativeTo(input) / os.up / newLast
      if os.isDir(path) then
        processed ++=
          fillDirectory(
            input = path,
            output = output / rp,
            settings = settings,
            makeOrigin = makeOrigin,
            overwrite = overwrite,
            skip = skip
          )
      else
        processed +=
          fillFile(
            file = path,
            origin = makeOrigin(rp),
            destination = output / rp,
            settings = settings,
            overwrite = overwrite
          )
      end if

    end if

  }
  processed.toSet
end fillDirectory
