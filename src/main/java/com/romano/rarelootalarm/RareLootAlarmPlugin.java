package com.romano.rarelootalarm;

import com.google.inject.Provides;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GroundItem;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundItemDespawned;
import net.runelite.api.events.GroundItemSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Rare Loot Alarm", description = "Plays a custom sound when ground loot reaches a configurable GP value.", tags = {"loot", "ground", "item", "alarm", "sound", "rare"})
public class RareLootAlarmPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(RareLootAlarmPlugin.class);
    @Inject private Client client;
    @Inject private ItemManager itemManager;
    @Inject private RareLootAlarmConfig config;
    private final Set<GroundItem> alertedItems = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile long lastAlarmTime;
    private volatile Clip activeClip;
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    @Provides
    RareLootAlarmConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(RareLootAlarmConfig.class); }

    @Override protected void startUp() { stopping.set(false); lastAlarmTime = 0L; alertedItems.clear(); log.info("Rare Loot Alarm started. Threshold: {} GP", config.minimumValue()); }
    @Override protected void shutDown() { stopping.set(true); alertedItems.clear(); stopCurrentClip(); log.info("Rare Loot Alarm stopped."); }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) { if (event.getGameState() != GameState.LOGGED_IN) alertedItems.clear(); }

    @Subscribe
    public void onGroundItemSpawned(GroundItemSpawned event)
    {
        if (!config.enabled() || client.getGameState() != GameState.LOGGED_IN) return;
        GroundItem item = event.getGroundItem();
        if (alertedItems.contains(item)) return;
        long unitPrice = itemManager.getItemPrice(item.getId());
        if (unitPrice <= 0) return;
        long totalValue = unitPrice * (long) item.getQuantity();
        if (totalValue < config.minimumValue()) return;
        alertedItems.add(item);
        playAlarm();
        log.info("Rare ground loot detected: itemId={} quantity={} value={} GP", item.getId(), item.getQuantity(), totalValue);
    }

    @Subscribe
    public void onGroundItemDespawned(GroundItemDespawned event) { alertedItems.remove(event.getGroundItem()); }

    private void playAlarm()
    {
        long now = System.currentTimeMillis();
        if (now - lastAlarmTime < config.cooldownSeconds() * 1000L) return;
        lastAlarmTime = now;
        String path = config.soundFile() == null ? "" : config.soundFile().trim();
        if (path.isEmpty()) { ToolkitFallback.beep(); return; }
        File file = new File(path);
        if (!file.isFile()) { log.warn("Sound file does not exist: {}", file); ToolkitFallback.beep(); return; }
        Thread t = new Thread(() -> playWav(file), "rare-loot-alarm-sound");
        t.setDaemon(true);
        t.start();
    }

    private void playWav(File file)
    {
        if (stopping.get()) return;
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file))
        {
            Clip clip = AudioSystem.getClip();
            synchronized (this) { stopCurrentClip(); activeClip = clip; }
            clip.open(in);
            setVolume(clip, config.volume());
            clip.start();
            clip.addLineListener(e -> { if (!clip.isRunning()) { clip.close(); synchronized (RareLootAlarmPlugin.this) { if (activeClip == clip) activeClip = null; } } });
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e)
        {
            log.warn("Unable to play Rare Loot Alarm sound: {}", file, e);
            ToolkitFallback.beep();
        }
    }

    private void setVolume(Clip clip, int percentage)
    {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        int p = Math.max(0, Math.min(100, percentage));
        if (p == 0) { c.setValue(c.getMinimum()); return; }
        float db = (float) (20.0 * Math.log10(p / 100.0));
        c.setValue(Math.max(c.getMinimum(), Math.min(c.getMaximum(), db)));
    }

    private synchronized void stopCurrentClip()
    {
        if (activeClip != null) { activeClip.stop(); activeClip.close(); activeClip = null; }
    }

    private static final class ToolkitFallback
    {
        private static void beep() { try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Throwable ignored) {} }
    }
}
