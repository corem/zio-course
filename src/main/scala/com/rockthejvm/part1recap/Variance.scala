package com.rockthejvm.part1recap

object Variance {
  // OOP - Substitution
  class Animal
  class Dog(name: String) extends Animal

  // Covariant
  val lassie = Dog("Lassie")
  val hachi = Dog("Hachi")
  val laika = Dog("Laika")

  val anAnimal: Animal = lassie
  val someAnimal: List[Animal] = List(lassie, hachi, laika)

  class MyList[+A]
  val myAnimalList: MyList[Animal] = new MyList[Dog]

  // Invariant
  trait Semigroup[A] {
    def combine(x: A, y: A): A
  }

  // all generics in Java
  // val aJavaList: java.util.ArrayList[Animal] = new util.ArrayList[Dog]()

  // Contravariance
  trait Vet[-A] {
    def heal(animal: A): Boolean
  }

  val myVet: Vet[Dog] = new Vet[Animal] {
    override def heal(animal: Animal): Boolean = {
      println("Here you go, you're good now...")
      true
    }
  }

  val healingLassie = myVet.heal(lassie)

  def main(array: Array[String]): Unit = {}
}
