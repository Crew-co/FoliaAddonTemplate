package com.example.myaddon

/**
 * A service contract. Publishing this via `context.registerService<GreetingService>(impl)`
 * lets OTHER addons use it without depending on your jar:
 *
 *     val greeter = context.service<GreetingService>()
 *
 * Keep service interfaces in a stable package if third parties will use them.
 */
interface GreetingService {
    fun greetingFor(name: String): String
}

class DefaultGreetingService(private val template: String) : GreetingService {
    override fun greetingFor(name: String): String = template.replace("{player}", name)
}
