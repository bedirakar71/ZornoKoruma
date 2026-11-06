package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/**
 * Gecikme telafili erişim kontrolü: saldırganın göz konumu adayları ile hedefin ping süresi boyunca
 * geçmiş kutuları arasındaki EN KÜÇÜK mesafe alınır (oyuncunun lehine). Yine de limit aşılıyorsa hile şüphesi.
 */
public final class ReachCheck extends Check {
    public ReachCheck(Settings s, ViolationSink sink) {
        super(CheckType.REACH, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on() || !c.geometryOk()) return false;
        if (!c.targetIsPlayer && !s.reachMobs) return false;
        PlayerData d = c.ad;

        double best = Double.MAX_VALUE;
        for (int e = 0; e < c.eyeN; e++) {
            for (int b = 0; b < c.boxN; b++) {
                double dist = Geometry.distance(c.ex[e], c.ey[e], c.ez[e],
                        c.minX[b], c.minY[b], c.minZ[b], c.maxX[b], c.maxY[b], c.maxZ[b]);
                if (dist < best) best = dist;
            }
        }

        double tol = s.reachTol + d.jitter + s.reachSpeedFactor * (c.speedA + c.speedT);
        if (c.vehicle) tol += s.vehicleExtra;
        if (c.legacy) tol += s.legacyExtra;
        if (c.sneakRecent) tol += s.sneakExtra;
        if (!c.targetIsPlayer) tol += s.reachMobExtra + c.speedT * (c.back + 1);
        double limit = c.reachMax + tol;

        boolean cancel = false;
        if (best > limit + s.reachHardMargin) {
            sink.flag(d, type, s.reachHardW, info(c, best, limit));
            d.bReach = s.reachBuf;
            cancel = s.reachCancel;
        } else if (best > limit) {
            d.bReach += 1.0;
            if (d.bReach >= s.reachBuf) sink.flag(d, type, s.reachSoftW, info(c, best, limit));
            cancel = s.reachCancel;
        } else {
            d.bReach = Math.max(0.0, d.bReach - 0.25);
        }

        // Yavaş/durağan durumda ölçüm çok kesin: 3.1-3.3 gibi küçük reach hileleri için istatistik.
        if (c.targetIsPlayer && c.speedA < s.reachStatMaxSpeed && c.speedT < s.reachStatMaxSpeed
                && !c.vehicle && !c.gliding && !c.sneakRecent) {
            if (best > c.reachMax + s.reachStatMargin + d.jitter) {
                d.bReachStat += 1.0;
                if (d.bReachStat >= s.reachStatBuf) {
                    sink.flag(d, type, s.reachStatW, "istatistik " + info(c, best, limit));
                    d.bReachStat = s.reachStatBuf * 0.5;
                }
            } else {
                d.bReachStat = Math.max(0.0, d.bReachStat - 0.35);
            }
        }
        return cancel;
    }

    private static String info(AttackContext c, double best, double limit) {
        return String.format(Locale.ROOT, "mesafe=%.2f limit=%.2f hız=%.2f/%.2f ping=%.0f",
                best, limit, c.speedA, c.speedT, c.ping);
    }
}
