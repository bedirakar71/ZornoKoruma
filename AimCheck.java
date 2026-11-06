package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/**
 * Vanilla istemci yalnızca nişan çizgisi hedefin kutusuna değiyorsa vurabilir. Burada bakış ışını ile
 * hedef kutusu arasındaki kaçırma mesafesi (blok) ölçülür; ayrıca ani 180° dönüş ve sabit dönüş örüntüleri aranır.
 */
public final class AimCheck extends Check {
    private final double[] look = new double[3];
    private final int[] boxes = new int[2];

    public AimCheck(Settings s, ViolationSink sink) {
        super(CheckType.AIM, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (!on()) return false;
        PlayerData d = c.ad;
        boolean cancel = false;

        if (c.geometryOk() && (c.targetIsPlayer || s.aimMobs)) {
            double miss = miss(c);
            if (miss > 0.0) {
                double allow = s.aimAllow + 0.5 * (c.speedA + c.speedT);
                allow += c.distToCenter * Math.tan(Math.toRadians(Math.min(c.rotSpeed, 50.0)));
                if (c.vehicle) allow += s.vehicleExtra;
                if (!c.targetIsPlayer) allow += c.speedT * (c.back + 1);
                if (miss > allow + s.aimHardMargin) {
                    sink.flag(d, type, s.aimHardW, info(c, miss, allow));
                    d.bAim = s.aimBuf;
                    cancel = true;
                } else if (miss > allow + s.aimSoftMargin) {
                    d.bAim += 1.0;
                    if (d.bAim >= s.aimBuf) {
                        sink.flag(d, type, s.aimSoftW, info(c, miss, allow));
                        cancel = true;
                    }
                } else {
                    d.bAim = Math.max(0.0, d.bAim - 0.25);
                }
            } else {
                d.bAim = Math.max(0.0, d.bAim - 0.25);
            }
        }
        if (s.aimFlip) flip(c);
        return cancel;
    }

    /** Tüm (göz, bakış, kutu) adayları içinde en küçük kaçırma; biri bile çarpıyorsa 0. */
    private double miss(AttackContext c) {
        int nb = 0;
        boxes[nb++] = 0;
        if (c.nearBox > 0) boxes[nb++] = c.nearBox;

        for (int r = 0; r < c.rotN; r++) {
            Geometry.look(c.yaw[r], c.pitch[r], look);
            for (int e = 0; e < c.eyeN; e++) {
                for (int k = 0; k < nb; k++) {
                    int b = boxes[k];
                    if (Geometry.rayBox(c.ex[e], c.ey[e], c.ez[e], look[0], look[1], look[2],
                            c.minX[b], c.minY[b], c.minZ[b], c.maxX[b], c.maxY[b], c.maxZ[b], 8.0) >= 0.0) {
                        return 0.0;
                    }
                }
            }
        }
        double best = Double.MAX_VALUE;
        double maxT = c.distToCenter + 3.0;
        for (int r = 0; r < c.rotN; r++) {
            Geometry.look(c.yaw[r], c.pitch[r], look);
            for (int e = 0; e < c.eyeN; e++) {
                for (int k = 0; k < nb; k++) {
                    int b = boxes[k];
                    double m = Geometry.rayBoxMiss(c.ex[e], c.ey[e], c.ez[e], look[0], look[1], look[2], maxT,
                            c.minX[b], c.minY[b], c.minZ[b], c.maxX[b], c.maxY[b], c.maxZ[b]);
                    if (m < best) best = m;
                }
            }
        }
        return best;
    }

    private void flip(AttackContext c) {
        PlayerData d = c.ad;
        if (c.tick - d.lastAtkTick > 12) {
            d.lastDYaw = Double.NaN;
            return;
        }
        double dYaw = Geometry.wrap(c.yaw[0] - d.lastAtkYaw);
        double dPitch = c.pitch[0] - d.lastAtkPitch;
        double ay = Math.abs(dYaw);

        boolean flipped = (ay > 150.0 && Math.abs(dPitch) < 0.3) || (ay > 140.0 && Math.abs(dPitch) > 60.0);
        if (flipped) {
            d.bFlip += 1.0;
            if (d.bFlip >= 2.0) {
                sink.flag(d, type, s.aimFlipW, String.format(Locale.ROOT, "ani yön değişimi yaw=%.1f pitch=%.1f", dYaw, dPitch));
                d.bFlip = 1.0;
            }
        } else {
            d.bFlip = Math.max(0.0, d.bFlip - 0.25);
        }

        if (ay > 60.0 && !Double.isNaN(d.lastDYaw) && Math.abs(dYaw - d.lastDYaw) < 0.001) {
            d.bRepeat += 1.0;
            if (d.bRepeat >= 2.0) {
                sink.flag(d, type, s.aimFlipW, String.format(Locale.ROOT, "birebir aynı dönüş yaw=%.3f", dYaw));
                d.bRepeat = 1.0;
            }
        } else {
            d.bRepeat = Math.max(0.0, d.bRepeat - 0.25);
        }
        d.lastDYaw = dYaw;
    }

    private static String info(AttackContext c, double miss, double allow) {
        return String.format(Locale.ROOT, "kaçırma=%.2f izin=%.2f mesafe=%.2f dönüş=%.0f°/tick",
                miss, allow, c.distToCenter, c.rotSpeed);
    }
}
