package scalaboot.template

import util.chaining.*
import scala.util.boundary, boundary.break
import scala.annotation.tailrec

enum StringTemplateExpr:
  case Lit(value: String)
  case Comment(value: String)
  case If(
      condition: BoolExpr,
      thenExpr: StringTemplateExpr,
      elseIf: List[(BoolExpr, StringTemplateExpr)],
      elseExpr: Option[StringTemplateExpr]
  )
  case Variable(name: String, modifiers: List[Modifier])
  case Many(exprs: List[StringTemplateExpr])
end StringTemplateExpr