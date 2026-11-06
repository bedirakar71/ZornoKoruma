package dev.zorno.guard;

import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /zguard (takma adlar: /zg) - Türkçe ve İngilizce alt komutlar. */
public final class GuardCommand implements TabExecutor {
    private static final List<String> SUBS =
            List.of("yardım", "uyarılar", "bilgi", "top", "log", "sıfırla", "kontrol", "durum", "yenile");
    private static final int PAGE = 8;

    private final ZornoGuard plugin;
    private final Settings s;
    private final Messenger msg;

    public GuardCommand(ZornoGuard plugin, Settings s, Messenger msg) {
        this.plugin = plugin;
        this.s = s;
        this.msg = msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "yardım" : args[0].toLowerCase(Locale.ROOT);
        boolean isAlerts = sub.equals("uyarılar") || sub.equals("uyarilar") || sub.equals("alerts");
        String perm = isAlerts ? "zornoguard.alerts" : "zornoguard.admin";
        if (!sender.hasPermission(perm)) {
            msg.sendKey(sender, "no-permission");
            return true;
        }
        switch (sub) {
            case "yardım", "yardim", "help", "?" -> help(sender, label);
            case "uyarılar", "uyarilar", "alerts" -> alerts(sender, args);
            case "bilgi", "info" -> info(sender, args);
            case "top", "liste" -> top(sender);
            case "log" -> log(sender, args);
            case "sıfırla", "sifirla", "reset" -> reset(sender, args);
            case "kontrol", "toggle" -> toggle(sender, args);
            case "durum", "status" -> status(sender);
            case "yenile", "reload" -> reload(sender);
            default -> msg.sendKey(sender, "unknown-sub", "label", label);
        }
        return true;
    }

    // ------------------------------------------------------------------ alt komutlar

    private void help(CommandSender to, String label) {
        msg.sendKey(to, "help-header");
        line(to, label, "durum", "Eklenti durumu, kontroller ve performans");
        line(to, label, "bilgi <oyuncu>", "Oyuncunun VL ve kontrol detayları");
        line(to, label, "top", "En yüksek VL'li oyuncular");
        line(to, label, "log [oyuncu] [sayfa]", "Son bayrak kayıtları");
        line(to, label, "sıfırla <oyuncu>", "Oyuncunun VL'sini sıfırla");
        line(to, label, "kontrol <ad>", "Bir kontrolü aç/kapat");
        line(to, label, "uyarılar [aç|kapat]", "Kendi uyarılarını aç/kapat");
        line(to, label, "yenile", "config.yml dosyasını yeniden yükle");
    }

    private void line(CommandSender to, String label, String usage, String desc) {
        msg.send(to, msg.raw("help-line"), "label", label, "usage", usage, "desc", desc);
    }

