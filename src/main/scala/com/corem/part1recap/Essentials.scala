package com.corem.part1recap
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Essentials {
  // Values
  val aBoolean: Boolean = false

  // Expressions
  val anIfExpression = if (2 > 3) "bigger" else "smaller"

  // Instructions
  val theUnit = println("Hello world")

  // OO
  class Animal
  class Cat extends Animal
  class Dog extends Animal
  trait Carnivore {
    def eat(animal: Animal): Unit
  }

  // Inheritance
  class Crocodile extends Animal with Carnivore {
    override def eat(
        animal: Animal
    ): Unit = println("Crunch")
  }

  // Singleton
  object MySingleton

  // Companion
  object Carnivore

  //Generics
  class MyList[A]

  // Methods
  val three = 1 + 2
  val anotherThree = 1.+(2)

  // FP
  val incrementer: Int => Int = x => x + 1
  val incremented = incrementer(45)

  // map, flatMap, filter
  val processedList = List(1, 2, 3).map(incrementer)
  val aLongerList = List(1, 2, 3).flatMap(x => List(x, x + 1))

  // For comprehension
  val checkerBoard =
    List(1, 2, 3).flatMap(n => List('a', 'b', 'c').map(c => (n, c)))
  val anotherCheckerBoard = for {
    n <- List(1, 2, 3)
    c <- List('a', 'b', 'c')
  } yield (n, c)

  // Options and Try
  val anOption = Option(3)
  val doubledOption = anOption.map(_ * 2)

  val anAttempt = Try(42)
  val aModifiedAttempt = anAttempt.map(_ + 10)

  // Pattern matching
  val anUnkown: Any = 50
  val ordinal = anUnkown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unkown"
  }

  val optionDescription: String = anOption match {
    case Some(value) => s"The option is not empty: $value"
    case None        => "The option is empty"
  }
  // Futures
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

  val aFuture = Future {
    42
  }

  aFuture.onComplete {
    case Success(value)     => println(s"The async meaning of life is $value")
    case Failure(exception) => println(s"Meaning of value failed $exception")
  }

  val anotherFuture = aFuture.map(_ + 1)

  // Partial functions
  val aPartialFunction: PartialFunction[Int, Int] = {
    case 1   => 43
    case 8   => 56
    case 100 => 999
  }

  // Advanced stuff
  trait HigherKindedType[F[_]]
  trait SequenceChecker[F[_]] {
    def isSequential: Boolean
  }

  val listChecker = new SequenceChecker[List] {
    override def isSequential: Boolean = true
  }

  // Main
  def main(args: Array[String]): Unit = {}
}
