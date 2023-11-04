/*
package com.rockthejvm.part2effects

import zio.*

import java.io.IOException
import java.net.NoRouteToHostException
import scala.util.{Failure, Success, Try}

object ZIOErrors extends ZIOAppDefault {

  val aFailedZio = ZIO.fail("Something went wrong")
  val failedWithThrowable = ZIO.fail(new RuntimeException("Boom !"))
  val failedWithDescription = failedWithThrowable.mapError(_.getMessage)

  // Attempt: run an effect that might throw an exception
  val badZio = ZIO.succeed {
    println("Trying something")
    val string: String = null
    string.length
  } // Bad

  val anAttempt: Task[Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  // Effectfully catch an error
  val catchError = anAttempt.catchAll(e => ZIO.attempt(s"Returning a different value because $e"))
  val catchSelectiveError = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exceptions: $e")
    case _ => ZIO.succeed("Ignoring everything else")
  }

  // Chain effects
  val aBetterAttempt = anAttempt.orElse(ZIO.succeed(56))

  // Fold: handle both success and failure
  val handleBoth = anAttempt.fold(ex => s"Something bad happend: $ex", value => s"Length of the string was $value")

  // Effectful fold: foldZio
  val handleBothV2: ZIO[Any, Nothing, String] = anAttempt.foldZIO(
    ex => ZIO.succeed(s"Something bad happend: $ex"),
    value => ZIO.succeed(s"Length of the string was $value")
  )

  // Conversion between Option/Try/Either to ZIO
  // Try => ZIO
  val aTryZio: Task[Int] = ZIO.fromTry(Try(42 / 0)) // Can fail with Throwable

  // Either => ZIO
  val anEither: Either[Int, String] = Right("Success!")
  val anEitherToZio: IO[Int, String] = ZIO.fromEither(anEither)

  // ZIO => ZIO with Either as the value channel
  val eitherZIO: URIO[Any, Either[Throwable, Int]] = anAttempt.either
  val anAttemptV2: ZIO[Any, Throwable, Int] = eitherZIO.absolve

  // Option => ZIO
  val anOption: ZIO[Any, Option[Nothing], Int] = ZIO.fromOption(Some(42))

  /**
   * Exercise: implement a version of fromTry, fromEither, either, absolve using fold and foldZIO
   */

  def try2Zio[A](aTry: Try[A]): Task[A] = aTry match {
    case Failure(exception) => ZIO.fail(exception)
    case Success(value) => ZIO.succeed(value)
  }

  def either2Zio[A,B](anEither: Either[A,B]): ZIO[Any, A, B] = anEither match {
    case Left(value) => ZIO.fail(value)
    case Right(value) => ZIO.succeed(value)
  }

  def option2Zio[A](anOption: Option[A]): ZIO[Any, Option[Nothing], A] = anOption match {
    case Some(value) => ZIO.succeed(value)
    case None => ZIO.fail(None)
  }

  def zio2zioEither[R,A,B](zio: ZIO[R,A,B]): ZIO[R, Nothing, Either[A,B]] =
    zio.foldZIO(
      error => ZIO.succeed(Left(error)),
      value => ZIO.succeed(Right(value))
    )

  def absolveZio[R,A,B](zio: ZIO[R,Nothing,Either[A,B]]): ZIO[R,A,B] =
    zio.flatMap {
      case Left(error) => ZIO.fail(error)
      case Right(value) => ZIO.succeed(value)
    }

  /*
  Errors vs Defects
  Errors = failures present in the ZIO type signatures
  Defects = failures that are unrecoverable, unforeseen, not present in the ZIO type signature

  ZIO[R,E,A] can finish with Exit[E,A]
  - Success[A] containing A
  - Cause[E]
    - Fail[E] containing the error
    - Die(t: Throwable) which was unforeseen
  */

  val divisionByZero: UIO[Int] = ZIO.succeed(1 / 0)
  val failedInt: ZIO[Any, String, Int] = ZIO.fail("I failed!")
  val failureCauseExposed: ZIO[Any, Cause[String], Int] = failedInt.sandbox
  val failureCauseHidden: ZIO[Any, String, Int] = failureCauseExposed.unsandbox
  // Fold with cause
  val foldedWithCause = failedInt.foldCause(
    cause => s"This failed with ${cause.defects}",
    value => s"This succeed with ${value}"
  )

  val foldedWithCauseV2 = failedInt.foldCauseZIO(
    cause => ZIO.succeed(s"This failed with ${cause.defects}"),
    value => ZIO.succeed(s"This succeed with $value")
  )

  /*
    Good practice:
      - at a lower level, your errors should be treated
      - at a higher level, you should hide "errors" and assume they are unrecoverable
  */

  def callHTTPEndpoint(url: String): ZIO[Any, IOException, String] =
    ZIO.fail(new IOException("No internet, dummy !"))

  val endpointCallWithDefects: ZIO[Any, Nothing, String] =
    callHTTPEndpoint("rockthejvm.com").orDie // All errors are now defects

  def callHTTPEndpointWideError(url: String): ZIO[Any, Exception, String] =
    ZIO.fail(new IOException("No internet !"))

  def callHTTPEndpointV2(url: String): ZIO[Any, IOException, String] =
    callHTTPEndpointWideError(url).refineOrDie[IOException] {
      case e: IOException => e
      case _: NoRouteToHostException => new IOException(s"No route to host to $url")
    }

  // Reverse: turn defects into the error channel
  val endpointCallWithError = endpointCallWithDefects.unrefine {
    case e => e.getMessage
  }

  /*
    Combine effects with different errors
  */
  case class IndexError(message: String)
  case class DbError(message: String)
  val callApi: ZIO[Any, IndexError, String] = ZIO.succeed("Page: <html></html>")
  val queryDb: ZIO[Any, DbError, Int] = ZIO.succeed(1)
  val combined: ZIO[Any, IndexError | DbError, (String, Int)] = for {
    page <- callApi
    rowsAffected <- queryDb
  } yield (page, rowsAffected) // Lost type safety !

  /*
    Solutions:
      - design an error model
          trait AppError
          case class IndexError(message: String) extends AppError
          case class DbError(message: String) extends AppError
      - use Scala 3 union types
          val combined: ZIO[Any, IndexError | DbError, (String, Int)]
      - .mapError to some common error type
  */

  /**
   * Exercices:
   */
  // 1 - Make this effect fail with a Typed error
  val aBadFailure = ZIO.succeed[Int](throw new RuntimeException("This is bad!"))
  val aBetterFailure = aBadFailure.sandbox // Eposes the defect in the cause
  val aBetterFailureV2 = aBadFailure.unrefine { // Surfaces out the exception in the error channel
    case e => e
  }

  // 2 - Transform a ZIO into another ZIO with a narrower exception type
  def ioException[R,A](zio: ZIO[R, Throwable, A]): ZIO[R, IOException, A] =
    zio.refineOrDie {
      case ioe: IOException => ioe
    }

  // 3
  def left[R,E,A,B](zio: ZIO[R,E,Either[A,B]]): ZIO[R, Either[E,A], B] =
    zio.foldZIO(
      e => ZIO.fail(Left(e)),
      either => either match {
        case Left(a) => ZIO.fail(Right(a))
        case Right(b) => ZIO.succeed(b)
      }
    )

  // 4
  val database = Map(
    "daniel" -> 123,
    "alice" -> 789
  )

  case class QueryError(reason: String)
  case class UserProfile(name: String, phone: Int)

  def lookupProfile(userId: String): ZIO[Any, QueryError, Option[UserProfile]] =
    if (userId != userId.toLowerCase())
      ZIO.fail(QueryError("userId format is invalid"))
    else
      ZIO.succeed(database.get(userId).map(phone => UserProfile(userId, phone)))

  // Surface out all the failed cases of API
  def betterLookupProfile(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).foldZIO(
      error => ZIO.fail(Some(error)),
      profileOption => profileOption match {
        case Some(profile) => ZIO.succeed(profile)
        case None => ZIO.fail(None)
      }
    )

  def betterLookupProfileV2(userId: String): ZIO[Any, Option[QueryError], UserProfile] =
    lookupProfile(userId).some

  override def run = ???
}
*/
