package dev.zorno.guard;

import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** VL tutar, uyarı gönderir, log yazar, ceza komutlarını çalıştırır, VL'yi zamanla azaltır. */
public final class ViolationManager implements ViolationSink {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ZornoGuard plugin;
    private final Settings s;
    private final Messenger msg;
    private final FlagLogger logger;
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private long totalFlags;

    public ViolationManager(ZornoGuard plugin, Settings s, Messenger msg, FlagLogger logger) {
        this.plugin = plugin;
        this.s = s;
        this.msg = msg;
        this.logger = logger;
    }

    public long totalFlags() {
        return totalFlags;
    }

    public List<String> recentLines() {
        return new ArrayList<>(recent);
    }

    @Override
    public void flag(PlayerData d, CheckType t, double w, String detail) {
        int ti = t.ordinal();
        d.vl[ti] += w;
        d.flagsTotal++;
        totalFlags++;
        double total = d.totalVl();
        Player p = Bukkit.getPlayer(d.uuid);

        String ping = p == null ? "?" : String.valueOf(p.getPing());

        long now = System.currentTimeMillis();
        // aynı oyuncu+kontrol için log sıklığı sınırlı (VL yine de her seferinde artar)
        if (now - d.lastLogMs[ti] >= s.logCooldownMs) {
            d.lastLogMs[ti] = now;
            String where = "";
            if (p != null) {
                org.bukkit.Location l = p.getLocation();
                where = String.format(Locale.ROOT, " | %s %.1f %.1f %.1f", p.getWorld().getName(), l.getX(), l.getY(), l.getZ());
            }
            String line = String.format(Locale.ROOT, "[%s] %s VL=%.1f (toplam %.1f) ping=%s | %s%s",
                    t.id.toUpperCase(Locale.ROOT), d.name, d.vl[ti], total, ping, detail, where);
            recent.addLast(LocalTime.now().format(TIME) + " " + line);
            while (recent.size() > Math.max(10, s.logMemoryLines)) recent.pollFirst();
            logger.write(line);
            if (s.logConsole) plugin.getLogger().info(line);
        }

        if (d.vl[ti] >= s.alertMinVl && now - d.lastAlertMs[ti] >= s.alertCooldownMs) {
            d.lastAlertMs[ti] = now;
            alert(d, t, total, detail, ping);
        }
        if (s.punishEnabled) punish(d, total, t);
    }

    private void alert(PlayerData d, CheckType t, double total, String detail, String ping) {
        String[] kv = {
                "player", d.name, "check", t.display,
                "vl", fmt(d.vl[t.ordinal()]), "total", fmt(total),
                "detail", detail, "ping", ping
        };
        Component base = msg.parse(msg.raw("alert").replace("{color}", t.color), kv);
        Component hover = msg.parse(msg.raw("alert-hover").replace("{color}", t.color), kv);
        Component full = base.hoverEvent(HoverEvent.showText(hover)).clickEvent(ClickEvent.runCommand("/zg info " + d.name));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("zornoguard.alerts") && !plugin.alertsOff().contains(staff.getUniqueId())) {
                staff.sendMessage(full);
            }
        }
        if (s.alertConsole) Bukkit.getConsoleSender().sendMessage(full);
    }

    private void punish(PlayerData d, double total, CheckType t) {
        List<Settings.PunishStep> steps = s.punishSteps;
        for (int i = 0; i < steps.size() && i < 62; i++) {
            Settings.PunishStep st = steps.get(i);
            long bit = 1L << i;
            if ((d.punishedMask & bit) != 0L || total < st.vl()) continue;
            d.punishedMask |= bit;
            for (String cmd : st.commands()) {
                final String run = cmd.replace("{player}", d.name)
                        .replace("{uuid}", d.uuid.toString())
                        .replace("{check}", t.id)
                        .replace("{vl}", fmt(total));
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run));
            }
        }
    }

    /** Her aralıkta tüm VL'leri azaltır; hepsi sıfırlanınca ceza basamakları yeniden hazırlanır. */
    public void decay() {
        for (PlayerData d : plugin.allData()) {
            boolean any = false;
            for (int i = 0; i < d.vl.length; i++) {
                if (d.vl[i] > 0.0) {
                    d.vl[i] = Math.max(0.0, d.vl[i] - s.decayAmount);
                    if (d.vl[i] > 0.0) any = true;
                }
            }
            if (!any) d.punishedMask = 0L;
        }
    }

    public static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
