package scalaboot

def fill(tokenized: Tokenized, settings: Settings) =
  val sb = new java.lang.StringBuilder
  tokenized.tokens.foreach { tok =>
    tok.fragment match
      case Fragment.Str(content) => sb.append(content)
      case Fragment.Inject(interp) =>
        interp match
          case Interpolation.Variable(name, formats) =>
            settings.values.get(name) match
              case None =>
                Err.raise(
                  s"Unknown variable [$name] in interpolation",
                  tokenized.source,
                  Some(tok.pos)
                )
              case Some(value) =>
                value match
                  case PropertyValue.Str(value) =>
                    sb.append(applyFormats(value, formats))

          case Interpolation.Comment =>

  }
  sb.toString()
end fill

def fillFile(
    file: Source.File,
    destination: os.Path,
    settings: Settings,
    overwrite: Boolean = false
): os.Path =
  Err.assert(file.path.toIO.isFile(), s"File [$file] doesn't exist")
  if !overwrite && destination.toIO.exists() then
    Err.assert(
      destination.toIO.isFile(),
      s"File [$file] exists and cannot be overwritten"
    )
  scribe.debug(s"Filling file [$destination] using [$file] as template")
  val tokenized = tokenize(file)
  val filled = fill(tokenized, settings)

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
    val newLast = fill(tokenize(Source.Str(path.last)), settings)
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
            file = Source.File(path, makeOrigin(rp)),
            destination = output / rp,
            settings = settings,
            overwrite = overwrite
          )
      end if

    end if

  }
  processed.toSet
end fillDirectory
