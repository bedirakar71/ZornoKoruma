package dev.zorno.guard;

import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Settings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** config.yml -> Settings. Eksik anahtarlar varsayılan değerde kalır (eski config ile de çalışır). */
public final class SettingsLoader {
    private SettingsLoader() {}

    public static void apply(FileConfiguration c, Settings s) {
        // genel
        s.cancelHits = c.getBoolean("general.cancel-hits", s.cancelHits);
        s.randomize = c.getBoolean("general.randomize-thresholds", s.randomize);
        s.graceTicks = c.getInt("general.grace-ticks", s.graceTicks);
        s.maxCompTicks = c.getInt("general.max-compensation-ticks", s.maxCompTicks);
        s.lagGuardMs = c.getDouble("general.lag-guard-ms", s.lagGuardMs);
        s.lagGuardTicks = c.getInt("general.lag-guard-ticks", s.lagGuardTicks);
        s.wallMinTps = c.getDouble("general.wall-min-tps", s.wallMinTps);
        List<String> perms = c.getStringList("general.bypass-permissions");
        if (!perms.isEmpty()) s.bypassPerms = new ArrayList<>(perms);

        for (CheckType t : CheckType.values()) {
            s.enabled[t.ordinal()] = c.getBoolean("checks." + t.id + ".enabled", true);
        }

        // reach
        String r = "checks.reach.";
        s.reachTol = c.getDouble(r + "tolerance", s.reachTol);
        s.reachSpeedFactor = c.getDouble(r + "speed-factor", s.reachSpeedFactor);
        s.reachHardMargin = c.getDouble(r + "hard-margin", s.reachHardMargin);
        s.reachSoftW = c.getDouble(r + "soft-weight", s.reachSoftW);
        s.reachHardW = c.getDouble(r + "hard-weight", s.reachHardW);
        s.reachBuf = c.getDouble(r + "buffer", s.reachBuf);
        s.reachMobs = c.getBoolean(r + "check-mobs", s.reachMobs);
        s.reachCancel = c.getBoolean(r + "cancel", s.reachCancel);
        s.reachMobExtra = c.getDouble(r + "mob-extra", s.reachMobExtra);
        s.vehicleExtra = c.getDouble(r + "vehicle-extra", s.vehicleExtra);
        s.legacyExtra = c.getDouble(r + "legacy-extra", s.legacyExtra);
        s.sneakExtra = c.getDouble(r + "sneak-extra", s.sneakExtra);
        s.reachStatMargin = c.getDouble(r + "stat-margin", s.reachStatMargin);
        s.reachStatBuf = c.getDouble(r + "stat-buffer", s.reachStatBuf);
        s.reachStatW = c.getDouble(r + "stat-weight", s.reachStatW);
        s.reachStatMaxSpeed = c.getDouble(r + "stat-max-speed", s.reachStatMaxSpeed);

        // aim
        String a = "checks.aim.";
        s.aimAllow = c.getDouble(a + "allow", s.aimAllow);
        s.aimSoftMargin = c.getDouble(a + "soft-margin", s.aimSoftMargin);
        s.aimHardMargin = c.getDouble(a + "hard-margin", s.aimHardMargin);
        s.aimSoftW = c.getDouble(a + "soft-weight", s.aimSoftW);
        s.aimHardW = c.getDouble(a + "hard-weight", s.aimHardW);
        s.aimBuf = c.getDouble(a + "buffer", s.aimBuf);
        s.aimMobs = c.getBoolean(a + "check-mobs", s.aimMobs);
        s.aimFlip = c.getBoolean(a + "flip-detection", s.aimFlip);
        s.aimFlipW = c.getDouble(a + "flip-weight", s.aimFlipW);

        // wall + inside
        String w = "checks.wall.";
        s.wallW = c.getDouble(w + "weight", s.wallW);
        s.wallLookW = c.getDouble(w + "look-weight", s.wallLookW);
        s.wallLookBuf = c.getDouble(w + "look-buffer", s.wallLookBuf);
        s.wallMobs = c.getBoolean(w + "check-mobs", s.wallMobs);
        s.wallCancel = c.getBoolean(w + "cancel", s.wallCancel);
        String in = "checks.inside.";
        s.insideSolidW = c.getDouble(in + "solid-weight", s.insideSolidW);
        s.insideSolidCancel = c.getBoolean(in + "solid-cancel", s.insideSolidCancel);
        s.insideWebW = c.getDouble(in + "web-weight", s.insideWebW);
        s.insideWebBuf = c.getDouble(in + "web-buffer", s.insideWebBuf);
        s.insideWebCancel = c.getBoolean(in + "web-cancel", s.insideWebCancel);

        // silent
        String si = "checks.silent.";
        s.silentSmall = c.getDouble(si + "small-angle", s.silentSmall);
        s.silentBig = c.getDouble(si + "big-angle", s.silentBig);
        s.silentMinTurn = c.getDouble(si + "min-turn", s.silentMinTurn);
        s.silentNet = c.getDouble(si + "net-ratio", s.silentNet);
        s.silentW = c.getDouble(si + "weight", s.silentW);
        s.silentBuf = c.getDouble(si + "buffer", s.silentBuf);

        // multi
        String m = "checks.multi.";
        s.multiWindowMs = c.getDouble(m + "window-ms", s.multiWindowMs);
        s.multiW = c.getDouble(m + "weight", s.multiW);
        s.switchMs = c.getDouble(m + "switch-ms", s.switchMs);
        s.switchAngle = c.getDouble(m + "switch-angle", s.switchAngle);
        s.switchW = c.getDouble(m + "switch-weight", s.switchW);
        s.switchTargets = c.getInt(m + "switch-targets", s.switchTargets);
        s.switchTargetsMs = c.getDouble(m + "switch-targets-ms", s.switchTargetsMs);

        // action
        String ac = "checks.action.";
        s.actionUsing = c.getBoolean(ac + "check-using-item", s.actionUsing);
        s.actionInv = c.getBoolean(ac + "check-inventory", s.actionInv);
        s.actionUseW = c.getDouble(ac + "using-weight", s.actionUseW);
        s.actionUseBuf = c.getDouble(ac + "using-buffer", s.actionUseBuf);
        s.actionInvW = c.getDouble(ac + "inventory-weight", s.actionInvW);
        s.actionInvGraceMs = c.getDouble(ac + "inventory-grace-ms", s.actionInvGraceMs);
        s.actionRotW = c.getDouble(ac + "rotation-weight", s.actionRotW);

        // rate
        String rt = "checks.rate.";
        s.maxCps = c.getInt(rt + "max-cps", s.maxCps);
        s.rateW = c.getDouble(rt + "weight", s.rateW);
        s.rateCancel = c.getBoolean(rt + "cancel", s.rateCancel);
        s.constWindow = c.getInt(rt + "constant-window", s.constWindow);
        s.constMeanMaxMs = c.getDouble(rt + "constant-mean-max-ms", s.constMeanMaxMs);
        s.constStdMs = c.getDouble(rt + "constant-std-ms", s.constStdMs);
        s.constModeRatio = c.getDouble(rt + "constant-mode-ratio", s.constModeRatio);
        s.constW = c.getDouble(rt + "constant-weight", s.constW);
        s.constBuf = c.getDouble(rt + "constant-buffer", s.constBuf);

        // swing
        String sw = "checks.swing.";
        s.swingWaitTicks = c.getInt(sw + "wait-ticks", s.swingWaitTicks);
        s.swingBuf = c.getDouble(sw + "buffer", s.swingBuf);
        s.swingW = c.getDouble(sw + "weight", s.swingW);

        // vl azalması
        s.decayAmount = c.getDouble("decay.amount", s.decayAmount);
        s.decayIntervalTicks = Math.max(20, (int) Math.round(c.getDouble("decay.interval-seconds", 3.0) * 20.0));

        // uyarı + log
        s.alertMinVl = c.getDouble("alerts.min-vl", s.alertMinVl);
        s.alertCooldownMs = c.getLong("alerts.cooldown-ms", s.alertCooldownMs);
        s.alertConsole = c.getBoolean("alerts.console", s.alertConsole);
        s.logFile = c.getBoolean("log.file", s.logFile);
        s.logConsole = c.getBoolean("log.console", s.logConsole);
        s.logCooldownMs = c.getLong("log.cooldown-ms", s.logCooldownMs);
        s.logRetentionDays = c.getInt("log.retention-days", s.logRetentionDays);
        s.logMemoryLines = c.getInt("log.memory-lines", s.logMemoryLines);

        // ceza basamakları
        s.punishEnabled = c.getBoolean("punishments.enabled", false);
        List<Settings.PunishStep> steps = new ArrayList<>();
        for (Map<?, ?> step : c.getMapList("punishments.steps")) {
            Object vl = step.get("vl");
            Object cmds = step.get("commands");
            if (!(vl instanceof Number n)) continue;
            List<String> list = new ArrayList<>();
            if (cmds instanceof List<?> l) {
                for (Object o : l) list.add(String.valueOf(o));
            }
            steps.add(new Settings.PunishStep(n.doubleValue(), list));
        }
        steps.sort(Comparator.comparingDouble(Settings.PunishStep::vl));
        s.punishSteps = steps;

        // mesajlar
        s.messages.clear();
        ConfigurationSection ms = c.getConfigurationSection("messages");
        if (ms != null) {
            for (String k : ms.getKeys(false)) s.messages.put(k, ms.getString(k, ""));
        }
        s.prefix = s.messages.getOrDefault("prefix", "");
    }
}
