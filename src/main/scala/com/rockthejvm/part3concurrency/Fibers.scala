package com.rockthejvm.part3concurrency

import zio.*
import com.rockthejvm.utils.*

import java.io.*

object Fibers extends ZIOAppDefault {

  val meaningOfLife = ZIO.succeed(42)
  val favLang = ZIO.succeed("Scala")

  // Fiber = lightweight thread
  def createFiber: Fiber[Throwable, String] = ??? // Difficult to create manually

  def combinator = for {
    mol <- meaningOfLife.debugThread
    lang <- favLang.debugThread
  } yield (mol, lang)

  val differentThreadIO = for {
    _ <- meaningOfLife.debugThread.fork
    _ <- favLang.debugThread.fork
  } yield ()

  val meaningOfLifeFiber: ZIO[Any, Nothing, Fiber[Throwable, Int]] = meaningOfLife.fork

  // Join a fiber
  def runOnAnotherThread[R,E,A](zio: ZIO[R,E,A]) = for {
    fib <- zio.fork
    result <- fib.join
  } yield result

  // Awaiting a fiber
  def runOnAnotherThreadV2[R,E,A](zio: ZIO[R,E,A]) = for {
    fib <- zio.fork
    result <- fib.await
  } yield result match {
    case Exit.Success(value) => s"Succeeded with $value"
    case Exit.Failure(cause) => s"Failed with $cause"
  }

  // poll: Peek at the result of the fiber right now, without blocking
  val peekFiber = for {
    fib <- ZIO.attempt {
        Thread.sleep(1000)
        42
      }.fork
    result <- fib.poll
  } yield result

  // Compose fibers
  // zip
  val zippedFibers = for {
    fib1 <- ZIO.succeed("Result from fiber1").debugThread.fork
    fib2 <- ZIO.succeed("Result from fiber2").debugThread.fork
    fiber = fib1.zip(fib2)
    tuple <- fiber.join
  } yield tuple

  // orElse
  val chainedFibers = for {
    fiber1 <- ZIO.fail("Not good !").debugThread.fork
    fiber2 <- ZIO.succeed("Rock the JVM").debugThread.fork
    fiber = fiber1.orElse(fiber2)
    message <- fiber.join
  } yield message

  //def run = runOnAnotherThreadV2(meaningOfLife).debugThread


  /**
   * Exercices
   */

  // 1. Zip 2 fibers without using the .zip combinator
  // Hint: create a fiber that waits for both
  def zipFiber[E,A,B](fiber1: Fiber[E,A], fiber2:Fiber[E,B]): ZIO[Any, Nothing, Fiber[E, (A,B)]] = ???

  // 2. Same thing with orElse
  def chainFibers[E,A](fiber1: Fiber[E,A], fiber2:Fiber[E,A]): ZIO[Any, Nothing, Fiber[E, A]] = ???

  // 3. Distributing a task in between many fibers
  // Spawn n fibers, count the n of words in each file then aggregate all the results together in one number
  def generateRandomFile(path: String): Unit = {
    val random = scala.util.Random
    val chars = 'a' to 'z'
    val nWords = random.nextInt(2000)

    val content = {1 to nWords}
      .map(_ => (1 to random.nextInt(10)).map(_ => chars(random.nextInt(26))).mkString) // one word for every 1 to nWords
      .mkString(" ")

    val writer = new FileWriter(new File(path))
    writer.write(content)
    writer.flush()
    writer.close()
  }

  // Use this to generate 10 files
  //def run = ZIO.succeed((1 to 10).foreach(i => generateRandomFile(s"src/main/resources/testfile_$i.txt")))
  def run = ZIO.succeed((1 to 10).foreach(i => generateRandomFile(s"src/main/resources/testfile_$i.txt")))
}
