package dev.zorno.guard;

import dev.zorno.guard.core.BlockProbe;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/** BlockProbe'un Paper gerçeklemesi: yalnızca yüklü chunk'larda, kısa ışınlar. */
public final class BukkitProbe implements BlockProbe {
    private World world;

    public void setWorld(World world) {
        this.world = world;
    }

    @Override
    public double colliderHit(double x1, double y1, double z1, double x2, double y2, double z2) {
        return trace(x1, y1, z1, x2, y2, z2, false);
    }

    @Override
    public double fullCubeHit(double x1, double y1, double z1, double x2, double y2, double z2) {
        return trace(x1, y1, z1, x2, y2, z2, true);
    }

    private double trace(double x1, double y1, double z1, double x2, double y2, double z2, boolean fullOnly) {
        if (world == null) return -1.0;
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4) return -1.0;
        RayTraceResult r = world.rayTraceBlocks(new Location(world, x1, y1, z1), new Vector(dx, dy, dz),
                len, FluidCollisionMode.NEVER, true);
        if (r == null) return -1.0;
        if (fullOnly) {
            Block b = r.getHitBlock();
            if (b == null || !isFullCube(b)) return -1.0;
        }
        Vector h = r.getHitPosition();
        double hx = h.getX() - x1, hy = h.getY() - y1, hz = h.getZ() - z1;
        return Math.sqrt(hx * hx + hy * hy + hz * hz);
    }

    private static boolean isFullCube(Block b) {
        BoundingBox bb = b.getBoundingBox();
        return bb.getWidthX() >= 0.99 && bb.getHeight() >= 0.99 && bb.getWidthZ() >= 0.99;
    }

    @Override
    public int insideKind(double x, double y, double z) {
        if (world == null) return 0;
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        double fx = x - bx, fy = y - by, fz = z - bz;
        // sınıra yakınsa (istemci/sunucu yuvarlama farkı) güvenme
        if (fx < 0.03 || fx > 0.97 || fy < 0.03 || fy > 0.97 || fz < 0.03 || fz > 0.97) return 0;
        Material m = world.getBlockAt(bx, by, bz).getType();
        if (m == Material.COBWEB) return 1;
        return m.isOccluding() ? 2 : 0;
    }
}
