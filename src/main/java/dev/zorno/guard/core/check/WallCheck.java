package dev.zorno.guard.core.check;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.CheckType;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import dev.zorno.guard.core.ViolationSink;
import java.util.Locale;

/**
 * Duvar arkası vuruş + ağ/blok içinden vuruş.
 * 1) Göz noktası opak bloğun / örümcek ağının içindeyse vanilla istemci bir varlığı seçemez.
 * 2) Bakış ışını hedef kutusuna girmeden önce tam küp bir bloğa çarpıyorsa vanilla istemci o bloğu seçerdi.
 * 3) Hedefin hiçbir noktası (15 örnek nokta, tüm göz/kutu adayları) görünmüyorsa tamamen duvar arkasında.
 */
public final class WallCheck extends Check {
    private static final double[] HEIGHTS = {0.5, 0.88, 0.12};

    private final double[] look = new double[3];
    private final int[] boxes = new int[2];

    public WallCheck(Settings s, ViolationSink sink) {
        super(CheckType.WALL, s, sink);
    }

    @Override
    public boolean run(AttackContext c) {
        if (c.probe == null || !c.geometryOk()) return false;
        PlayerData d = c.ad;
        boolean cancel = false;

        // ---- 1) göz bloğun içinde mi
        if (s.on(CheckType.INSIDE)) {
            int k0 = c.probe.insideKind(c.ex[0], c.ey[0], c.ez[0]);
            int k1 = c.eyeN > 1 ? c.probe.insideKind(c.ex[1], c.ey[1], c.ez[1]) : k0;
            if (k0 != 0 && k1 != 0) {
                if (k0 == 2 && k1 == 2) {
                    sink.flag(d, CheckType.INSIDE, s.insideSolidW, "gözler katı bloğun içinde");
                    cancel |= s.insideSolidCancel;
                } else {
                    d.bInsideWeb += 1.0;
                    if (d.bInsideWeb >= s.insideWebBuf) {
                        sink.flag(d, CheckType.INSIDE, s.insideWebW, "ağ içindeyken vuruş");
                        d.bInsideWeb = s.insideWebBuf * 0.5;
                        cancel |= s.insideWebCancel;
                    }
                }
                return cancel;
            }
            d.bInsideWeb = Math.max(0.0, d.bInsideWeb - 0.5);
        }

        if (!on()) return cancel;
        if (!c.targetIsPlayer && (!s.wallMobs || c.speedT > 0.05)) return cancel;
        if (c.distToCenter < 1.0 || c.degraded) return cancel;

        // ---- 2) bakış ışını testi
        int lt = lookTest(c);
        if (lt == 1) {
            d.bWallLook = Math.max(0.0, d.bWallLook - 0.5);
            return cancel;
        }

        // ---- 3) hedefin hiçbir noktası görünmüyor mu
        if (!anyVisible(c)) {
            sink.flag(d, type, s.wallW, String.format(Locale.ROOT, "hedef tamamen duvar arkasında mesafe=%.2f", c.distToCenter));
            d.bWallLook = 0.0;
            cancel |= s.wallCancel;
        } else if (lt == 2) {
            d.bWallLook += 1.0;
            if (d.bWallLook >= s.wallLookBuf) {
                sink.flag(d, type, s.wallLookW, "nişan çizgisi katı bloktan geçiyor");
                cancel |= s.wallCancel;
            }
        } else {
            d.bWallLook = Math.max(0.0, d.bWallLook - 0.5);
        }
        return cancel;
    }

    private int pickBoxes(AttackContext c) {
        int nb = 0;
        boxes[nb++] = 0;
        if (c.nearBox > 0) boxes[nb++] = c.nearBox;
        return nb;
    }

    /** 1: nişan çizgisi temiz, 2: nişan kutuya değiyor ama hepsi engelli, 0: nişan kutuya hiç değmiyor. */
    private int lookTest(AttackContext c) {
        int nb = pickBoxes(c);
        boolean anyHit = false;
        for (int r = 0; r < c.rotN; r++) {
            Geometry.look(c.yaw[r], c.pitch[r], look);
            for (int e = 0; e < c.eyeN; e++) {
                for (int k = 0; k < nb; k++) {
                    int b = boxes[k];
                    double t = Geometry.rayBox(c.ex[e], c.ey[e], c.ez[e], look[0], look[1], look[2],
                            c.minX[b] - 0.05, c.minY[b] - 0.05, c.minZ[b] - 0.05,
                            c.maxX[b] + 0.05, c.maxY[b] + 0.05, c.maxZ[b] + 0.05, 8.0);
                    if (t < 0.0) continue;
                    anyHit = true;
                    if (t < 0.3) return 1;
                    double h = c.probe.fullCubeHit(c.ex[e], c.ey[e], c.ez[e],
                            c.ex[e] + look[0] * t, c.ey[e] + look[1] * t, c.ez[e] + look[2] * t);
                    if (h < 0.0 || h > t - 0.15) return 1;
                }
            }
        }
        return anyHit ? 2 : 0;
    }

    private boolean anyVisible(AttackContext c) {
        int nb = pickBoxes(c);
        for (int k = 0; k < nb; k++) {
            int b = boxes[k];
            for (int e = 0; e < c.eyeN; e++) {
                if (boxVisible(c, e, b)) return true;
            }
        }
        return false;
    }

    private boolean boxVisible(AttackContext c, int e, int b) {
        double cx = (c.minX[b] + c.maxX[b]) * 0.5, cz = (c.minZ[b] + c.maxZ[b]) * 0.5;
        double ox = (c.maxX[b] - c.minX[b]) * 0.35, oz = (c.maxZ[b] - c.minZ[b]) * 0.35;
        double h = c.maxY[b] - c.minY[b];
        for (double f : HEIGHTS) {
            if (clear(c, e, cx, c.minY[b] + h * f, cz)) return true;
        }
        for (double f : HEIGHTS) {
            double y = c.minY[b] + h * f;
            if (clear(c, e, cx + ox, y, cz) || clear(c, e, cx - ox, y, cz)
                    || clear(c, e, cx, y, cz + oz) || clear(c, e, cx, y, cz - oz)) {
                return true;
            }
        }
        return false;
    }

    private boolean clear(AttackContext c, int e, double x, double y, double z) {
        return c.probe.colliderHit(c.ex[e], c.ey[e], c.ez[e], x, y, z) < 0.0;
    }
}
