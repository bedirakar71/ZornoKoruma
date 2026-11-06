package dev.zorno.guard;

import dev.zorno.guard.core.AttackContext;
import dev.zorno.guard.core.DetectionEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class CombatListener implements Listener {

    private final ZornoGuard plugin;
    private final DetectionEngine detectionEngine;

    public CombatListener(ZornoGuard plugin, DetectionEngine detectionEngine) {
        this.plugin = plugin;
        this.detectionEngine = detectionEngine;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player target = (Player) event.getEntity();
            
            AttackContext context = new AttackContext(attacker, target, System.currentTimeMillis());
            detectionEngine.analyzeAttack(context);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() == org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
            detectionEngine.registerArmSwing(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedPosition()) {
            detectionEngine.registerMovement(event.getPlayer(), event.getFrom(), event.To());
        }
    }
}
