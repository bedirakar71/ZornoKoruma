src/main/java/dev/zorno/guard/CombatListener.javapackage dev.zorno.guard;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.PlayerData;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public final class CombatListener implements Listener {
    private final ZornoGuard plugin;

    public CombatListener(ZornoGuard plugin) {
        this.plugin = plugin;
    }

    /** Vuruş paketi işlenmeden hemen önce (hasar olayından önce): tüm saldırı denemelerini görür. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(PrePlayerAttackEntityEvent e) {
        if (!(e.getAttacked() instanceof LivingEntity target)) return;
        if (target instanceof ArmorStand || target instanceof EnderDragon) return;
        Player p = e.getPlayer();
        if (plugin.isExempt(p)) return;

        long t0 = System.nanoTime();
        PlayerData d = plugin.data(p);
        AttackContext c = plugin.factory().build(p, d, target);
        if (c != null && plugin.engine().onAttack(c) && plugin.settings().cancelHits) {
            e.setCancelled(true);
        }
        plugin.perf().record(System.nanoTime() - t0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwing(PlayerAnimationEvent e) {
        PlayerData d = plugin.dataOrNull(e.getPlayer());
        if (d != null) d.lastSwingTick = plugin.tick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent e) {
        PlayerData d = plugin.dataOrNull(e.getPlayer());
        if (d != null) d.sneakToggleTick = plugin.tick();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent e) {
        HumanEntity he = e.getPlayer();
        if (he instanceof Player p) {
            PlayerData d = plugin.dataOrNull(p);
            if (d != null) d.lastInvOpenMs = System.currentTimeMillis();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerData d = plugin.dataOrNull(e.getPlayer());
        if (d != null) d.resetHistory(plugin.tick());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        PlayerData d = plugin.dataOrNull(e.getPlayer());
        if (d != null) d.resetHistory(plugin.tick());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        PlayerData d = plugin.dataOrNull(e.getPlayer());
        if (d != null) d.resetHistory(plugin.tick());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        plugin.data(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.remove(e.getPlayer());
    }
}
