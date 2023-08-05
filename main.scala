package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

given Conversion[UnsafeCursor, LoggableMessage] =
  LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))

given Conversion[Token, LoggableMessage] =
  LoggableMessage[Token](a => new TextOutput(a.toString()))

@main def scalaboot =
  // scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
  // val text = """
  // |hello world $interp$ 
  // |and this onessss
  // | ss $hello$ as $cuz$
  // """.stripMargin.trim

  // val tokenized = tokenize(text)

  val test = os.pwd / "test-project"

  val props = readProperties(test / "default.properties")
  val defaults = makeDefaults(props)

  fillDirectory(
    input = test,
    output = os.pwd / "test-out",
    defaults,
    overwrite = true
  )

//   pprint.pprintln(props)
//   pprint.pprintln(defaults)

//   pprint.pprintln(fill(tokenized, defaults))

end scalaboot
