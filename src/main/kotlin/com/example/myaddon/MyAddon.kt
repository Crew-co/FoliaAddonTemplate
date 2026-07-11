package com.example.myaddon

import com.example.foliatemplate.addon.AddonBase
import com.example.foliatemplate.addon.registerService
import com.example.foliatemplate.addon.service

/**
 * Your addon's entry point.
 *
 * Lifecycle: the host constructs this (no-arg constructor), injects [context],
 * then calls [onEnable]. On shutdown or `/addons reload` it calls [onDisable].
 *
 * Everything you register through `context` is tracked and cleaned up for you
 * on unload — listeners, scheduled tasks, and services. You only need
 * [onDisable] for state the host doesn't know about (open files, DB pools, ...).
 */
class MyAddon : AddonBase() {

    /** Your own config, loaded from context.dataFolder. */
    lateinit var settings: MyAddonConfig
        private set

    override fun onEnable() {
        // context.dataFolder → plugins/FoliaTemplate/addons/MyAddon/
        settings = MyAddonConfig.load(context)

        // 1. Commands — the host's @Command annotations, registered through you.
        context.registerCommand(MyCommand(this))

        // 2. Listeners — unregistered automatically on unload.
        context.registerListener(MyListener(this))

        // 3. Publish a service other addons can consume via context.service<T>().
        context.registerService<GreetingService>(DefaultGreetingService(settings.greeting))

        // 4. Scheduling — Folia-safe, and cancelled for you on unload.
        //    NEVER use Bukkit.getScheduler(); there is no main thread.
        context.schedulers.asyncRepeating(initialDelay = 60, period = 600) {
            context.logger.info("Heartbeat (async, every 10 minutes).")
        }

        context.logger.info("Enabled v${context.description.version}")
    }

    override fun onDisable() {
        context.logger.info("Disabled. Listeners and tasks were already cleaned up.")
    }

    /** Consume a service published by the host or another addon. */
    fun greeter(): GreetingService? = context.service<GreetingService>()
}
