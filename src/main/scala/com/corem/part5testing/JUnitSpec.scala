package com.corem.part5testing

import zio._
import zio.test._

class JUnitSpec extends zio.test.junit.JUnitRunnableSpec {

  def spec = suite("Some test suite")(
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
        assert(List(1, 2, 3))(Assertion.isNonEmpty && Assertion.hasSameElements(List(1, 2, 3)))
      },
      test("Another nested test") {
        assert(List(1, 2, 3).headOption)(Assertion.equalTo(Some(1)))
      },
      test("A failed nested test") {
        assertTrue(1 + 1 == 100)
      }
    )
  )

}