    private void alerts(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            msg.sendKey(sender, "players-only");
            return;
        }
        boolean on = plugin.alertsOff().contains(p.getUniqueId());
        if (args.length >= 2) {
            String v = args[1].toLowerCase(Locale.ROOT);
            if (v.equals("aç") || v.equals("ac") || v.equals("on")) on = true;
            else if (v.equals("kapat") || v.equals("off")) on = false;
        }
        if (on) plugin.alertsOff().remove(p.getUniqueId());
        else plugin.alertsOff().add(p.getUniqueId());
        msg.sendKey(p, on ? "alerts-on" : "alerts-off");
    }

    private void info(CommandSender to, String[] a) {
        if (a.length < 2) {
            msg.sendKey(to, "usage-info");
            return;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        PlayerData d = t == null ? null : plugin.dataOrNull(t);
        if (t == null || d == null) {
            msg.sendKey(to, "player-not-found", "player", a[1]);
            return;
        }
        msg.sendKey(to, "info-header", "player", d.name, "ping", String.valueOf(t.getPing()),
                "total", ViolationManager.fmt(d.totalVl()), "flags", String.valueOf(d.flagsTotal));
        boolean any = false;
        for (CheckType ct : CheckType.values()) {
            double v = d.vl[ct.ordinal()];
            if (v <= 0.0) continue;
            any = true;
            msg.send(to, msg.raw("info-line").replace("{color}", ct.color),
                    "check", ct.display, "vl", ViolationManager.fmt(v), "bar", bar(v));
        }
        if (!any) msg.sendKey(to, "info-clean");
    }

    private static String bar(double v) {
        int n = (int) Math.max(0, Math.min(10, Math.round(v / 2.0)));
        return "█".repeat(n) + "░".repeat(10 - n);
    }

    private void top(CommandSender to) {
        List<PlayerData> list = new ArrayList<>(plugin.allData());
        list.sort((x, y) -> Double.compare(y.totalVl(), x.totalVl()));
        msg.sendKey(to, "top-header");
        int shown = 0;
        for (PlayerData d : list) {
            if (d.totalVl() <= 0.0 || shown >= 10) break;
            shown++;
            msg.sendKey(to, "top-line", "rank", String.valueOf(shown), "player", d.name,
                    "total", ViolationManager.fmt(d.totalVl()));
        }
        if (shown == 0) msg.sendKey(to, "top-empty");
    }

    private void log(CommandSender to, String[] a) {
        String filter = null;
        int page = 1;
        for (int i = 1; i < a.length; i++) {
            try {
                page = Math.max(1, Integer.parseInt(a[i]));
            } catch (NumberFormatException e) {
                filter = a[i].toLowerCase(Locale.ROOT);
            }
        }
        List<String> all = plugin.violations().recentLines();
        List<String> rows = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            String l = all.get(i);
            if (filter == null || l.toLowerCase(Locale.ROOT).contains(filter)) rows.add(l);
        }
        if (rows.isEmpty()) {
            msg.sendKey(to, "log-empty");
            return;
        }
        int pages = (rows.size() + PAGE - 1) / PAGE;
        page = Math.min(page, pages);
        msg.sendKey(to, "log-header", "page", String.valueOf(page), "pages", String.valueOf(pages),
                "count", String.valueOf(rows.size()));
        int from = (page - 1) * PAGE;
        for (int i = from; i < Math.min(rows.size(), from + PAGE); i++) {
            msg.sendKey(to, "log-line", "line", rows.get(i));
        }
    }

    private void reset(CommandSender to, String[] a) {
        if (a.length < 2) {
            msg.sendKey(to, "usage-reset");
            return;
        }
        Player t = Bukkit.getPlayerExact(a[1]);
        PlayerData d = t == null ? null : plugin.dataOrNull(t);
        if (d == null) {
            msg.sendKey(to, "player-not-found", "player", a[1]);
            return;
        }
        d.resetViolations();
        msg.sendKey(to, "reset-done", "player", d.name);
    }

    private void toggle(CommandSender to, String[] a) {
        if (a.length < 2) {
            msg.sendKey(to, "usage-toggle", "list", ids());
            return;
        }
        CheckType t = CheckType.byId(a[1]);
        if (t == null) {
            msg.sendKey(to, "unknown-check", "check", a[1], "list", ids());
            return;
        }
        boolean now = !s.enabled[t.ordinal()];
        s.enabled[t.ordinal()] = now;
        plugin.getConfig().set("checks." + t.id + ".enabled", now);
        plugin.saveConfig();
        msg.send(to, msg.raw("toggled").replace("{color}", t.color).replace("{state}", state(now)), "check", t.display);
    }

    private void status(CommandSender to) {
        msg.sendKey(to, "status-header", "version", plugin.getDescription().getVersion());
        for (CheckType t : CheckType.values()) {
            msg.send(to, msg.raw("status-check").replace("{color}", t.color).replace("{state}", state(s.on(t))),
                    "check", t.display, "id", t.id);
        }
        PerfStats ps = plugin.perf();
        msg.sendKey(to, "status-perf", "avg", String.format(Locale.ROOT, "%.1f", ps.avgMicros()),
                "max", String.format(Locale.ROOT, "%.0f", ps.maxMicros()), "count", String.valueOf(ps.count()));
        msg.sendKey(to, "status-server", "tps", String.format(Locale.ROOT, "%.1f", plugin.tps()),
                "players", String.valueOf(plugin.allData().size()), "flags", String.valueOf(plugin.violations().totalFlags()));
    }

    private void reload(CommandSender to) {
        plugin.reloadAll();
        msg.sendKey(to, "reloaded");
    }

    private String state(boolean on) {
        return msg.raw(on ? "state-on" : "state-off");
    }

    private static String ids() {
        StringBuilder sb = new StringBuilder();
        for (CheckType t : CheckType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.id);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ tab tamamlama

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return filter(SUBS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("bilgi") || sub.equals("info") || sub.equals("sıfırla") || sub.equals("sifirla")
                    || sub.equals("reset") || sub.equals("log")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filter(names, args[1]);
            }
            if (sub.equals("kontrol") || sub.equals("toggle")) {
                List<String> list = new ArrayList<>();
                for (CheckType t : CheckType.values()) list.add(t.id);
                return filter(list, args[1]);
            }
            if (sub.equals("uyarılar") || sub.equals("uyarilar") || sub.equals("alerts")) {
                return filter(List.of("aç", "kapat"), args[1]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> src, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String v : src) {
            if (v.toLowerCase(Locale.ROOT).startsWith(p)) out.add(v);
        }
        return out;
    }
}
