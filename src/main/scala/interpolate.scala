package scalaboot

def fill(tokenized: Tokenized, settings: Settings) =
  val sb = new java.lang.StringBuilder
  tokenized.tokens.foreach { tok =>
    tok.fragment match
      case Fragment.Str(content) => sb.append(content)
      case Fragment.Inject(interp) =>
        interp match
          case Interpolation.Variable(name) =>
            settings.values.get(name) match
              case None =>
                Err.raise(
                  s"Unknown variable [$name] in interpolation",
                  tokenized.source,
                  Some(tok.pos)
                )
              case Some(value) =>
                value match
                  case PropertyValue.Str(value) => sb.append(value)

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
    overwrite: Boolean = false
): Set[os.Path] =
  Err.assert(input.toIO.exists(), s"Directory [$input] doesn't exist")
  Err.assert(input.toIO.isDirectory(), s"Path [$input] is not a directory")

  val processed = collection.mutable.Set.empty[os.Path]

  os.walk.stream(input, maxDepth = 1).foreach { path =>
    if path == input / "default.properties" then
      scribe.debug(
        s"Skipping [$path] (default.properties is a reserved filename in scala-boot)"
      )
    else if path.toIO.isDirectory() then
      // TODO: handle conditional names and such
      processed ++=
        fillDirectory(
          path,
          output / (path.relativeTo(input)),
          settings,
          makeOrigin = makeOrigin,
          overwrite
        )
    else
      val rp = path.relativeTo(input)
      processed +=
        fillFile(
          Source.File(path, makeOrigin(rp)),
          output / rp,
          settings,
          overwrite
        )
  }

  processed.toSet
end fillDirectory
