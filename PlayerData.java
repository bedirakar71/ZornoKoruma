package dev.zorno.guard.core;

import java.util.Arrays;
import java.util.UUID;

/** Oyuncu başına durum: konum/bakış geçmişi, ihlaller, tamponlar. Yalnızca ana thread'de kullanılır. */
public final class PlayerData {
    public static final int H = 32;
    public static final int NONE = Integer.MIN_VALUE;

    public final UUID uuid;
    public final String name;
    /** Eşiklerin tahmin edilmesini zorlaştıran oyuncuya özel küçük sapma. */
    public final double jitter;

    // ---- geçmiş (tick başına bir örnek)
    public final double[] hx = new double[H], hy = new double[H], hz = new double[H];
    public final double[] hw = new double[H], hh = new double[H], he = new double[H];
    public final float[] hyaw = new float[H], hpitch = new float[H];
    public final int[] hstamp = new int[H];
    public int validFrom = 0;

    // ---- ihlaller
    public final double[] vl = new double[CheckType.COUNT];
    public final long[] lastAlertMs = new long[CheckType.COUNT];
    public final long[] lastLogMs = new long[CheckType.COUNT];
    public int flagsTotal;
    public long punishedMask;

    // ---- tamponlar
    public double bReach, bReachStat, bAim, bFlip, bRepeat, bWallLook, bInsideWeb;
    public double bSilent, bMulti, bSwitch, bUse, bConst, bSwing;

    // ---- son vuruş
    public int lastAtkTick = -1000;
    public long lastAtkNanos;
    public int lastTargetId = NONE;
    public double ldx, ldy, ldz;
    public float lastAtkYaw, lastAtkPitch;
    public double lastDYaw = Double.NaN;

    private final long[] atkNanos = new long[32];
    private int atkHead, atkCount;
    private final int[] tgtId = new int[8];
    private final long[] tgtNanos = new long[8];
    private int tgtHead, tgtCount;

    // ---- swing
    public int lastSwingTick = -1000;
    public boolean swingPending;
    public int swingPendingTick;

    // ---- sessiz rotasyon (bekleyen sıçrama)
    public boolean spPending;
    public int spTick;
    public float spYaw0, spPitch0, spYawAt, spPitchAt;
    public double spTurnIn, spEx, spEy, spEz, spCx, spCy, spCz;

    // ---- diğer
    public int sneakToggleTick = -1000;
    public long lastInvOpenMs;

    public PlayerData(UUID uuid, String name, double jitter) {
        this.uuid = uuid;
        this.name = name;
        this.jitter = jitter;
        Arrays.fill(hstamp, -1);
    }

    public void record(int tick, double x, double y, double z, double w, double h, double eye, float yaw, float pitch) {
        int i = tick & (H - 1);
        hx[i] = x; hy[i] = y; hz[i] = z;
        hw[i] = w; hh[i] = h; he[i] = eye;
        hyaw[i] = yaw; hpitch[i] = pitch;
        hstamp[i] = tick;
    }

    public static int idx(int tick) {
        return tick & (H - 1);
    }

    public boolean has(int tick) {
        return tick >= validFrom && hstamp[tick & (H - 1)] == tick;
    }

    /** Işınlanma/dünya değişimi: önceki örnekler geçersiz. */
    public void resetHistory(int tick) {
        validFrom = tick + 1;
        spPending = false;
        swingPending = false;
    }

    public double totalVl() {
        double t = 0.0;
        for (double v : vl) t += v;
        return t;
    }

    public void resetViolations() {
        Arrays.fill(vl, 0.0);
        punishedMask = 0L;
        bReach = bReachStat = bAim = bFlip = bRepeat = bWallLook = bInsideWeb = 0.0;
        bSilent = bMulti = bSwitch = bUse = bConst = bSwing = 0.0;
    }

    // ---- vuruş zamanları
    public void pushAttack(long nanos) {
        atkNanos[atkHead] = nanos;
        atkHead = (atkHead + 1) & 31;
        if (atkCount < 32) atkCount++;
    }

    public int attacksSince(long sinceNanos) {
        int n = 0;
        for (int k = 0; k < atkCount; k++) {
            int i = (atkHead - 1 - k) & 31;
            if (atkNanos[i] >= sinceNanos) n++; else break;
        }
        return n;
    }

    /** Son n vuruş aralığını (ms) en yeniden eskiye doldurur; kaç tane bulunduğunu döndürür. */
    public int intervals(int n, double[] out) {
        int avail = Math.min(atkCount - 1, n);
        if (avail <= 0) return 0;
        int i = (atkHead - 1) & 31;
        long prev = atkNanos[i];
        for (int k = 0; k < avail; k++) {
            int j = (i - 1 - k) & 31;
            out[k] = (prev - atkNanos[j]) / 1.0E6;
            prev = atkNanos[j];
        }
        return avail;
    }

    // ---- hedef halkası
    public void pushTarget(int id, long nanos) {
        tgtId[tgtHead] = id;
        tgtNanos[tgtHead] = nanos;
        tgtHead = (tgtHead + 1) & 7;
        if (tgtCount < 8) tgtCount++;
    }

    public int distinctTargetsSince(long sinceNanos) {
        int n = 0;
        int[] seen = new int[8];
        for (int k = 0; k < tgtCount; k++) {
            int i = (tgtHead - 1 - k) & 7;
            if (tgtNanos[i] < sinceNanos) break;
            int id = tgtId[i];
            boolean dup = false;
            for (int j = 0; j < n; j++) {
                if (seen[j] == id) { dup = true; break; }
            }
            if (!dup) seen[n++] = id;
        }
        return n;
    }

    public void clearTargets() {
        tgtCount = 0;
    }
}
