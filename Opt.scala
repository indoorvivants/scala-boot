package scalaboot
import scala.reflect.TypeTest

opaque type Opt[+A] = A | Null
object Opt:
  val empty: Opt[Nothing] = null

  inline def apply[A](a: A): Opt[A] = a

  inline def fromOption[A](o: Option[A]): Opt[A] =
    o.orNull.asInstanceOf[Opt[A]]

  inline def unapply[A](o: Opt[A]): Option[A] =
    if o != null then Some(o.asInstanceOf[o.type & A])
    else None

  extension [A](o: Opt[A])
    inline def toOption = if o == empty then None else Some(o.asInstanceOf[A])
    inline def foreach(inline f: A => Unit): Unit =
      if o != null then f(o.nn)
    inline def fold[B](inline f: A => B)(inline ifEmpty: B): B =
      if o != null then f(o.nn) else ifEmpty

  given [A]: TypeTest[Opt[A], A] with
    inline def unapply(o: Opt[A]): Option[o.type & A] =
      if o != null then Some(o.asInstanceOf[o.type & A])
      else None

  given [A]: CanEqual[Opt[A], Opt[A]] = CanEqual.canEqualAny
  given [A]: CanEqual[Opt[A], Null] = CanEqual.canEqualAny
end Opt
