package com.example.myaddon

import com.example.foliatemplate.command.Command
import com.example.foliatemplate.command.CommandContext
import com.example.foliatemplate.command.Default
import com.example.foliatemplate.command.Subcommand
import org.bukkit.Bukkit

/**
 * An addon-provided command. Registered via `context.registerCommand(this)`, it
 * behaves exactly like a host command — same annotations, same CommandContext,
 * same tab-completion.
 */
@Command(
    name = "myaddon",
    aliases = ["ma"],
    permission = "myaddon.use",
    description = "Commands from MyAddon.",
)
class MyCommand(private val addon: MyAddon) {

    @Default
    fun greet(ctx: CommandContext) {
        val greeting = addon.greeter()?.greetingFor(ctx.sender.name) ?: "Hello!"
        ctx.reply(greeting)
    }

    @Subcommand("all", description = "Greet everyone online.")
    fun all(ctx: CommandContext) {
        val greeter = addon.greeter() ?: return
        // Each player is owned by their own region thread, so message each of
        // them on THEIR region rather than from whichever thread we're on now.
        Bukkit.getOnlinePlayers().forEach { player ->
            addon.context.schedulers.entity(player) {
                player.sendMessage(
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(greeter.greetingFor(player.name)),
                )
            }
        }
        ctx.success("Greeted everyone.")
    }
}
