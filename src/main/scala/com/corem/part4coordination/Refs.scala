package com.corem.part4coordination

import zio.*
import com.corem.utils.debugThread

import java.util.concurrent.TimeUnit

object Refs extends ZIOAppDefault {

  val atomicMol: ZIO[Any, Nothing, Ref[Int]] = Ref.make(42)

  // Obtain a value
  val mol = atomicMol.flatMap { ref => ref.get } // Returns a UIO[Int], thread-safe getter

  // Changing
  val setMol = atomicMol.flatMap { ref => ref.set(100) } // Returns a UIO[Int], thread-safe setter

  // Get + change in ONE atomic operation
  val gsMol = atomicMol.flatMap { ref => ref.getAndSet(500) }

  // Update
  val uMol = atomicMol.flatMap { ref => ref.update(_ * 100) } // Similar to Ref.set(f(ref.get)) in one atomic action

  // Update and get in ONE atomic operation
  val updatedMolWithValue = atomicMol.flatMap { ref =>
    ref.updateAndGet(_ * 100) // returns the NEW value
    ref.getAndUpdate(_ * 100) // returns the OLD value
  }

  // Modify
  val modifiedMol: UIO[String] = atomicMol.flatMap { ref =>
    ref.modify(value => (s"My current value is $value", value * 100))
  }

  // Example: distribution work
  def demoConcurrentWorkImpure(): UIO[Unit] = {
    var count = 0

    def task(workload: String): UIO[Unit] = {
      val wordCount = workload.split(" ").length
      for {
        _ <- ZIO.succeed(s"Counting words for : $workload : $wordCount").debugThread
        newCount <- ZIO.succeed(count + wordCount)
        _ <- ZIO.succeed(s"New total: $newCount").debugThread
        _ <- ZIO.succeed(count += wordCount) // Updating the variable
      } yield ()
    }

    val effects = List("I love ZIO", "This Ref thing is cool", "Daniel writes a lot of code!").map(task)
    ZIO.collectAllParDiscard(effects)
  }

  /*
    - Not thread safe !
    - Hard to debug in case of failure
    - Mixing pure and impure code
  */

  def demoConcurrentWorkPure(): UIO[Unit] = {
    def task(workload: String, total: Ref[Int]): UIO[Unit] = {
      val wordCount = workload.split(" ").length
      for {
        _ <- ZIO.succeed(s"Counting words for : $workload : $wordCount").debugThread
        newCount <- total.updateAndGet(_ + wordCount)
        _ <- ZIO.succeed(s"New total: $newCount").debugThread
      } yield ()
    }

    for {
      counter <- Ref.make(0)
      _ <- ZIO.collectAllParDiscard(
        List("I love ZIO", "This Ref thing is cool", "Daniel writes a lot of code!")
          .map(load => task(load, counter))
      )
    } yield ()
  }

  /**
   * Exercises
   */

  // 1. Refactor the code using a Ref
  def tickingClockImpure(): UIO[Unit] = {
    var ticks = 0L
    // print the current time every 1s + increase a counter ("ticks")
    def tickingClock: UIO[Unit] = for {
      _ <- ZIO.sleep(1.second)
      _ <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
      _ <- ZIO.succeed(ticks += 1)
      _ <- tickingClock
    } yield ()

    // print the total ticks count every 5s
    def printTicks: UIO[Unit] = for {
      _ <- ZIO.sleep(5.seconds)
      _ <- ZIO.succeed(s"Ticks: $ticks").debugThread
      _ <- printTicks
    } yield ()

    (tickingClock zipPar printTicks).unit
  }

  def tickingClockPure(): UIO[Unit] = {
    // print the current time every 1s + increase a counter ("ticks")
    def tickingClock(ticks: Ref[Long]): UIO[Unit] = for {
      _ <- ZIO.sleep(1.second)
      _ <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
      _ <- ticks.update(_ + 1)
      _ <- tickingClock(ticks)
    } yield ()

    // print the total ticks count every 5s
    def printTicks(ticks: Ref[Long]): UIO[Unit] = for {
      _ <- ZIO.sleep(5.seconds)
      t <- ticks.get
      _ <- ZIO.succeed(s"Ticks: $t").debugThread
      _ <- printTicks(ticks)
    } yield ()

    for {
      ticks <- Ref.make(0L)
      _ <- tickingClock(ticks) zipPar printTicks(ticks)
    } yield ()
  }

  // 2.
  def tickingClockPureV2(): UIO[Unit] = {
    val ticksRef: UIO[Ref[Long]] = Ref.make(0L)
    // print the current time every 1s + increase a counter ("ticks")
    def tickingClock: UIO[Unit] = for {
      ticks <- ticksRef
      _ <- ZIO.sleep(1.second)
      _ <- Clock.currentTime(TimeUnit.MILLISECONDS).debugThread
      _ <- ticks.update(_ + 1)
      _ <- tickingClock
    } yield ()

    // print the total ticks count every 5s
    def printTicks: UIO[Unit] = for {
      ticks <- ticksRef
      _ <- ZIO.sleep(5.seconds)
      t <- ticks.get
      _ <- ZIO.succeed(s"Ticks: $t").debugThread
      _ <- printTicks
    } yield ()

    for {
      ticks <- Ref.make(0L)
      _ <- tickingClock zipPar printTicks
    } yield ()
  }

  // Update function may be run more than once
  def demoMultipleUpdates: UIO[Unit] = {
    def task(id: Int, ref: Ref[Int]): UIO[Unit] =
      ref.modify(previous => (println(s"Task $id updating ref at $previous"), id))

    for {
      ref <- Ref.make(0)
      _ <- ZIO.collectAllParDiscard((1 to 10).toList.map(i => task(i, ref)))
    } yield ()
  }

  def run = demoMultipleUpdates
}
