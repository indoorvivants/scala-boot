package scalaboot.template

import scala.util.*
import scala.Tuple.Append

enum Param:
  // case Num
  case Str

type ResultType[P <: Param] = P match
  // case Param.Num.type => Int
  case Param.Str.type => String

case class FuncCall(name: String, params: List[(Param, Any)])

sealed trait Func:
  type In <: Tuple
  def name: String
  def apply(in: In): Try[String]
  def prototype: Array[Param]
  def fromArgs(ar: Array[Any]): In
  
  private lazy val cached = prototype
  override def toString(): String = s"Func<$name: ${cached.mkString(",")}>"

object Func:
  def builder(name: String) = FuncBuilder(EmptyFunc(name))

class EmptyFunc(val name: String) extends Func:
  override type In = EmptyTuple
  override def apply(in: EmptyTuple): Try[String] = Success("")
  override def prototype: Array[Param] = Array()
  override def fromArgs(ar: Array[Any]): EmptyTuple = EmptyTuple

case class FuncBuilder[Inc <: Tuple](start: Func {type In = Inc}):
  self =>

  def withArg(
      param: Param
  ): FuncBuilder[Tuple.Append[Inc, ResultType[param.type]]] =
    type Res = Tuple.Append[Inc, ResultType[param.type]]
    val newFunc = new Func:
      override type In = Res
      private final val cached = self.start.prototype :+ param
      override def apply(in: Res): Try[String] =
        ???
      override def name: String = self.start.name
      override def fromArgs(
          ar: Array[Any]
      ): Res =
        val first = start.fromArgs(ar.dropRight(1))
        val last = ar.last.asInstanceOf[ResultType[param.type]]

        first :* last
      override def prototype: Array[Param] = cached

    FuncBuilder[Res](newFunc)
  end withArg

  def withApply(f: Inc => Try[String]) =
    new Func:
      override type In = Inc
      override def name: String = self.start.name
      override def apply(in: In): Try[String] = f(in)
      override def prototype: Array[Param] = self.start.prototype
      override def fromArgs(ar: Array[Any]): In = self.start.fromArgs(ar)
end FuncBuilder
