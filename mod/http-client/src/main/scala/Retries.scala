package scalaboot.client

import scala.concurrent.duration.*
import scala.util.control.NonFatal
import scala.util.Try

enum Action:
  case Stop
  case KeepRetrying(remaining: Int, sleep: FiniteDuration)

trait Retries:
  def next(attempt: Int): Action

object Retries:
  def no: Retries = new Retries:
    override def next(attempt: Int): Action = Action.Stop

  def linear(max: Int, delay: FiniteDuration): Retries = new Retries:
    override def next(attempt: Int): Action =
      if attempt <= max then Action.KeepRetrying(max - attempt, delay)
      else Action.Stop

  def exponential(
      max: Int,
      step: FiniteDuration
  ): Retries = new Retries:
    override def next(attempt: Int): Action =
      if attempt <= max then
        Action.KeepRetrying(
          max - attempt,
          ((1L << (attempt - 1)) * step.toMillis).millis
        )
      else Action.Stop
end Retries

case class Attempt(
    action: Action.KeepRetrying,
    err: Throwable
)

def retryable[A](
    ioa: => A,
    errors: PartialFunction[Throwable, Boolean] = { case NonFatal(_) => true },
    policy: Retries = Retries.no,
    logger: Attempt => Unit = _ => (),
) =
  val isError = errors.isDefinedAt(_)

  def go(n: Int, last: Either[Throwable, A]): A =
    policy.next(n) match
      case Action.Stop => last.fold(throw _, identity)
      case a @ Action.KeepRetrying(_, delay) =>
        val result = Try(ioa).toEither
        result match
          case Left(value) if isError(value) =>
            logger(Attempt(a, value))
            Thread.sleep(delay.toMillis)
            go(n + 1, result)
          case Left(err)    => throw err
          case Right(value) => value

  go(1, Try(ioa).toEither)
end retryable
