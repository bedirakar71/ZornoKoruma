package dev.zorno.guard.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tüm ayarlar tek nesnede; config.yml'den SettingsLoader doldurur. */
public final class Settings {
    public record PunishStep(double vl, List<String> commands) {}

    // ---- genel
    public boolean cancelHits = true;
    public boolean randomize = true;
    public int graceTicks = 20;
    public int maxCompTicks = 10;
    public double lagGuardMs = 130.0;
    public int lagGuardTicks = 15;
    public double wallMinTps = 14.0;
    public List<String> bypassPerms = new ArrayList<>(List.of("zornoguard.bypass", "anticheat.bypass"));
    public final boolean[] enabled = new boolean[CheckType.COUNT];

    // ---- reach
    public double reachTol = 0.15, reachSpeedFactor = 0.5, reachHardMargin = 0.35;
    public double reachSoftW = 1.0, reachHardW = 2.5, reachBuf = 2.0;
    public boolean reachMobs = true, reachCancel = true;
    public double reachMobExtra = 0.45, vehicleExtra = 0.75, legacyExtra = 0.10, sneakExtra = 0.36;
    public double reachStatMargin = 0.12, reachStatBuf = 4.0, reachStatW = 1.0, reachStatMaxSpeed = 0.12;

    // ---- aim
    public double aimAllow = 0.25, aimSoftMargin = 0.30, aimHardMargin = 1.20;
    public double aimSoftW = 1.0, aimHardW = 2.5, aimBuf = 2.0;
    public boolean aimMobs = true, aimFlip = true;
    public double aimFlipW = 1.0;

    // ---- wall + inside
    public double wallW = 2.5, wallLookW = 1.0, wallLookBuf = 2.0;
    public boolean wallMobs = true, wallCancel = true;
    public double insideSolidW = 2.5, insideWebW = 0.75, insideWebBuf = 3.0;
    public boolean insideSolidCancel = true, insideWebCancel = false;

    // ---- silent (rotasyon sıçraması)
    public double silentSmall = 25.0, silentBig = 50.0, silentMinTurn = 45.0, silentNet = 0.4;
    public double silentW = 2.0, silentBuf = 2.0;

    // ---- multi
    public double multiWindowMs = 80.0, multiW = 2.0;
    public double switchMs = 110.0, switchAngle = 60.0, switchW = 1.0;
    public int switchTargets = 4;
    public double switchTargetsMs = 800.0;

    // ---- action
    public boolean actionUsing = true, actionInv = true;
    public double actionUseW = 2.0, actionUseBuf = 2.0, actionInvW = 3.0, actionRotW = 3.0;
    public double actionInvGraceMs = 400.0;

    // ---- rate
    public int maxCps = 17;
    public double rateW = 0.5;
    public boolean rateCancel = true;
    public int constWindow = 20;
    public double constMeanMaxMs = 250.0, constStdMs = 5.0, constModeRatio = 0.92, constW = 1.0, constBuf = 3.0;

    // ---- swing
    public int swingWaitTicks = 3;
    public double swingBuf = 4.0, swingW = 1.0;

    // ---- vl azalması
    public double decayAmount = 1.0;
    public int decayIntervalTicks = 60;

    // ---- uyarı / log
    public double alertMinVl = 1.0;
    public long alertCooldownMs = 500;
    public boolean alertConsole = true;
    public boolean logFile = true, logConsole = false;
    public long logCooldownMs = 500;
    public int logRetentionDays = 14;
    public int logMemoryLines = 300;

    // ---- ceza
    public boolean punishEnabled = false;
    public List<PunishStep> punishSteps = new ArrayList<>();

    // ---- mesajlar
    public String prefix = "";
    public final Map<String, String> messages = new HashMap<>();

    public Settings() {
        Arrays.fill(enabled, true);
    }

    public boolean on(CheckType t) {
        return enabled[t.ordinal()];
    }
}
