use zmq = "zmq"

actor Main
    new create(env: Env) =>
        env.out.print("Creating ZMQ Message")
        let messenger = Messenger
        let msg = messenger.create_message("Message in a bottle")
        env.out.print("Ready to send... " + msg.string())

class Messenger
    fun create_message(msg: String): zmq.Message =>
        recover zmq.Message.>push(msg) end