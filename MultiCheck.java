package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/** Çoklu hedef / hedef değiştirme (multi-aura, switch-aura) tespiti. */
public final class MultiCheck extends Check {
    public MultiCheck(Settings s, ViolationSink sink) {
        super(CheckType.MULTI, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        PlayerData d = c.ad;
        boolean cancel = false;

        if (d.lastTargetId != PlayerData.NONE && d.lastTargetId != c.targetId) {
            double dtMs = (c.nowNanos - d.lastAtkNanos) / 1.0E6;

            if (dtMs < s.multiWindowMs) {
                double w = d.bMulti >= 1.0 ? s.multiW : s.multiW * 0.5;
                d.bMulti += 1.0;
                sink.flag(d, type, w, String.format(Locale.ROOT, "farklı hedefe %.0f ms içinde vuruş", dtMs));
                cancel = true;
            } else {
                d.bMulti = Math.max(0.0, d.bMulti - 0.25);
            }

            if (dtMs < s.switchMs && c.eyeN > 0) {
                double dx = c.tcx - c.ex[0], dy = c.tcy - c.ey[0], dz = c.tcz - c.ez[0];
                double ang = Geometry.angleBetween(d.ldx, d.ldy, d.ldz, dx, dy, dz);
                if (ang > s.switchAngle) {
                    d.bSwitch += 1.0;
                    if (d.bSwitch >= 2.0) {
                        sink.flag(d, type, s.switchW, String.format(Locale.ROOT,
                                "hedef değiştirme %.0f° / %.0f ms", ang, dtMs));
                        d.bSwitch = 1.0;
                    }
                } else {
                    d.bSwitch = Math.max(0.0, d.bSwitch - 0.25);
                }
            }
        }

        d.pushTarget(c.targetId, c.nowNanos);
        int distinct = d.distinctTargetsSince(c.nowNanos - (long) (s.switchTargetsMs * 1.0E6));
        if (distinct >= s.switchTargets) {
            sink.flag(d, type, s.switchW, distinct + " farklı hedef " + (int) s.switchTargetsMs + " ms içinde");
            d.clearTargets();
        }
        return cancel;
    }
}
