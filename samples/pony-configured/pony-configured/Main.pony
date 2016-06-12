actor Main
    "The main actor of pony-configured"

    new create(env: Env) =>
        "Say hello"
        env.out.print("Hello, configured Pony!")
