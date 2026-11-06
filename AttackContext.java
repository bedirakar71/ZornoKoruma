package dev.zorno.guard.core;

/** Tek bir vuruş için toplanmış (Bukkit'ten bağımsız) ölçümler. */
public final class AttackContext {
    public static final int MAX_EYES = 3, MAX_ROTS = 3, MAX_BOXES = 16;

    public PlayerData ad;
    public int tick;
    public long nowNanos;
    public int targetId;
    public boolean targetIsPlayer;

    // saldırganın göz konumu adayları (0 = canlı)
    public final double[] ex = new double[MAX_EYES], ey = new double[MAX_EYES], ez = new double[MAX_EYES];
    public int eyeN;
    // bakış adayları (0 = canlı)
    public final float[] yaw = new float[MAX_ROTS], pitch = new float[MAX_ROTS];
    public int rotN;
    // hedef kutusu adayları (0 = canlı, diğerleri ping telafisi için geçmiş)
    public final double[] minX = new double[MAX_BOXES], minY = new double[MAX_BOXES], minZ = new double[MAX_BOXES];
    public final double[] maxX = new double[MAX_BOXES], maxY = new double[MAX_BOXES], maxZ = new double[MAX_BOXES];
    public int boxN;
    public int nearBox;

    public double tcx, tcy, tcz;
    public double distToCenter;
    public double ping;
    public int back;
    public double speedA, speedT, rotSpeed;
    public double reachMax = 3.0;

    public boolean vehicle, gliding, legacy, creative, sneakRecent;
    public boolean usingItem, containerOpen;
    public long sinceInvOpenMs = Long.MAX_VALUE;
    public boolean lagging, inGrace, degraded;

    public BlockProbe probe;

    public boolean addEye(double x, double y, double z) {
        for (int i = 0; i < eyeN; i++) {
            double dx = ex[i] - x, dy = ey[i] - y, dz = ez[i] - z;
            if (dx * dx + dy * dy + dz * dz < 1.0E-8) return false;
        }
        if (eyeN >= MAX_EYES) return false;
        ex[eyeN] = x; ey[eyeN] = y; ez[eyeN] = z;
        eyeN++;
        return true;
    }

    public boolean addRot(float yw, float pt) {
        for (int i = 0; i < rotN; i++) {
            if (Math.abs(yaw[i] - yw) < 1.0E-4f && Math.abs(pitch[i] - pt) < 1.0E-4f) return false;
        }
        if (rotN >= MAX_ROTS) return false;
        yaw[rotN] = yw; pitch[rotN] = pt;
        rotN++;
        return true;
    }

    public boolean addBox(double x0, double y0, double z0, double x1, double y1, double z1) {
        for (int i = 0; i < boxN; i++) {
            if (Math.abs(minX[i] - x0) < 1.0E-4 && Math.abs(minY[i] - y0) < 1.0E-4 && Math.abs(minZ[i] - z0) < 1.0E-4
                    && Math.abs(maxX[i] - x1) < 1.0E-4 && Math.abs(maxY[i] - y1) < 1.0E-4 && Math.abs(maxZ[i] - z1) < 1.0E-4) {
                return false;
            }
        }
        if (boxN >= MAX_BOXES) return false;
        minX[boxN] = x0; minY[boxN] = y0; minZ[boxN] = z0;
        maxX[boxN] = x1; maxY[boxN] = y1; maxZ[boxN] = z1;
        boxN++;
        return true;
    }

    /** Geometri tabanlı kontroller için yeterli ve güvenilir veri var mı. */
    public boolean geometryOk() {
        return !lagging && !inGrace && eyeN > 0 && rotN > 0 && boxN > 0;
    }
}
