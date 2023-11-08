package com.corem.part3concurrency

import zio.*
import com.corem.utils.*

import java.io.File
import java.util.Scanner

object Resources extends ZIOAppDefault {
  // Finalizers
  def unsafeMethod(): Int = throw new RuntimeException("Not an int here for you !")

  val anAttempt = ZIO.attempt(unsafeMethod())

  // Finalizers can be attached to an object
  val attemptWithFinalizer = anAttempt.ensuring(ZIO.succeed("Finalizer!").debugThread)

  // Multiple finalizers
  val attemptWith2Finalizers = attemptWithFinalizer.ensuring(ZIO.succeed("another finalizer!").debugThread)

  // .onInterrupt, .onError, .onDone, .onExit

  // Resource lifecycle
  class Connection(url: String) {
    def open() = ZIO.succeed(s"Opening connection to $url...").debugThread

    def close() = ZIO.succeed(s"Closing connection to $url...").debugThread
  }

  object Connection {
    def create(url: String) = ZIO.succeed(new Connection(url))
  }

  val fetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // Resource leak

  val correctFetchUrl = for {
    conn <- Connection.create("rockthejvm.com")
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).ensuring(conn.close()).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield () // Preventing leaks but tedious

  // acquireRelease
  val cleanConnection = ZIO.acquireRelease(Connection.create("rockthejvm.com"))(_.close())
  val fetchWithResource = for {
    conn <- cleanConnection
    fib <- (conn.open() *> ZIO.sleep(300.seconds)).fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  val fetchWithScopedResource = ZIO.scoped(fetchWithResource)

  // acquireReleaseWith
  val cleanConnectionV2 = ZIO.acquireReleaseWith(
    Connection.create("rockthejvm.com")
  )(
    _.close()
  )(
    conn => conn.open() *> ZIO.sleep(300.seconds) // use effect
  )

  val fetchWithResourceV2 = for {
    fib <- cleanConnectionV2.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  /**
   * Exercices
   * 1. Use the acquireRelease to open a file, print all lines, (one every 100 millis), then close the file
   * 2.
   */

  def openFileScanner(path: String): UIO[Scanner] =
    ZIO.succeed(new Scanner(new File(path)))

  def readLineByLine(scanner: Scanner): UIO[Unit] =
    if (scanner.hasNextLine)
      ZIO.succeed(scanner.nextLine()).debugThread *> ZIO.sleep(100.millis) *> readLineByLine(scanner)
    else
      ZIO.unit

  def acquireOpenFile(path: String): UIO[Unit] =
    ZIO.succeed(s"Opening file at $path").debugThread *>
      ZIO.acquireReleaseWith(
        openFileScanner(path) // acquire
      )(
        scanner => ZIO.succeed(s"Closing file at $path").debugThread *> ZIO.succeed(scanner.close()) // close
      )(
        readLineByLine // usage
      )

  val testInterruptFileDisplay = for {
    fib <- acquireOpenFile("src/main/scala/com/corem/part3concurrency/Resources.scala").fork
    _ <- ZIO.sleep(2.seconds) *> fib.interrupt
  } yield ()

  // acquireRelease vs acquireReleaseWith
  def connFromConfig(path: String): UIO[Unit] =
    ZIO.acquireReleaseWith(openFileScanner(path))(scanner => ZIO.succeed("Closing file").debugThread *> ZIO.succeed(scanner.close())) { scanner =>
      ZIO.acquireReleaseWith(Connection.create(scanner.nextLine()))(_.close()) { conn =>
        conn.open() *> ZIO.never
      }
    }

  // nested resource
  def connFromConfigV2(path: String): UIO[Unit] = ZIO.scoped {
    for {
      scan <- ZIO.acquireRelease(openFileScanner(path))(scanner => ZIO.succeed("Closing file").debugThread *> ZIO.succeed(scanner.close()))
      conn <- ZIO.acquireRelease(Connection.create(scan.nextLine()))(_.close())
      _ <- conn.open() *> ZIO.never
    } yield ()
  }

  def run = connFromConfigV2("src/main/resources/connection.conf")
}
