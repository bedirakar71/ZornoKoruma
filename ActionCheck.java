package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;

/** Vanilla istemcinin yapamayacağı eylemler: eşya kullanırken / sandık açıkken vurmak, geçersiz bakış. */
public final class ActionCheck extends Check {
    public ActionCheck(Settings s, ViolationSink sink) {
        super(CheckType.ACTION, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        PlayerData d = c.ad;
        boolean cancel = false;

        float p = c.pitch[0];
        float y = c.yaw[0];
        if (Float.isNaN(p) || Float.isNaN(y) || Math.abs(p) > 90.5f) {
            sink.flag(d, type, s.actionRotW, "geçersiz rotasyon pitch=" + p);
            cancel = true;
        }

        if (s.actionUsing) {
            if (c.usingItem && !c.legacy) {
                d.bUse += 1.0;
                if (d.bUse >= s.actionUseBuf) {
                    sink.flag(d, type, s.actionUseW, "eşya kullanırken vuruş (yeme/blok/yay)");
                    d.bUse = s.actionUseBuf * 0.5;
                    cancel = true;
                }
            } else {
                d.bUse = Math.max(0.0, d.bUse - 0.5);
            }
        }

        if (s.actionInv && c.containerOpen && c.sinceInvOpenMs > s.actionInvGraceMs + c.ping) {
            sink.flag(d, type, s.actionInvW, "envanter/sandık açıkken vuruş");
            cancel = true;
        }
        return cancel;
    }
}
