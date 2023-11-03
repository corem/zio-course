package com.rockthejvm.part2effects

import zio.*

import scala.io.StdIn

object ZIOEffect {
  // Success
  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  // Failure
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong")
  // Suspension
  val aSuspendedZio: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  // map + flatMap
  val improvedMOL = meaningOfLife.map(_ * 2)
  val printingMOL = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  // For comprehension
  val smallProgram = for {
    _ <- ZIO.succeed(println("What's your name ?"))
    name <- ZIO.succeed(StdIn.readLine())
    _ <- ZIO.succeed(println(s"Welcome $name"))
  } yield ()

  // Combinators
  // zip
  val anotherMOL = ZIO.succeed(100)
  val tupledZIO = meaningOfLife.zip(anotherMOL)
  val combinedZIO = meaningOfLife.zipWith(anotherMOL)(_ * _)

  // Type aliases of ZIOs
  // UIO[A] = ZIO[Any, Nothing, A]
  val aUIO: UIO[Int] = ZIO.succeed(42)

  // URIO[R,A] = ZIO[R, Nothing, A]
  val aURIO: URIO[Int, Int] = ZIO.succeed(34)

  // RIO[R,A] = ZIO[R, Throwable, A]
  val anRIO: RIO[Int, Int] = ZIO.succeed(65)
  val aFailedRIO: RIO[Int, Int] = ZIO.fail(new RuntimeException("Error"))

  // Task[A] = ZIO[Any, Throwable, A]
  val aSuccessfulTask: Task[Int] = ZIO.succeed(89)
  val aFailedTask: Task[Int] = ZIO.fail(new RuntimeException("Something bad"))

  // IO[E,A] = ZIO[Any, E, A]
  val aSuccessfulIO: IO[String, Int] = ZIO.succeed(34)
  val aFailedIO: IO[String, Int] = ZIO.fail("Something bad")
  def main(args: Array[String]): Unit = {

  }
}
