package com.corem.part3concurrency

import zio._
import com.corem.utils._

object MasterringInterruption extends ZIOAppDefault {

  // Interruptions:
  // fib.interrupt
  // ZIO.race, ZIO.zipPar, ZIO.collectAllPAr
  // outilving parent fiber

  // Manuel interruption
  val aManuellyInterruptedZIO = ZIO.succeed("Computing...").debugThread *> ZIO.interrupt *> ZIO.succeed(42).debugThread

  // Finalizer
  val effectWithInterruptionFinalizer = aManuellyInterruptedZIO.onInterrupt(ZIO.succeed("I was interrupted").debugThread)

  // Uninterruptible
  // Payment flow to NOT be interrupted
  val fussyPaymentSystem = (
    ZIO.succeed("Payment running, don't cancel me...").debugThread *>
    ZIO.sleep(1.second) *> // The actual payment
    ZIO.succeed("Payment completed").debugThread
  ).onInterrupt(ZIO.succeed("MEGA CANCEL OF DOOM!").debugThread) // Don't want this triggered

  val cancellationOfDoom = for {
    fib <- fussyPaymentSystem.fork
    _ <- ZIO.sleep(500.millis) *> fib.interrupt
    _ <- fib.join
  } yield ()

  // ZIO.uninterruptible
  val atomicPayment = ZIO.uninterruptible(fussyPaymentSystem) // Make a ZIO atomic
  val atomicPaymentV2 = fussyPaymentSystem.uninterruptible // Same

  val noCancellationOfDoom = for {
    fib <- atomicPayment.fork
    _ <- ZIO.sleep(500.millis) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val zio1 = ZIO.succeed(1)
  val zio2 = ZIO.succeed(2)
  val zio3 = ZIO.succeed(3)
  val zioComposed = (zio1 *> zio2 *> zio3).uninterruptible // All the ZIOs are uninterruptible
  // Inner scopes override outer scopes zio1 and zio3 are uninterruptible but zio2 override with interruptible
  val zioComposed2 = (zio1 *> zio2.interruptible *> zio3).uninterruptible

  // UninterruptibleMask
  /* Example: an authentication service
     - input password, can be interrupted, because otherwise it might block the fiber indefinitely
     - verify password, which cannot be interrupted once it's triggered
  */
  val inputPassword = for {
    _ <- ZIO.succeed("Input password").debugThread
    _ <- ZIO.succeed("(Typing password)").debugThread
    _ <- ZIO.sleep(2.seconds)
    pass <- ZIO.succeed("RockTheJVM!")
  } yield pass

  val verifyPassword = (pw: String) => for {
    _ <- ZIO.succeed("Verifying...").debugThread
    _ <- ZIO.sleep(2.seconds)
    result <- ZIO.succeed(pw == "RockTheJVM!")
  } yield result

  val authFlow = ZIO.uninterruptibleMask { restore =>
    // EVERYTHING is uninterruptible... except "restore"
    for {
      pw <- restore(inputPassword).onInterrupt(ZIO.succeed("Authentication timed out. Try again later.").debugThread)
      verification <- verifyPassword(pw)
      _ <- if (verification) ZIO.succeed("Authentication successful.").debugThread
           else ZIO.succeed("Authentication failed").debugThread
    } yield ()
  }

  val authProgram = for {
    authFib <- authFlow.fork
    _ <- ZIO.sleep(3.seconds) *> ZIO.succeed("Attempting to cancel authentication...").debugThread *> authFib.interrupt
    _ <- authFib.join
  } yield ()

  /**
   * Exercices
   */
  // 1. What will these effects do ?
  val cancelBeforeMol = ZIO.interrupt *> ZIO.succeed(42).debugThread
  val uncancelBeforeMol = ZIO.uninterruptible(ZIO.interrupt *> ZIO.succeed(42).debugThread)
  // Neither will print anything

  // 2.
  val authProgramV2 = for {
    authFib <- ZIO.uninterruptibleMask(_ => authFlow).fork
    _ <- ZIO.sleep(3.seconds) *> ZIO.succeed("Attempting to cancel authentication...").debugThread *> authFib.interrupt
    _ <- authFib.join
  } yield ()

  /*
    Uninterruptible calls are masks which suppress cancellation.
  */

  // 3.
  val threeStepProgram = {
    val sequence = ZIO.uninterruptibleMask { restore =>
      for {
        _ <- restore(ZIO.succeed("Interruptible 1").debugThread *> ZIO.sleep(1.second))
        _ <- ZIO.succeed("Uninterruptible").debugThread *> ZIO.sleep(1.second)
        _ <- restore(ZIO.succeed("Interruptible 2").debugThread *> ZIO.sleep(1.second))
      } yield ()
    }

    for {
      fib <- sequence.fork
      _ <- ZIO.sleep(1500.millis) *> ZIO.succeed("Interrupting!").debugThread *> fib.interrupt
      _ <- fib.join
    } yield ()
  }

  def run = uncancelBeforeMol
}
