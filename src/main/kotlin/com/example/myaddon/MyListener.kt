package com.example.myaddon

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

/**
 * A normal Bukkit listener. Register it with `context.registerListener(...)` and
 * the host unregisters it for you on unload.
 *
 * Folia: an event handler runs on the region that owns the event's player/block,
 * so touching THAT player here is already thread-safe. To reach a different
 * entity or location, schedule onto its region via context.schedulers.
 */
class MyListener(private val addon: MyAddon) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!addon.settings.greetOnJoin) return
        val message = addon.greeter()?.greetingFor(event.player.name) ?: return
        event.player.sendMessage(MiniMessage.miniMessage().deserialize(message))
    }
}
