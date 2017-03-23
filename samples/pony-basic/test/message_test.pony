use "ponytest"
use zmq = "zmq"
use "../basic" // our basic package

class MessageTest is UnitTest
  new iso create() => None

  fun name(): String => "zmq.Message"

  fun apply(h: TestHelper) =>
    let messenger = Messenger
    h.assert_eq[zmq.Message](
      recover zmq.Message.>push("foo") end,
      messenger.create_message("foo")
    )
