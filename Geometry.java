package dev.zorno.guard.core;

/** Saf matematik: kutu mesafesi, ışın-kutu kesişimi, bakış vektörü, açılar. Bukkit'e bağımlı değil. */
public final class Geometry {
    private Geometry() {}

    /** Noktanın AABB'ye en kısa mesafesi (içerideyse 0). */
    public static double distance(double px, double py, double pz,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        double dx = Math.max(Math.max(minX - px, 0.0), px - maxX);
        double dy = Math.max(Math.max(minY - py, 0.0), py - maxY);
        double dz = Math.max(Math.max(minZ - pz, 0.0), pz - maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Işın-AABB (slab). Giriş mesafesini döndürür, çarpmıyorsa -1. Yön normalize olmalı. */
    public static double rayBox(double ox, double oy, double oz, double dx, double dy, double dz,
                                double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, double maxDist) {
        double tMin = 0.0, tMax = maxDist;
        if (Math.abs(dx) < 1.0E-9) {
            if (ox < minX || ox > maxX) return -1.0;
        } else {
            double inv = 1.0 / dx;
            double t1 = (minX - ox) * inv, t2 = (maxX - ox) * inv;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return -1.0;
        }
        if (Math.abs(dy) < 1.0E-9) {
            if (oy < minY || oy > maxY) return -1.0;
        } else {
            double inv = 1.0 / dy;
            double t1 = (minY - oy) * inv, t2 = (maxY - oy) * inv;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return -1.0;
        }
        if (Math.abs(dz) < 1.0E-9) {
            if (oz < minZ || oz > maxZ) return -1.0;
        } else {
            double inv = 1.0 / dz;
            double t1 = (minZ - oz) * inv, t2 = (maxZ - oz) * inv;
            if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return -1.0;
        }
        return tMin;
    }

    /** Işının kutuya en yakın geçtiği mesafe (blok). Çarpıyorsa 0. Dışbükey olduğu için üçlü arama. */
    public static double rayBoxMiss(double ox, double oy, double oz, double dx, double dy, double dz,
                                    double maxT,
                                    double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        if (rayBox(ox, oy, oz, dx, dy, dz, minX, minY, minZ, maxX, maxY, maxZ, maxT) >= 0.0) return 0.0;
        double lo = 0.0, hi = maxT;
        for (int i = 0; i < 28; i++) {
            double m1 = lo + (hi - lo) / 3.0;
            double m2 = hi - (hi - lo) / 3.0;
            double d1 = distance(ox + dx * m1, oy + dy * m1, oz + dz * m1, minX, minY, minZ, maxX, maxY, maxZ);
            double d2 = distance(ox + dx * m2, oy + dy * m2, oz + dz * m2, minX, minY, minZ, maxX, maxY, maxZ);
            if (d1 < d2) hi = m2; else lo = m1;
        }
        double t = (lo + hi) * 0.5;
        return distance(ox + dx * t, oy + dy * t, oz + dz * t, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Minecraft bakış vektörü (Location#getDirection ile aynı). */
    public static void look(double yawDeg, double pitchDeg, double[] out) {
        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        double cp = Math.cos(pitch);
        out[0] = -Math.sin(yaw) * cp;
        out[1] = -Math.sin(pitch);
        out[2] = Math.cos(yaw) * cp;
    }

    public static double wrap(double a) {
        a = a % 360.0;
        if (a >= 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }

    /** İki vektör arasındaki açı (derece). */
    public static double angleBetween(double ax, double ay, double az, double bx, double by, double bz) {
        double la = Math.sqrt(ax * ax + ay * ay + az * az);
        double lb = Math.sqrt(bx * bx + by * by + bz * bz);
        if (la < 1.0E-9 || lb < 1.0E-9) return 0.0;
        double dot = (ax * bx + ay * by + az * bz) / (la * lb);
        if (dot > 1.0) dot = 1.0;
        if (dot < -1.0) dot = -1.0;
        return Math.toDegrees(Math.acos(dot));
    }

    /** İki bakış yönü arasındaki gerçek açı (derece). */
    public static double rotDistance(double yaw1, double pitch1, double yaw2, double pitch2) {
        double[] a = new double[3], b = new double[3];
        look(yaw1, pitch1, a);
        look(yaw2, pitch2, b);
        return angleBetween(a[0], a[1], a[2], b[0], b[1], b[2]);
    }
}
