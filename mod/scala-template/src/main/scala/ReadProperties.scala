package scalaboot.template

import java.io.InputStream

def ReadProperties(is: InputStream) =
  val props = java.util.Properties()
  props.load(is)

  val propsBuilder = List.newBuilder[(String, FuncCall | Tokenized)]
  val names = props.stringPropertyNames()
  names.forEach { name =>
    propsBuilder.addOne(
      name -> tokenizePropertyValue(props.getProperty(name))
    )
  }
  Props(
    propsBuilder.result().toMap,
    propsBuilder.result().map(_._1).zipWithIndex.toMap
  )
end ReadProperties
