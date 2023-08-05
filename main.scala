package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

given Conversion[UnsafeCursor, LoggableMessage] = x =>
  LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))(x)

given Conversion[Token, LoggableMessage] = x =>
  LoggableMessage[Token](a => new TextOutput(a.toString()))(x)

@main def scalaboot =
  scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
  val text = """
  |hello world $interp$ 
  |and this onessss
  | ss $hello$ as $cuz$
  """.stripMargin.trim

  val tokenized = tokenize(text)

  val props = readProperties(os.pwd / "template.properties")
  val defaults = makeDefaults(props)

  pprint.pprintln(props)
  pprint.pprintln(defaults)

  pprint.pprintln(fill(tokenized, defaults))

end scalaboot
