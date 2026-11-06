package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/** CPS sınırı + sabit aralıklı (otomatik tıklayıcı / aura) vuruş tespiti. */
public final class RateCheck extends Check {
    private final double[] iv = new double[40];

    public RateCheck(Settings s, ViolationSink sink) {
        super(CheckType.RATE, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        PlayerData d = c.ad;
        d.pushAttack(c.nowNanos);
        boolean cancel = false;

        int cps = d.attacksSince(c.nowNanos - 1_000_000_000L);
        if (cps > s.maxCps && !c.lagging) {
            sink.flag(d, type, s.rateW, "cps=" + cps + " limit=" + s.maxCps);
            cancel = s.rateCancel;
        }

        if (c.lagging) return cancel;
        int need = Math.min(Math.max(s.constWindow - 1, 8), 31);
        int n = d.intervals(need, iv);
        if (n >= need) {
            double sum = 0.0;
            for (int k = 0; k < n; k++) sum += iv[k];
            double mean = sum / n;
            double var = 0.0;
            for (int k = 0; k < n; k++) {
                double x = iv[k] - mean;
                var += x * x;
            }
            double std = Math.sqrt(var / n);
            // istemci tick'ine (50 ms) yuvarlanmış en sık aralığın oranı
            int best = 0;
            for (int i = 0; i < n; i++) {
                long qi = Math.round(iv[i] / 50.0);
                int cnt = 0;
                for (int j = 0; j < n; j++) {
                    if (Math.round(iv[j] / 50.0) == qi) cnt++;
                }
                if (cnt > best) best = cnt;
            }
            double ratio = best / (double) n;
            boolean sus = mean >= 30.0 && mean <= s.constMeanMaxMs && (std < s.constStdMs || ratio >= s.constModeRatio);
            if (sus) {
                d.bConst += 1.0;
                if (d.bConst >= s.constBuf) {
                    sink.flag(d, type, s.constW, String.format(Locale.ROOT,
                            "sabit aralık ort=%.0fms sapma=%.1fms oran=%.2f", mean, std, ratio));
                    d.bConst = s.constBuf * 0.5;
                }
            } else {
                d.bConst = Math.max(0.0, d.bConst - 0.5);
            }
        }
        return cancel;
    }
}
