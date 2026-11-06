package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;

/** Vanilla istemci her vuruşta kol sallar; vuruş var ama kol sallama paketi yoksa (NoSwing aura). */
public final class SwingCheck extends Check {
    public SwingCheck(Settings s, ViolationSink sink) {
        super(CheckType.SWING, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        c.ad.swingPending = true;
        c.ad.swingPendingTick = c.tick;
        return false;
    }

    @Override
    public void tick(PlayerData d, int tick, boolean lagging) {
        if (!d.swingPending || tick - d.swingPendingTick < s.swingWaitTicks) return;
        d.swingPending = false;
        if (!on() || lagging) return;
        boolean ok = d.lastSwingTick >= d.swingPendingTick - 2;
        if (ok) {
            d.bSwing = Math.max(0.0, d.bSwing - 1.0);
        } else {
            d.bSwing += 1.0;
            if (d.bSwing >= s.swingBuf) {
                sink.flag(d, type, s.swingW, "vuruş var ama kol sallama paketi yok");
                d.bSwing = s.swingBuf * 0.5;
            }
        }
    }
}
