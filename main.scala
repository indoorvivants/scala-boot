package scalaboot

import scribe.Level
import scribe.message.LoggableMessage
import scribe.output.TextOutput

given Conversion[UnsafeCursor, LoggableMessage] with
  def apply(x: UnsafeCursor): LoggableMessage =
    LoggableMessage[UnsafeCursor](a => new TextOutput(a.toString()))(x)

given Conversion[Token, LoggableMessage] with
  def apply(x: Token): LoggableMessage =
    LoggableMessage[Token](a => new TextOutput(a.toString()))(x)

def tokenize(s: String) =
  val b = Vector.newBuilder[Token]
  parse(using Context(text = s))(b.addOne)
  b.result()

@main def scalaboot =
  scribe.Logger.root.withMinimumLevel(Level.Debug).replace()
  val text = """
  |hello world $interpolation$ 
  |and this onessss
  | ss $too$ as $wrong_in$
  """.stripMargin.trim

  pprint.pprintln(tokenize(text))

end scalaboot
