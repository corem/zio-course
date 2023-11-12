package com.corem.part4coordination

import zio._
import zio.stm._
import com.corem.utils._

object Transactional extends ZIOAppDefault {

  // STM = Atomic effect
  val anSTM: ZSTM[Any, Nothing, Int] = STM.succeed(42)
  val aFailedSTM = STM.fail("something bad")
  val anAttemptSTM: ZSTM[Any, Throwable, Int] = STM.attempt(42 / 0)

  // Type aliases
  val ustm: USTM[Int] = STM.succeed(2)
  val anSTMV2: STM[Nothing, Int] = STM.succeed(42)

  // STM vs ZIO
  // Compose STMs to obtain other STMs
  // Evaluation is FULLY ATOMIC
  // "C"
  val anAtomicEffect: ZIO[Any, Throwable, Int] = anAttemptSTM.commit

  def transferMoney(sender: Ref[Long], receiver: Ref[Long], amount: Long): ZIO[Any, String, Long] =
    for {
      senderBalance <- sender.get
      _ <- if (senderBalance < amount) ZIO.fail("Transfer failed: insufficient funds...")
           else ZIO.unit
      _ <- sender.update(_ - amount)
      _ <- receiver.update(_ + amount)
      newBalance <- sender.get
    } yield newBalance

  def exploitBuggyBank() = for {
    sender <- Ref.make(1000L)
    receiver <- Ref.make(0L)
    fib1 <- transferMoney(sender, receiver, 1000).fork
    fib2 <- transferMoney(sender, receiver, 1000).fork
    _ <- (fib1 zip fib2).join
    _ <- receiver.get.debugThread
  } yield ()

  def loop(effect: ZIO[Any, String, Unit], i: Int): ZIO[Any, Nothing, Unit] =
    if (i > 10000)
      ZIO.unit
    else
      effect.ignore *> loop(effect, i + 1)

  // STM Implementation
  def transferMoneyTransactional(sender: TRef[Long], receiver: TRef[Long], amount: Long): STM[String, Long] =
    for {
      senderBalance <- sender.get
      _ <- if (senderBalance < amount) STM.fail("Transfer failed: insufficient funds...") else STM.unit
      _ <- sender.update(_ - amount)
      _ <- receiver.update(_ + amount)
      newBalance <- sender.get
    } yield newBalance

  def cannotExploit() = for {
    sender <- TRef.make(1000L).commit
    receiver <- TRef.make(0L).commit
    fib1 <- transferMoneyTransactional(sender, receiver, 1000).commit.fork
    fib2 <- transferMoneyTransactional(sender, receiver, 1000).commit.fork
    _ <- (fib1 zip fib2).join
    _ <- receiver.get.commit.debugThread
  } yield ()


  // STM data structures
  // Atomic variable: TRef
  // Same API: get, set, update, modify
  val aVariable: USTM[TRef[Int]] = TRef.make(42)

  // TArray
  val specifiedValuesTArray: USTM[TArray[Int]] = TArray.make(1, 2, 3)
  val iterableArray: USTM[TArray[Int]] = TArray.fromIterable(List(1, 2, 3, 4, 5))
  // get/apply
  val tArrayGetElem: USTM[Int] = for {
    tArray <- iterableArray
    elem <- tArray(2)
  } yield elem
  // update
  val tArrayUpdateElem: USTM[TArray[Int]] = for {
    tArray <- iterableArray
    _ <- tArray.update(1, el => el + 10)
  } yield tArray
  // transform
  val transformedArray: USTM[TArray[Int]] = for {
    tArray <- iterableArray
    _ <- tArray.transform(_ * 10)
  } yield tArray
  // fold/foldSTM, foreach

  // TSet
  // create
  val specificValuesTSet: USTM[TSet[Int]] = TSet.make(1, 2, 3, 4, 5, 1, 2, 3)
  // contains
  val tSetContainsElem: USTM[Boolean] = for {
    tSet <- specificValuesTSet
    res <- tSet.contains(3)
  } yield res
  // put
  val putElem: USTM[TSet[Int]] = for {
    tSet <- specificValuesTSet
    _ <- tSet.put(7)
  } yield tSet
  // delete
  val deleteElem: USTM[TSet[Int]] = for {
    tSet <- specificValuesTSet
    _ <- tSet.delete(1)
  } yield tSet
  // union, intersect, diff
  // removeIf, retainIf
  // transform, fold + STM versions

