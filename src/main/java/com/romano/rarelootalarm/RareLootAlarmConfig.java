package com.romano.rarelootalarm;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("rarelootalarm")
public interface RareLootAlarmConfig extends Config
{
    @Range(min = 1, max = 2147483647)
    @ConfigItem(keyName = "minimumValue", name = "Minimum loot value", description = "Minimum total ground-pile value required to trigger the alarm.", position = 0)
    default int minimumValue() { return 20_000_000; }

    @ConfigItem(keyName = "soundFile", name = "Sound file", description = "Full path to a WAV sound file.", position = 1)
    default String soundFile() { return ""; }

    @Range(min = 0, max = 100)
    @ConfigItem(keyName = "volume", name = "Volume", description = "Alarm volume percentage.", position = 2)
    default int volume() { return 100; }

    @Range(min = 0, max = 60)
    @ConfigItem(keyName = "cooldownSeconds", name = "Cooldown", description = "Minimum seconds between alarm sounds.", position = 3)
    default int cooldownSeconds() { return 2; }

    @ConfigItem(keyName = "enabled", name = "Enable alarm", description = "Enable or disable the rare loot alarm.", position = 4)
    default boolean enabled() { return true; }
}
