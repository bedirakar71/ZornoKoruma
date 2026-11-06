package dev.zorno.guard.core;

import dev.zorno.guard.core.check.ActionCheck;
import dev.zorno.guard.core.check.AimCheck;
import dev.zorno.guard.core.check.Check;
import dev.zorno.guard.core.check.MultiCheck;
import dev.zorno.guard.core.check.RateCheck;
import dev.zorno.guard.core.check.ReachCheck;
import dev.zorno.guard.core.check.SilentCheck;
import dev.zorno.guard.core.check.SwingCheck;
import dev.zorno.guard.core.check.WallCheck;

/** Kontrolleri sırayla çalıştırır. Ucuz olanlar (hız, eylem) önce, pahalı olan duvar kontrolü sonda. */
public final class DetectionEngine {
    private final Check[] checks;

    public DetectionEngine(Settings s, ViolationSink sink) {
        this.checks = new Check[] {
                new RateCheck(s, sink),
                new ActionCheck(s, sink),
                new ReachCheck(s, sink),
                new AimCheck(s, sink),
                new WallCheck(s, sink),
                new MultiCheck(s, sink),
                new SilentCheck(s, sink),
                new SwingCheck(s, sink)
        };
    }

    /** true dönerse vuruş iptal edilmeli. */
    public boolean onAttack(AttackContext c) {
        boolean cancel = false;
        for (Check ch : checks) {
            cancel |= ch.run(c);
        }
        remember(c);
        return cancel;
    }

    public void onTick(PlayerData d, int tick, boolean lagging) {
        for (Check ch : checks) {
            ch.tick(d, tick, lagging);
        }
    }

    private void remember(AttackContext c) {
        PlayerData d = c.ad;
        if (c.eyeN > 0) {
            double dx = c.tcx - c.ex[0], dy = c.tcy - c.ey[0], dz = c.tcz - c.ez[0];
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1.0E-6) {
                d.ldx = dx / len;
                d.ldy = dy / len;
                d.ldz = dz / len;
            }
        }
        d.lastAtkTick = c.tick;
        d.lastAtkNanos = c.nowNanos;
        d.lastTargetId = c.targetId;
        if (c.rotN > 0) {
            d.lastAtkYaw = c.yaw[0];
            d.lastAtkPitch = c.pitch[0];
        }
    }
}
