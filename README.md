# Rare Loot Alarm

RuneLite plugin that plays a custom WAV sound when a newly spawned ground-item stack is worth at least the configured GP threshold.

Default threshold: **20,000,000 GP**.

The value is calculated as RuneLite's current ItemManager price × stack quantity. The plugin only observes ground-item events and plays local audio; it does not click, loot, attack, or otherwise automate the game.

## Setup

1. Install JDK 11 and IntelliJ IDEA.
2. Open this folder as a Gradle project.
3. Refresh Gradle dependencies.
4. Run the Gradle `run` task with assertions enabled.
5. Log into the development RuneLite client.
6. Enable **Rare Loot Alarm** in the plugin sidebar.
7. Set **Sound file** to the full path of a `.wav` file.

For RuneLite's current plugin development workflow, see the official example/plugin-hub documentation.
