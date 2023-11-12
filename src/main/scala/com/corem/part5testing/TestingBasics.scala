package com.corem.part5testing

import zio._
import zio.test._
import com.corem.utils._

case class Person(name: String, age: Int) {
  def spellName: String = name.toUpperCase()
  def saySomething: UIO[String] = ZIO.succeed(s"Hi, I'm $name")
}

object MyTestSpec extends ZIOSpecDefault {
  def spec = test("First test") {
    val person = Person("Remi", 99)

    // Must return an assertion
    assert(person.spellName)(Assertion.equalTo("REMI"))
    // Same
    assertTrue(person.spellName == "REMI")
  }
}

object MyFirstEffectTestSpec extends ZIOSpecDefault {
  def spec = test("First Effect Test") {
    val person = Person("Remi", 101)
    assertZIO(person.saySomething)(Assertion.equalTo("Hi, I'm Remi"))
    assertZIO(person.saySomething)(Assertion.assertion("should be a greeting")(gr => gr == "Hi, I'm Remi"))
    // Doesn't work with assertTrue
    // Assertion examples :
    /*
      - Assertion.equalTo => tests for equality
      - Assertion.assertion => test any truth value (the most general assertion)
      - Assertion.fails/failsCause => expects the effect to fail with the EXACT failure/cause you specify
      - Assertion.dies => expects the effect to die with a Throwable, can run an assertion on that Throwable
      - Assertion.isInterrupted => validates an interruption
      - Specialized
        - isLeft/isRight for Either
        - isSome/isNone for Option
        - isSuccess/isFailure for Try
        - isEmpty/nonEmpty for iterables
        - isEmptyString/nonEmptyString/startsWithString/matchesRegex for Strings
    */
  }
}

object ASuiteSpec extends ZIOSpecDefault {
  def spec = suite("Full suite of tests")(
    // Pass multiple tests as arguments
    test("Simple test") {
      assertTrue(1 + 3 == 4)
    },
    test("A second test") {
      assertZIO(ZIO.succeed("Scala"))(Assertion.hasSizeString(Assertion.equalTo(5)) && Assertion.startsWithString("S"))
    },
    // Subsuites
    suite("A nested suite")(
      test("A nested test") {
        assert(List(1,2,3))(Assertion.isNonEmpty && Assertion.hasSameElements(List(1,2,3)))
      },
      test("Another nested test") {
        assert(List(1,2,3).headOption)(Assertion.equalTo(Some(1)))
      },
      test("A failed nested test") {
        assertTrue(1 + 1 == 100)
      }
    )
  )
}