  // TMap
  val aTMapEffect: USTM[TMap[String, Int]] = TMap.make(("Daniel", 123), ("Alice", 456), ("QE2", 999))
  // put
  val putElemTMap: USTM[TMap[String, Int]] = for {
    tMap <- aTMapEffect
    _ <- tMap.put("Master Obi-Wan", 111)
  } yield tMap
  // get
  val getElemTMap: USTM[Option[Int]] = for {
    tMap <- aTMapEffect
    elem <- tMap.get("Daniel")
  } yield elem
  // delete, removeIf, retainIf
  // transform + STM
  // fold + STM
  // foreach
  // keys, values

  // TQueue
  val tQueueBounded: USTM[TQueue[Int]] = TQueue.bounded[Int](5)
  // offer/offerAll
  val demoOffer: USTM[TQueue[Int]] =
    for {
      tQueue <- tQueueBounded
      _ <- tQueue.offerAll(List(1, 2, 3, 4, 5, 6))
    } yield tQueue
  // take/takeAll
  val demoTakeAll: USTM[Chunk[Int]] = for {
    tQueue <- demoOffer
    elems <- tQueue.takeAll
  } yield elems
  // takeOption, peek
  // toList, toVector
  // size

  // TPriorityQueue
  val maxQueue: USTM[TPriorityQueue[Int]] = TPriorityQueue.make(3, 4, 1, 2, 5)

  // Concurrent coordination
  // TRef
  // TPromise
  // same API
  val tPromiseEffect: USTM[TPromise[String, Int]] = TPromise.make[String, Int]
  // await, poll
  val tPromiseAwait: STM[String, Int] = for {
    p <- tPromiseEffect
    res <- p.await
  } yield res
  // succeed/fail/complete
  val demoSucceed: USTM[Unit] = for {
    p <- tPromiseEffect
    _ <- p.succeed(100)
  } yield ()

  // TSemaphore
  val tSemaphoreEffect: USTM[TSemaphore] = TSemaphore.make(10)
  // acquire + acquireN
  val semaphoreAcq: USTM[Unit] = for {
    sem <- tSemaphoreEffect
    _ <- sem.acquire
  } yield ()
  // release + releaseN
  val semaphoreRel: USTM[Unit] = for {
    sem <- tSemaphoreEffect
    _ <- sem.release
  } yield ()
  // withPermit
  val semWithPermit: UIO[Int] = tSemaphoreEffect.commit.flatMap { sem =>
    sem.withPermit {
      ZIO.succeed(42)
    }
  }

  // TReentrantLock - can acquire the same lock multiple times without deadlock
  // readers-writers problem
  // has two locks: read lock (lower priority) and write lock (higher priority)
  val reentrantLockEffect = TReentrantLock.make
  val demoReentrantLock = for {
    lock <- reentrantLockEffect
    _ <- lock.acquireRead // Acquires the lock
    _ <- STM.succeed(100) // Critical section, only those that acquire read lock can access it
    rl <- lock.readLocked // Status of the lock, whether is read-locked (true in this example)
    wl <- lock.writeLocked // Same for writer
  } yield ()

  def demoReadersWriters(): UIO[Unit] = {
    def read(i: Int, lock: TReentrantLock): UIO[Unit] = for {
      _ <- lock.acquireRead.commit
      // critical region
      _ <- ZIO.succeed(s"[Task $i] Taken the read lock, reading...").debugThread
      time <- Random.nextIntBounded(1000)
      _ <- ZIO.sleep(time.millis)
      res <- Random.nextIntBounded(100) // actual computation
      _ <- ZIO.succeed(s"[Task $i] read value: $res").debugThread
      // critical region end
      _ <- lock.releaseRead.commit
    } yield ()

    def write(lock: TReentrantLock): UIO[Unit] = for {
      // writer
      _ <- ZIO.sleep(200.millis)
      _ <- ZIO.succeed("[Writer] Trying to write...").debugThread
      _ <- lock.acquireWrite.commit
      // Start - critical region
      _ <- ZIO.succeed("[Writer] I'm able to write").debugThread
      // End - critical region
      _ <- lock.releaseWrite.commit
    } yield ()

    for {
      lock <- TReentrantLock.make.commit
      readersFib <- ZIO.collectAllParDiscard((1 to 10).map(read(_, lock))).fork
      writerFib <- write(lock).fork
      _ <- readersFib.join
      _ <- writerFib.join
    } yield ()
  }

  def run = demoReadersWriters()

}
