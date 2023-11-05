package com.rockthejvm.part3concurrency

import zio._
import com.rockthejvm.utils._

object Interruptions extends ZIOAppDefault {

  val zioWithTime =
    (
      ZIO.succeed("Starting computation").debugThread *>
      ZIO.sleep(2.seconds) *>
      ZIO.succeed(42).debugThread
    )
      .onInterrupt(ZIO.succeed("I was interrupted!").debugThread)
      // onInterrupt, onDone

  val interruption = for {
    fib <- zioWithTime.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting !").debugThread *> fib.interrupt /* <- This blocks the calling fiber until the interrupted fiber is either done or interrupted */
    _ <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  val interruptionV2 = for {
    fib <- zioWithTime.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting !").debugThread *> fib.interruptFork
    _ <- ZIO.succeed("Interruption successful").debugThread
    result <- fib.join
  } yield result

  /*
    Automatic interruption
  */
  // Outliving a parent fiber
  val parentEffect =
    ZIO.succeed("Spawning fiber").debugThread *>
      // zioWithTime.fork *> // Spawning child fiber
      zioWithTime.forkDaemon *> // This fiber will now be a child of the main fiber, not the parentEffect
      ZIO.sleep(1.second) *>
      ZIO.succeed("Parent successful").debugThread // Done here

  val testOutlivingParent = for {
    parentEffectFib <- parentEffect.fork
    _ <- ZIO.sleep(3.seconds)
    _ <- parentEffectFib.join
  } yield ()
  // Child fibers will be automatically interrupted if the parent fiber is completed

  // Racing
  val slowEffect = (ZIO.sleep(2.seconds) *> ZIO.succeed("Slow").debugThread).onInterrupt(ZIO.succeed("[Slow] Interrupted").debugThread)
  val fastEffect = (ZIO.sleep(1.second) *> ZIO.succeed("Fast").debugThread).onInterrupt(ZIO.succeed("[Fast] Interrupted").debugThread)

  val aRace = slowEffect.race(fastEffect)

  val testRace = aRace.fork *> ZIO.sleep(3.seconds)

  /**
   * Exercices
   */

  // 1. Implement a timeout function
  //  - If ZIO is successful before timeout => a successful effect
  //  - If ZIO fails before timeout => a failed effect
  //  - If ZIO takes longer than timeout => interrupt the effectS
  def timeout[R,E,A](zio: ZIO[R,E,A], time: Duration): ZIO[R,E,A] =
    for {
      fib <- zio.fork
      _ <- (ZIO.sleep(time) *> fib.interrupt).fork
      result <- fib.join
    } yield result

  def testTimeout = timeout(
    ZIO.succeed("Starting...").debugThread *> ZIO.sleep(2.seconds) *> ZIO.succeed("I made it!").debugThread,
    1.second
  ).debugThread

  // 2. Timeout V2
  // - If ZIO is successful before timeout => a successful effect with Some(a)
  // - IF ZIO fails before timeout => a failed effect
  // - If ZIO takes longer than timeout => interrupt the effect, return a successful effect with None
  // hint: foldCauseZIO
  //
  def timeoutV2[R,E,A](zio: ZIO[R,E,A], time: Duration): ZIO[R,E,Option[A]] =
    timeout(zio, time).foldCauseZIO(
      cause => if (cause.isInterrupted) ZIO.succeed(None) else ZIO.failCause(cause),
      value => ZIO.succeed(Some(value))
    )

  def testTimeoutV2 = timeoutV2(
    ZIO.succeed("Starting...").debugThread *> ZIO.sleep(2.seconds) *> ZIO.succeed("I made it!").debugThread,
    1.second
  ).debugThread

  def run = testTimeoutV2

}
