package com.corem.part3concurrency

import zio.*
import com.corem.utils.*

import java.util.concurrent.atomic.AtomicBoolean

object BlockingEffects extends ZIOAppDefault {

  def blockingTask(n: Int): UIO[Int] =
    ZIO.succeed(s"Running blocking task $n").debugThread *>
      ZIO.succeed(Thread.sleep(10000)) *>
      blockingTask(n)

  val program = ZIO.foreachPar((1 to 100).toList)(blockingTask)
  // Thread starvation

  // Blocking thread pool
  val aBlockingZIO = ZIO.attemptBlocking {
    println(s"[${Thread.currentThread().getName}] running a long computation")
    Thread.sleep(10000)
    42
  }

  // Blocking code cannot (usually) be interrupted
  val tryInterrupting = for {
    blockingFib <- aBlockingZIO.fork
    _ <- ZIO.sleep(1.second) *> ZIO.succeed("Interrupting...").debugThread *> blockingFib.interrupt
    mol <- blockingFib.join
  } yield mol

  // Can use attemptBlockingInterrupt
  val aBlockingInterruptZIO = ZIO.attemptBlockingInterrupt{
    println(s"[${Thread.currentThread().getName}] running a long computation...")
    Thread.sleep(10000)
    42
  }

  // Set a flag/switch
  def interruptibleBlockingEffect(canceledFlag: AtomicBoolean): Task[Unit] =
    ZIO.attemptBlockingCancelable {
      (1 to 10000).foreach { element =>
        if(!canceledFlag.get()) {
          println(element)
          Thread.sleep(100)
        }
      }
    } (ZIO.succeed(canceledFlag.set(true))) // Cancelling/interrupting effect

  val interruptibleBlockingDemo = for {
    fib <- interruptibleBlockingEffect(new AtomicBoolean(false)).fork
    _ <- ZIO.sleep(2.seconds) *> ZIO.succeed("Interrupting...").debugThread *> fib.interrupt
    _ <- fib.join
  } yield ()

  // Semantic blocking - no blocking of threads, descheduling the effect/fiber
  val sleeping = ZIO.sleep(1.second) // Semantically blocking, interruptible
  val sleepingThread = ZIO.succeed(Thread.sleep(1000)) // Blocking, uninterruptible
  // yield
  val chainedZIO = (1 to 1000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> _.debugThread)
  val yieldingDemo = (1 to 1000).map(i => ZIO.succeed(i)).reduce(_.debugThread *> ZIO.yieldNow *> _.debugThread)

  def run = yieldingDemo
}
