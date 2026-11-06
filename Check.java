package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;

public abstract class Check {
    protected final CheckType type;
    protected final Settings s;
    protected final ViolationSink sink;

    protected Check(CheckType type, Settings s, ViolationSink sink) {
        this.type = type;
        this.s = s;
        this.sink = sink;
    }

    protected final boolean on() {
        return s.enabled[type.ordinal()];
    }

    /** Vuruş anında çalışır; true dönerse vuruş iptal edilir. */
    public boolean run(AttackContext c) {
        return false;
    }

    /** Her tick'te (örnekleyiciden) çalışır; geciken değerlendirmeler için. */
    public void tick(PlayerData d, int tick, boolean lagging) {
    }
}
