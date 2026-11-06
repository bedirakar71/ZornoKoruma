package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/**
 * Sessiz rotasyon (silent aura) imzası: oyuncu yalnızca vuruş anında hedefe bakıyor,
 * vuruştan önce de sonra da hedeften çok uzağa bakıyor ve kamera başladığı yöne geri dönüyor.
 */
public final class SilentCheck extends Check {
    private final double[] a = new double[3];
    private final double[] b = new double[3];

    public SilentCheck(Settings s, ViolationSink sink) {
        super(CheckType.SILENT, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        PlayerData d = c.ad;
        if (c.lagging || c.inGrace || c.eyeN == 0 || c.rotN == 0 || !d.has(c.tick - 1)) return false;
        d.bSilent = Math.max(0.0, d.bSilent - 0.15);
        if (d.spPending || c.distToCenter < 1.2) return false;

        int i = PlayerData.idx(c.tick - 1);
        float y0 = d.hyaw[i], p0 = d.hpitch[i];
        double dx = c.tcx - c.ex[0], dy = c.tcy - c.ey[0], dz = c.tcz - c.ez[0];
        Geometry.look(c.yaw[0], c.pitch[0], a);
        double errAt = Geometry.angleBetween(a[0], a[1], a[2], dx, dy, dz);
        Geometry.look(y0, p0, b);
        double errBefore = Geometry.angleBetween(b[0], b[1], b[2], dx, dy, dz);
        double turnIn = Geometry.rotDistance(y0, p0, c.yaw[0], c.pitch[0]);

        if (errAt <= s.silentSmall && errBefore >= s.silentBig && turnIn >= s.silentMinTurn) {
            d.spPending = true;
            d.spTick = c.tick;
            d.spYaw0 = y0; d.spPitch0 = p0;
            d.spYawAt = c.yaw[0]; d.spPitchAt = c.pitch[0];
            d.spTurnIn = turnIn;
            d.spEx = c.ex[0]; d.spEy = c.ey[0]; d.spEz = c.ez[0];
            d.spCx = c.tcx; d.spCy = c.tcy; d.spCz = c.tcz;
        }
        return false;
    }

    @Override
    public void tick(PlayerData d, int tick, boolean lagging) {
        if (!d.spPending || tick < d.spTick + 2) return;
        d.spPending = false;
        if (!on() || lagging || !d.has(tick) || d.validFrom > d.spTick - 1) return;

        int i = PlayerData.idx(tick);
        float ya = d.hyaw[i], pa = d.hpitch[i];
        double dx = d.spCx - d.spEx, dy = d.spCy - d.spEy, dz = d.spCz - d.spEz;
        Geometry.look(ya, pa, a);
        double errAfter = Geometry.angleBetween(a[0], a[1], a[2], dx, dy, dz);
        double turnOut = Geometry.rotDistance(d.spYawAt, d.spPitchAt, ya, pa);
        double net = Geometry.rotDistance(d.spYaw0, d.spPitch0, ya, pa);

        if (errAfter >= s.silentBig && turnOut >= s.silentMinTurn && net <= s.silentNet * Math.min(d.spTurnIn, turnOut)) {
            d.bSilent += 1.0;
            if (d.bSilent >= s.silentBuf) {
                sink.flag(d, type, s.silentW, String.format(Locale.ROOT,
                        "yalnızca vuruş anında hedefe bakış: giriş %.0f° çıkış %.0f° net %.0f°", d.spTurnIn, turnOut, net));
                d.bSilent = s.silentBuf * 0.5;
            }
        }
    }
}
