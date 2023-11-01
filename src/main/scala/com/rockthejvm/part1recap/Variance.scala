package com.rockthejvm.part1recap

object Variance {
  // OOP - Substitution
  class Animal
  class Dog(name: String) extends Animal

  // Variance question for List

  val lassie = Dog("lassie")
  val anAnimal: Animal = lassie

  def main(array: Array[String]): Unit = {}
}
