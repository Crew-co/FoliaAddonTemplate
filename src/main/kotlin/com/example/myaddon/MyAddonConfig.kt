package com.example.myaddon

import com.example.foliatemplate.addon.AddonContext
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Your addon's own config, in its private folder:
 *   plugins/FoliaTemplate/addons/MyAddon/config.yml
 *
 * Kept as an immutable snapshot: on Folia, region threads should read cached
 * values, never hit disk. Reload by constructing a new instance off-thread and
 * swapping the reference.
 */
data class MyAddonConfig(
    val greeting: String,
    val greetOnJoin: Boolean,
) {
    companion object {
        fun load(context: AddonContext): MyAddonConfig {
            val file = File(context.dataFolder, "config.yml")
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.writeText(
                    """
                    # Message used by GreetingService. {player} is replaced.
                    greeting: "<green>Hello, <white>{player}</white>!"

                    # Greet players when they join?
                    greet-on-join: true
                    """.trimIndent(),
                )
            }
            val yaml = YamlConfiguration.loadConfiguration(file)
            return MyAddonConfig(
                greeting = yaml.getString("greeting") ?: "<green>Hello, {player}!",
                greetOnJoin = yaml.getBoolean("greet-on-join", true),
            )
        }
    }
}
