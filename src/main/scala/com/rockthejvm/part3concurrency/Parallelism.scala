/*
package com.rockthejvm.part3concurrency

import zio._
import com.rockthejvm.utils._

object Parallelism extends ZIOAppDefault {

  val meaningOfLife = ZIO.succeed(42)
  val favLang = ZIO.succeed("Scala")
  val combine = meaningOfLife.zip(favLang) // Combines/zips in a sequential way because based on flatMap

  // Combine in parallel
  val combinePar = meaningOfLife.zipPar(favLang) // Combination is parallel, each effect runs on its own fiber

  /*
    - Start each on fibers
    - What if one fails ? The other one should be interrupted
    - What if one is interrupted ? The entire construction should be interrupted
    - What is the whole zipPar is interrupted ? Need to interrupt both effects
  */

  // try a zipPar combinator
  // hint: fort/join/await, interrupt
  def myZipPar[R,E,A,B](zioa: ZIO[R,E,A], ziob: ZIO[R,E,B]): ZIO[R,E,(A,B)] = {
    val exits = for {
      fiba <- zioa.fork
      fibb <- ziob.fork
      exita <- fiba.await
      exitb <- exita match {
          case Exit.Success(value) => fibb.await
          case Exit.Failure(_) => fibb.interrupt
        }
      } yield (exita, exitb)

    exits.flatMap {
      case (Exit.Success(a), Exit.Success(b)) => ZIO.succeed((a,b)) // Happy path
      case (Exit.Success(_), Exit.Failure(cause)) => ZIO.failCause(cause)
      case (Exit.Failure(cause), Exit.Success(_)) => ZIO.failCause(cause)
      case (Exit.Failure(cause1), Exit.Failure(cause2)) => ZIO.failCause(cause1 && cause2)
    }
  }

  // Parallel combinators
  // zipPar, zipWithPar

  // collectAllPar
  val effects: Seq[ZIO[Any, Nothing, Int]] = (1 to 10).map(i => ZIO.succeed(i).debugThread)
  val collectedValues: ZIO[Any, Nothing, Seq[Int]] = ZIO.collectAllPar(effects) // "traverse"

  // foreachPar
  val printlnParallel = ZIO.foreachPar((1 to 10).toList)(i => ZIO.succeed(println(i)))

  // reduceAllPar, mergeAllPar
  val sumPar = ZIO.reduceAllPar(ZIO.succeed(0), effects)(_ + _)
  val sumV2 = ZIO.mergeAllPar(effects)(0)(_ + _)

  /*
    - If all the effects succeed, all good
    - One effect fails => everyone else is interrupted, error is surfaced
    - One effect is interrupted => everyone else is interrupted, error = interruption (for the big expression)
    - If the entire thing is interrupted => all effects are interrupted
  */

  /**
   * Exercise: Do the word counting exercice, using a parallel combinator
   */
  def countWords(path: String): UIO[Int] =
    ZIO.succeed {
      val source = scala.io.Source.fromFile(path)
      val nWords = source.getLines.mkString(" ").split(" ").count(_.nonEmpty)
      println(s"Counted $nWords in $path")
      source.close()
      nWords
    }

  def wordCountParallel(n: Int): UIO[Int] = {
    val effects = (1 to n)
      .map(i => s"src/main/resources/testfile_$i.txt")
      .map(path => countWords(path))

    // V1. collectAllPar
    ZIO.collectAllPar(effects).map(_.sum)

    // V2. mergeAllPar
    ZIO.mergeAllPar(effects)(0)(_ + _)

    // V3. reduceAllPar-
    ZIO.reduceAllPar(ZIO.succeed(0), effects)(_ + _)
  }

  def run = wordCountParallel(10).debugThread
}
*/
