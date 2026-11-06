package dev.zorno.guard;

import dev.zorno.guard.core.DetectionEngine;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * ZornoGuard - Kill Aura / Reach / Duvar arkası vuruş koruması (Paper 1.21.x).
 * Tüm tespit mantığı dev.zorno.guard.core altında, Bukkit'ten bağımsızdır.
 */
public final class ZornoGuard extends JavaPlugin {
    private final Settings settings = new Settings();
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final Set<UUID> alertsOff = new HashSet<>();
    private final PerfStats perf = new PerfStats();

    private Messenger msg;
    private FlagLogger flagLogger;
    private ViolationManager violations;
    private DetectionEngine engine;
    private ContextFactory factory;
    private BukkitTask sampler;
    private Attribute reachAttr;

    private int tick;
    private int lagUntil;
    private double tps = 20.0;
    private boolean degraded;

    @Override
    public void onEnable() {
        try {
            Class.forName("io.papermc.paper.event.player.PrePlayerAttackEntityEvent");
        } catch (ClassNotFoundException e) {
            getLogger().severe("ZornoGuard Paper (1.21+) gerektirir; bu sunucu yazılımında çalışmaz. Eklenti kapatılıyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        SettingsLoader.apply(getConfig(), settings);
        resolveReachAttribute();

        msg = new Messenger(settings);
        flagLogger = new FlagLogger(getDataFolder(), settings, getLogger());
        violations = new ViolationManager(this, settings, msg, flagLogger);
        engine = new DetectionEngine(settings, violations);
        factory = new ContextFactory(this, settings);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);

        GuardCommand cmd = new GuardCommand(this, settings, msg);
        PluginCommand pc = getCommand("zguard");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        sampler = getServer().getScheduler().runTaskTimer(this, new SamplerTask(this), 1L, 1L);
        for (Player p : getServer().getOnlinePlayers()) data(p);
        getLogger().info("ZornoGuard aktif: " + enabledCount() + " kontrol açık.");
    }

    @Override
    public void onDisable() {
        if (sampler != null) sampler.cancel();
        if (flagLogger != null) flagLogger.shutdown();
        data.clear();
    }

    public void reloadAll() {
        reloadConfig();
        SettingsLoader.apply(getConfig(), settings);
        resolveReachAttribute();
    }

    private int enabledCount() {
        int n = 0;
        for (boolean b : settings.enabled) if (b) n++;
        return n;
    }

    /** Etkileşim menzili özelliğini (entity_interaction_range) sürüm adından bağımsız bulur. */
    private void resolveReachAttribute() {
        reachAttr = null;
        for (String k : new String[] {"entity_interaction_range", "player.entity_interaction_range"}) {
            Attribute a = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(k));
            if (a != null) {
                reachAttr = a;
                return;
            }
        }
    }

    /** Oyuncunun gerçek vuruş menzili (eşya/efekt/yaratıcı mod çarpanları dahil). */
    public double reachOf(Player p) {
        double v = 3.0;
        if (reachAttr != null) {
            AttributeInstance ai = p.getAttribute(reachAttr);
            if (ai != null) v = ai.getValue();
        }
        if (p.getGameMode() == GameMode.CREATIVE) v = Math.max(v, 5.0);
        return v;
    }

    public boolean isExempt(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return true;
        for (String perm : settings.bypassPerms) {
            if (p.hasPermission(perm)) return true;
        }
        return false;
    }

    // ---- oyuncu verisi
    public PlayerData data(Player p) {
        PlayerData d = data.get(p.getUniqueId());
        if (d == null) {
            double jitter = settings.randomize ? (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.06 : 0.0;
            d = new PlayerData(p.getUniqueId(), p.getName(), jitter);
            d.resetHistory(tick);
            data.put(p.getUniqueId(), d);
        }
        return d;
    }

    public PlayerData dataOrNull(Player p) {
        return data.get(p.getUniqueId());
    }

    public void remove(Player p) {
        data.remove(p.getUniqueId());
    }

    public Collection<PlayerData> allData() {
        return data.values();
    }

    // ---- zaman / sunucu durumu
    public int tick() { return tick; }
    public int nextTick() { return ++tick; }
    public int lagUntil() { return lagUntil; }
    public void setLagUntil(int t) { lagUntil = t; }
    public boolean degraded() { return degraded; }
    public double tps() { return tps; }

    public void updateTps() {
        try {
            double[] t = getServer().getTPS();
            if (t != null && t.length > 0) tps = Math.min(20.0, t[0]);
        } catch (Throwable ignored) {
            // TPS okunamazsa eski değer kalır
        }
        degraded = tps < settings.wallMinTps;
    }

    // ---- erişimciler
    public Settings settings() { return settings; }
    public DetectionEngine engine() { return engine; }
    public ContextFactory factory() { return factory; }
    public ViolationManager violations() { return violations; }
    public PerfStats perf() { return perf; }
    public Set<UUID> alertsOff() { return alertsOff; }
}
