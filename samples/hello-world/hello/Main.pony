actor Main
    new create(env: Env) =>
        env.out.print(Hello())

class Hello
    fun apply(): String => "Hello world"
