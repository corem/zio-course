package com.corem.part4coordination

import zio._
import com.corem.utils._

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

  def run = downloadFileWithRefPromise()
}
