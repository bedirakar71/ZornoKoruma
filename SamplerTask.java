package dev.zorno.guard;

import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Her tick her oyuncunun konum/bakışını halkaya yazar (yeni nesne üretmeden) ve geciken kontrolleri çalıştırır. */
public final class SamplerTask implements Runnable {
    private final ZornoGuard plugin;
    private final Location tmp = new Location(null, 0, 0, 0);
    private long lastNs = System.nanoTime();
    private int sinceDecay;
    private int sinceTps;

    public SamplerTask(ZornoGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Settings s = plugin.settings();
        int tick = plugin.nextTick();

        long now = System.nanoTime();
        double dtMs = (now - lastNs) / 1.0E6;
        lastNs = now;
        if (dtMs > s.lagGuardMs) plugin.setLagUntil(tick + s.lagGuardTicks);
        boolean lagging = tick < plugin.lagUntil();

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = plugin.dataOrNull(p);
            if (d == null) continue;
            p.getLocation(tmp);
            d.record(tick, tmp.getX(), tmp.getY(), tmp.getZ(), p.getWidth(), p.getHeight(), p.getEyeHeight(),
                    tmp.getYaw(), tmp.getPitch());
            plugin.engine().onTick(d, tick, lagging);
        }

        if (++sinceDecay >= s.decayIntervalTicks) {
            sinceDecay = 0;
            plugin.violations().decay();
        }
        if (++sinceTps >= 40) {
            sinceTps = 0;
            plugin.updateTps();
        }
    }
}
