package com.zornoely.koruma;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();
    private final List<ProtectedRegion> regions = new ArrayList<>();

    public static class ProtectedRegion {
        private final Location pos1;
        private final Location pos2;
        private final boolean preventBreak;
        private final boolean preventPlace;

        public ProtectedRegion(Location pos1, Location pos2, boolean preventBreak, boolean preventPlace) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.preventBreak = preventBreak;
            this.preventPlace = preventPlace;
        }

        public boolean isInside(Location loc) {
            if (loc.getWorld() == null || pos1.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) return false;

            double minX = Math.min(pos1.getX(), pos2.getX());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            return loc.getX() >= minX && loc.getX() <= maxX &&
                   loc.getY() >= minY && loc.getY() <= maxY &&
                   loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }

        public boolean isPreventBreak() { return preventBreak; }
        public boolean isPreventPlace() { return preventPlace; }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("balta") != null) getCommand("balta").setExecutor(this);
        if (getCommand("koruma") != null) getCommand("koruma").setExecutor(this);
        getLogger().info("ZornoKoruma aktif edildi!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyundan kullanılabilir!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("balta")) {
            ItemStack axe = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = axe.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Koruma Baltası");
                meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Sol Tık: " + ChatColor.WHITE + "1. Pozisyonu Seç",
                    ChatColor.YELLOW + "Sağ Tık: " + ChatColor.WHITE + "2. Pozisyonu Seç"
                ));
                axe.setItemMeta(meta);
            }
            player.getInventory().addItem(axe);
            player.sendMessage(color("&b&lZornoEly &8&l► &aSeçim baltası verildi! Sol ve Sağ tık ile 2 nokta seç."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("koruma")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("ac")) {
                Location p1 = pos1Map.get(player.getUniqueId());
                Location p2 = pos2Map.get(player.getUniqueId());

                if (p1 == null || p2 == null) {
                    player.sendMessage(color("&b&lZornoEly &8&l► &cÖnce /balta ile 2 köşe seçmelisin!"));
                    return true;
                }

                String altKomut = args[1].toLowerCase();

                if (altKomut.equals("blok") && args.length >= 3 && args[2].equalsIgnoreCase("kirma")) {
                    regions.add(new ProtectedRegion(p1, p2, true, false));
                    player.sendMessage(color("&b&lZornoEly &8&l► &aSeçili alanda &eSadece Blok Kırma &akoruması açıldı!"));
                    return true;
                }

                if (altKomut.equals("blok") && args.length >= 3 && args[2].equalsIgnoreCase("koyma")) {
                    regions.add(new ProtectedRegion(p1, p2, true, true));
                    player.sendMessage(color("&b&lZornoEly &8&l► &aSeçili alanda &eBlok Kırma ve Koyma &akoruması açıldı!"));
                    return true;
                }
            }

            player.sendMessage(color("&b&lZornoEly &8&l► &eKullanım:"));
            player.sendMessage(color("&f/koruma ac blok kirma &7- Sadece blok kırmayı engeller."));
            player.sendMessage(color("&f/koruma ac blok koyma &7- Blok kırma ve koymayı engeller."));
            return true;
        }

        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.getType() == Material.GOLDEN_AXE && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Koruma Baltası")) {
                if (event.getClickedBlock() == null) return;

                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    pos1Map.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                    p.sendMessage(color("&b&lZornoEly &8&l► &a1. Pozisyon seçildi! &7(" + 
                        event.getClickedBlock().getX() + ", " + 
                        event.getClickedBlock().getY() + ", " + 
                        event.getClickedBlock().getZ() + ")"));
                    event.setCancelled(true);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    pos2Map.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                    p.sendMessage(color("&b&lZornoEly &8&l► &a2. Pozisyon seçildi! &7(" + 
                        event.getClickedBlock().getX() + ", " + 
                        event.getClickedBlock().getY() + ", " + 
                        event.getClickedBlock().getZ() + ")"));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("koruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        for (ProtectedRegion region : regions) {
            if (region.isInside(loc) && region.isPreventBreak()) {
                event.setCancelled(true);
                p.sendMessage(color("&b&lZornoEly &8&l► &cBu alanda blok kıramazsın!"));
                break;
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("koruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        for (ProtectedRegion region : regions) {
            if (region.isInside(loc) && region.isPreventPlace()) {
                event.setCancelled(true);
                p.sendMessage(color("&b&lZornoEly &8&l► &cBu alanda blok koyamazsın!"));
                break;
            }
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
