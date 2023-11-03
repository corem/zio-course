package com.rockthejvm.part2effects

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Try

object Effects {

  // Functional Programming
  // Expressions
  def combine(a: Int, b: Int): Int = a + b

  // Local reasoning
  // Referential transparency
  val five = combine(2, 3)
  val fiveV2 = 2 + 3
  val fivev3 = 5

  // Not all expressions are RT
  val resultOfPrinting: Unit = println("Learning ZIO")
  val resultOfPrintingV2: Unit = ()

  // Changing a var
  var anInt = 0
  val changingInt: Unit = (anInt = 42) // side effect
  val changingIntV2: Unit = () // Not the same program

  // Side effects are inevitable
  /* Effect properties:
  - the type signature describes what Kind of computation it will perform
  - the type signature describes the type of Value that it will produce
  - if side effects are required, construction must be separate from the Execution
   */

  // Option is an effect
  /* Option = possibly absent values
  - the type signature describes the kind of computation = a possibly absent value
  - type signature says that the computation returns an A, if the computation does produce something
  - no side effects are needed
   */
  val anOption: Option[Int] = Option(42)

  // Future is not an effect
  /* Future
  - describes an asynchronous computation
  - produces a value of type A, if it finishes and it's successful
  - side effect are required, construction is not separate from execution
   */
  import scala.concurrent.ExecutionContext.Implicits.global
  val aFuture: Future[Int] = Future(42)

  // MyIO
  /*
  - describes a computation which might perform side effects
  - produces a value of type A if the computation is successful
   */
  case class MyIO[A](unsafeRun: () => A) {
    def map[B](f: A => B): MyIO[B] =
      MyIO(() => f(unsafeRun()))

    def flatMap[B](f: A => MyIO[B]): MyIO[B] =
      MyIO(() => f(unsafeRun()).unsafeRun())
  }

  val anIOWithSideEffects: MyIO[Int] = MyIO(() => {
    println("producing effect")
    42
  })

  /** Exercices - Create some IO
    * 1. Measure the current time of the system
    * 2. Measure the duration of a computation
    *  - use exercice 1
    *  - use map/flatMap combinations of MyIO
    *  3. Read something from the console
    *  4. Print something for the console, then read, then print a welcome message
    */

  // 1
  val currentTime: MyIO[Long] = MyIO(() => System.currentTimeMillis())

  // 2
  def measure[A](computation: MyIO[A]): MyIO[(Long, A)] = for {
    startTime <- currentTime
    result <- computation
    endTime <- currentTime
  } yield (endTime - startTime, result)

  def measureV2[A](computation: MyIO[A]): MyIO[(Long, A)] = {
    MyIO { () =>
      val startTime = System.currentTimeMillis()
      val result = computation.unsafeRun()
      val endTime = System.currentTimeMillis()
      (endTime - startTime, result)
    }
  }

  def demoMeasurement(): Unit = {
    val computation = MyIO(() => {
      println("Crunching numbers...")
      Thread.sleep(1000)
      println("Done!")
      42
    })

    println(measure(computation).unsafeRun())
    println(measureV2(computation).unsafeRun())
  }

  // 3
  val readLine: MyIO[String] = MyIO(() => StdIn.readLine())
  def putStrLn(line: String): MyIO[Unit] = MyIO(() => println(line))

  // 4
  val program = for {
    _ <- putStrLn("What's your name ?")
    name <- readLine
    _ <- putStrLn(s"Welcome $name")
  } yield ()

  // A simplified ZIO
  case class MyZIO[-R, +E, +A](unsafeRun: R => Either[E, A]) {
    def map[B](f: A => B): MyZIO[R, E, B] =
      MyZIO(r =>
        unsafeRun(r) match {
          case Left(e)  => Left(e)
          case Right(v) => Right(f(v))
        }
      )

    def flatMap[R1 <: R, E1 >: E, B](
        f: A => MyZIO[R1, E1, B]
    ): MyZIO[R1, E1, B] =
      MyZIO(r =>
        unsafeRun(r) match {
          case Left(e)  => Left(e)
          case Right(v) => f(v).unsafeRun(r)
        }
      )
  }

  def main(args: Array[String]): Unit = {
    program.unsafeRun()
  }
}
