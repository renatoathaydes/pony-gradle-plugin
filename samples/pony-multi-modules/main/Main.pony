use "mod1"

actor Main
    new create(env: Env) =>
        let mod1Name = Mod1("module 1").name
        env.out.print("Hello, " + mod1Name)

