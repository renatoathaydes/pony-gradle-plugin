use "ponytest"
use "../hello"

actor Main is TestList
  new create(env: Env) =>
    PonyTest(env, this)

  new make() =>
    None

  fun tag tests(test: PonyTest) =>
    test(_TestHello)

class iso _TestHello is UnitTest
  fun name(): String => "Hello world in Pony is great"

  fun apply(h: TestHelper) =>
    h.assert_eq[String](Hello(), "Hello world")
