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

  // 1. Sequence two ZIOs and take the value of the last one
//  def sequenceTakeLast[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]): ZIO[R,E,B] = for {
//    _ <- zioa
//    result <- ziob
//  } yield result
//
//  def sequenceTakeLastZio[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, B] =
//    zioa *> ziob
//
//  // 2. Sequence two ZIOs and take the value of the first one
//  def sequenceTakeFirst[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]): ZIO[R,E,A] = for {
//    result <- zioa
//    _ <- ziob
//  } yield result
//
  def sequenceTakeFirstZio[R, E, A, B](zioa: ZIO[R, E, A], ziob: ZIO[R, E, B]): ZIO[R, E, A] =
    zioa <* ziob
//
//  // 3. Run a ZIO forever
//  def runForever[R,E,A](zio: ZIO[R,E,A]): ZIO[R,E,A] =
//    zio.flatMap(_ => runForever(zio))
//
//  def runForeverZio[R,E,A](zio: ZIO[R,E,A]): ZIO[R,E,A] =
//    zio *> runForeverZio(zio)
//
//  val endlessLoop = runForever {
//    ZIO.succeed {
//      println("Running...")
//      Thread.sleep(1000)
//    }
//  }
//
//  // 4. Convert the value of a ZIO to something else
//  def convert[R,E,A,B](zio: ZIO[R,E,A], value: B): ZIO[R,E,B] =
//    zio.map(_ => value)
//
//  def convertZio[R, E, A, B](zio: ZIO[R, E, A], value: B): ZIO[R, E, B] =
//    zio.as(value)
//
//  // 5. Discard the value of a ZIO to Unit
//  def asUnit[R,E,A](zio: ZIO[R,E,A]): ZIO[R,E,Unit] =
//    convert(zio, ())
//
//  def asUnitZio[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, Unit] =
//    zio.unit

  // 6. Recursion
//  def sum(n: Int): Int =
//    if (n == 0) 0
//    else n + sum(n - 1)
//
//  def sumZio(n: Int): UIO[Int] =
//    if (n == 0) ZIO.succeed(0)
//    else for {
//      current <- ZIO.succeed(n)
//      prevSum <- sumZio(n - 1)
//    } yield current + prevSum

  // 7. Fibonacci
  // Hint: ZIO.suspend / ZIO.suspendSucceed
//  def fibo(n: Int): BigInt =
//    if (n == 0) 0
//    else if (n == 1) 1
//    else fibo(n-1) + fibo(n-2)
//
//  def fiboZio(n: Int): UIO[Int] =
//    if (n == 0) ZIO.succeed(0)
//    else if (n == 1) ZIO.succeed(1)
//    else
//      for {
//        last <- ZIO.suspendSucceed(fiboZio(n - 1))
//        prev <- fiboZio(n - 2)
//      } yield last + prev


  def main(args: Array[String]): Unit = {
    val runtime = Runtime.default
    given trace: Trace = Trace.empty
    Unsafe.unsafeCompat { (u: Unsafe) =>
      given uns: Unsafe = u
      val firstEffect = ZIO.succeed {
        println("Computing first effect")
        Thread.sleep(1000)
        1
      }

      val secondEffect = ZIO.succeed {
        println("Computing second effect")
        Thread.sleep(1000)
        2
      }

      println(runtime.unsafe.run(sequenceTakeFirstZio(firstEffect, secondEffect)))
      // println(runtime.unsafe.run(fiboZio(34)))
    }
  }
}
