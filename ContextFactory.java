package dev.zorno.guard;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.Geometry;
import dev.zorno.guard.core.PlayerData;
import dev.zorno.guard.core.Settings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.util.BoundingBox;

/** Bukkit nesnelerinden saf (primitif) AttackContext üretir. Hiçbir ağır işlem yapmaz. */
public final class ContextFactory {
    private final ZornoGuard plugin;
    private final Settings s;
    private final BukkitProbe probe = new BukkitProbe();
    private final ViaCompat via = new ViaCompat();
    private final Location tmp = new Location(null, 0, 0, 0);

    public ContextFactory(ZornoGuard plugin, Settings s) {
        this.plugin = plugin;
        this.s = s;
    }

    public AttackContext build(Player p, PlayerData ad, LivingEntity target) {
        int tick = plugin.tick();
        AttackContext c = new AttackContext();
        c.ad = ad;
        c.tick = tick;
        c.nowNanos = System.nanoTime();
        c.targetId = target.getEntityId();
        c.lagging = tick < plugin.lagUntil();
        c.degraded = plugin.degraded();
        c.inGrace = tick < ad.validFrom + s.graceTicks;
        probe.setWorld(p.getWorld());
        c.probe = probe;

        // ---- saldırgan: göz konumu ve bakış adayları (canlı, tick başı, önceki tick)
        Location eye = p.getEyeLocation();
        c.addEye(eye.getX(), eye.getY(), eye.getZ());
        c.addRot(eye.getYaw(), eye.getPitch());
        for (int t = tick; t >= tick - 1; t--) {
            if (!ad.has(t)) continue;
            int i = PlayerData.idx(t);
            c.addEye(ad.hx[i], ad.hy[i] + ad.he[i], ad.hz[i]);
            c.addRot(ad.hyaw[i], ad.hpitch[i]);
        }

        p.getLocation(tmp);
        double fx = tmp.getX(), fy = tmp.getY(), fz = tmp.getZ();
        double sa = 0.0;
        double rs = 0.0;
        if (ad.has(tick)) {
            int i = PlayerData.idx(tick);
            sa = dist(fx, fy, fz, ad.hx[i], ad.hy[i], ad.hz[i]);
            rs = Geometry.rotDistance(ad.hyaw[i], ad.hpitch[i], eye.getYaw(), eye.getPitch());
            if (ad.has(tick - 1)) {
                int j = PlayerData.idx(tick - 1);
                sa = Math.max(sa, dist(ad.hx[i], ad.hy[i], ad.hz[i], ad.hx[j], ad.hy[j], ad.hz[j]));
                rs = Math.max(rs, Geometry.rotDistance(ad.hyaw[j], ad.hpitch[j], ad.hyaw[i], ad.hpitch[i]));
            }
        }
        c.speedA = sa;
        c.rotSpeed = rs;

        // ---- gecikme telafisi penceresi
        int ping = p.getPing();
        c.ping = ping;
        c.back = Math.max(3, Math.min(s.maxCompTicks, (int) Math.ceil(ping / 50.0) + 3));

        // ---- hedef kutuları
        BoundingBox bb = target.getBoundingBox();
        c.addBox(bb.getMinX(), bb.getMinY(), bb.getMinZ(), bb.getMaxX(), bb.getMaxY(), bb.getMaxZ());
        c.tcx = (bb.getMinX() + bb.getMaxX()) * 0.5;
        c.tcy = (bb.getMinY() + bb.getMaxY()) * 0.5;
        c.tcz = (bb.getMinZ() + bb.getMaxZ()) * 0.5;

        boolean targetGliding = false;
        if (target instanceof Player tp) {
            c.targetIsPlayer = true;
            targetGliding = tp.isGliding();
            PlayerData td = plugin.dataOrNull(tp);
            if (td == null) {
                c.inGrace = true;
            } else {
                if (tick < td.validFrom + s.graceTicks) c.inGrace = true;
                double maxStep = 0.0;
                for (int t = tick; t >= tick - c.back; t--) {
                    if (!td.has(t)) break;
                    int j = PlayerData.idx(t);
                    double hw = td.hw[j] * 0.5;
                    c.addBox(td.hx[j] - hw, td.hy[j], td.hz[j] - hw, td.hx[j] + hw, td.hy[j] + td.hh[j], td.hz[j] + hw);
                    if (td.has(t - 1)) {
                        int k = PlayerData.idx(t - 1);
                        maxStep = Math.max(maxStep, dist(td.hx[j], td.hy[j], td.hz[j], td.hx[k], td.hy[k], td.hz[k]));
                    }
                }
                if (td.has(tick)) {
                    int j = PlayerData.idx(tick);
                    maxStep = Math.max(maxStep, dist(bb.getMinX() + (bb.getMaxX() - bb.getMinX()) * 0.5, bb.getMinY(),
                            bb.getMinZ() + (bb.getMaxZ() - bb.getMinZ()) * 0.5, td.hx[j], td.hy[j], td.hz[j]));
                }
                c.speedT = maxStep;
            }
        } else {
            c.targetIsPlayer = false;
            org.bukkit.util.Vector v = target.getVelocity();
            double vy = Math.abs(v.getY()) < 0.1 ? 0.0 : v.getY(); // yerdeki mobun sabit yerçekimi değeri hız sayılmasın
            c.speedT = Math.sqrt(v.getX() * v.getX() + vy * vy + v.getZ() * v.getZ());
        }

        // ---- en yakın kutu ve merkeze uzaklık
        double best = Double.MAX_VALUE;
        for (int b = 0; b < c.boxN; b++) {
            double dd = Geometry.distance(c.ex[0], c.ey[0], c.ez[0], c.minX[b], c.minY[b], c.minZ[b], c.maxX[b], c.maxY[b], c.maxZ[b]);
            if (dd < best) {
                best = dd;
                c.nearBox = b;
            }
        }
        double cdx = c.tcx - c.ex[0], cdy = c.tcy - c.ey[0], cdz = c.tcz - c.ez[0];
        c.distToCenter = Math.sqrt(cdx * cdx + cdy * cdy + cdz * cdz);

        // ---- durum bayrakları
        c.reachMax = plugin.reachOf(p);
        c.vehicle = p.isInsideVehicle() || target.isInsideVehicle();
        c.gliding = p.isGliding() || targetGliding;
        c.legacy = via.isLegacy(p.getUniqueId());
        c.creative = p.getGameMode() == GameMode.CREATIVE;
        c.sneakRecent = tick - ad.sneakToggleTick <= 2;
        c.usingItem = p.isHandRaised() || p.isBlocking();
        InventoryType it = p.getOpenInventory().getType();
        c.containerOpen = it != InventoryType.CRAFTING && it != InventoryType.CREATIVE;
        c.sinceInvOpenMs = ad.lastInvOpenMs == 0L ? Long.MAX_VALUE : System.currentTimeMillis() - ad.lastInvOpenMs;
        return c;
    }

    private static double dist(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
