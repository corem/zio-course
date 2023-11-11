package com.corem.part4coordination

import zio.*
import com.corem.utils.*

import scala.sys.process.ProcessIO

object Promises extends ZIOAppDefault {
  val aPromise: ZIO[Any, Nothing, Promise[Throwable, Int]] = Promise.make[Throwable, Int]

  // Await - Block the fiber until the promise has a value
  val reader = aPromise.flatMap { promise =>
    promise.await
  }

  // Succeed, fail, complete
  val write = aPromise.flatMap { promise =>
    promise.succeed(42)
  }

  def demoPromise(): Task[Unit] = {
    // Producer - consumer problem
    def consumer(promise: Promise[Throwable, Int]): Task[Unit] = for {
      _ <- ZIO.succeed("[Consumer] Waiting for result...").debugThread
      mol <- promise.await
      _ <- ZIO.succeed(s"[Consumer] I got the result: $mol").debugThread
    } yield ()

    def producer(promise: Promise[Throwable, Int]): Task[Unit] = for {
      _ <- ZIO.succeed("[Producer] Crunching numbers...").debugThread
      _ <- ZIO.sleep(3.seconds)
      _ <- ZIO.succeed("[Producer] Complete")
      mol <- ZIO.succeed(42)
      _ <- promise.succeed(mol)
    } yield ()

    for {
      promise <- Promise.make[Throwable, Int]
      _ <- consumer(promise) zipPar producer(promise)
    } yield ()
  }

  /*
    - Purely functional block on a fiber until you get a signal from another fiber
    - Waiting on a value which may not yet be available, without thread starvation
    - Inter-fiber communication
  */
  val fileParts = List("I ", "love S", "cala", " with pure FP an", "d ZIO! <EOF>")
  def downloadFileWithRefPromise(): Task[Unit] = {

    def downloadFile(contentRef: Ref[String], promise: Promise[Throwable, String]): Task[Unit] =
      ZIO.collectAllDiscard(
        fileParts.map { part =>
          for {
            _ <- ZIO.succeed(s"Got '$part'").debugThread
            _ <- ZIO.sleep(1.second)
            file <- contentRef.updateAndGet(_ + part)
            _ <- if (file.endsWith("<EOF>")) promise.succeed(file) else ZIO.unit
          } yield ()
        }
      )

    def notifyFileComplete(contentRef: Ref[String], promise: Promise[Throwable, String]): Task[Unit] = for {
      _ <- ZIO.succeed("Downloading...").debugThread
      file <- promise.await
      _ <- ZIO.succeed(s"File download complete: $file").debugThread
    } yield ()

    for {
      contentRef <- Ref.make("")
      promise <- Promise.make[Throwable, String]
      _ <- downloadFile(contentRef, promise) zipPar notifyFileComplete(contentRef, promise)
    } yield ()
  }

  /**
   * Exercices
   * 1. Write a simulated egg boiler with two ZIOs
   *  - one increments a counter every 1s
   *  - one waits for the counter to become 10, after which it will ring a bell
   *
   * 2. Write a race pair
   */

  def eggBoiler(): UIO[Unit] = {
    def eggReady(signal: Promise[Nothing, Unit]) = for {
      _ <- ZIO.succeed("Egg boiling on some other fiber, waiting...").debugThread
      _ <- signal.await
      _ <- ZIO.succeed("Egg ready!").debugThread
    } yield ()

    def tickingClock(ticks: Ref[Int], signal: Promise[Nothing, Unit]): UIO[Unit] = for {
      _ <- ZIO.sleep(1.second)
      count <- ticks.updateAndGet(_ + 1)
      _ <- ZIO.succeed(count).debugThread
      _ <- if (count >= 10) signal.succeed(()) else tickingClock(ticks, signal)
    } yield ()

    for {
      ticks <- Ref.make(0)
      signal <- Promise.make[Nothing, Unit]
      _ <- eggReady(signal) zipPar tickingClock(ticks, signal)
    } yield ()
  }

  def racePair[R,E,A,B](zioa: => ZIO[R,E,A], ziob: ZIO[R,E,B]):
  ZIO[R,Nothing,Either[(Exit[E,A], Fiber[E,B]), (Fiber[E,A], Exit[E,B])]] = {
    ZIO.uninterruptibleMask { restore =>
      for {
        promise <- Promise.make[Nothing, Either[Exit[E,A], Exit[E,B]]]
        fibA <- restore(zioa).onExit(exita => promise.succeed(Left(exita))).fork
        fibB <- restore(ziob).onExit(exitb => promise.succeed(Right(exitb))).fork

        result <- restore(promise.await).onInterrupt {
          for {
            interruptA <- fibA.interrupt.fork
            interruptB <- fibB.interrupt.fork
            _ <- interruptA.join
            _ <- interruptB.join
          } yield ()
        }
      } yield result match {
        case Left(exitA) => Left((exitA, fibB))
        case Right(exitB) => Right((fibA, exitB))
      }
    }
  }

  val demoRacePair = {
    val zioa = ZIO.sleep(1.second).as(1).onInterrupt(ZIO.succeed("First interrupted").debugThread)
    val ziob = ZIO.sleep(2.second).as(2).onInterrupt(ZIO.succeed("Second interrupted").debugThread)

    val pair = racePair(zioa, ziob)

    pair.flatMap {
      case Left((exita, fibb)) => fibb.interrupt *> ZIO.succeed("First won").debugThread *> ZIO.succeed(exita).debugThread
      case Right((fiba, exitb)) => fiba.interrupt *> ZIO.succeed("Second won").debugThread *> ZIO.succeed(exitb).debugThread
    }
  }

  def run = demoRacePair
}
