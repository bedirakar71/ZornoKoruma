package com.zornoely.koruma;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
    private final Map<UUID, Set<Location>> blockSelectionMap = new HashMap<>();
    private final List<ProtectedRegion> regions = new ArrayList<>();

    public static class ProtectedRegion {
        private final Location pos1;
        private final Location pos2;
        private final Set<Location> specificBlocks;
        private final boolean preventBreak;
        private final boolean preventPlace;
        private final int priority;

        // Küp (Cuboid) koruma - Balta ile
        public ProtectedRegion(Location pos1, Location pos2, boolean preventBreak, boolean preventPlace, int priority) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.specificBlocks = null;
            this.preventBreak = preventBreak;
            this.preventPlace = preventPlace;
            this.priority = priority;
        }

        // Tekil blok seçimi - Kazma ile (Üçgen vb. özel şekiller)
        public ProtectedRegion(Set<Location> specificBlocks, boolean preventBreak, boolean preventPlace, int priority) {
            this.pos1 = null;
            this.pos2 = null;
            this.specificBlocks = new HashSet<>(specificBlocks);
            this.preventBreak = preventBreak;
            this.preventPlace = preventPlace;
            this.priority = priority;
        }

        public boolean isInside(Location loc) {
            if (specificBlocks != null) {
                for (Location bLoc : specificBlocks) {
                    if (bLoc.getWorld() != null && loc.getWorld() != null &&
                        bLoc.getWorld().equals(loc.getWorld()) &&
                        bLoc.getBlockX() == loc.getBlockX() &&
                        bLoc.getBlockY() == loc.getBlockY() &&
                        bLoc.getBlockZ() == loc.getBlockZ()) {
                        return true;
                    }
                }
                return false;
            }

            if (pos1 == null || pos2 == null || loc.getWorld() == null || pos1.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) {
                return false;
            }

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
        public int getPriority() { return priority; }
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("balta") != null) getCommand("balta").setExecutor(this);
        if (getCommand("kazma") != null) getCommand("kazma").setExecutor(this);
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
            player.sendMessage(color("&b&lZornoEly &8&l► &aKutu seçim baltası verildi! (2 köşe seçimi)"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("kazma")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("temizle")) {
                blockSelectionMap.remove(player.getUniqueId());
                player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen özel bloklar temizlendi!"));
                return true;
            }

            ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
            ItemMeta meta = pick.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "Koruma Kazması");
                meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Tıkla: " + ChatColor.WHITE + "Blok Seç / Seçimi Kaldır",
                    ChatColor.GRAY + "Üçgen ve özel şekiller yapmak içindir."
                ));
                pick.setItemMeta(meta);
            }
            player.getInventory().addItem(pick);
            player.sendMessage(color("&b&lZornoEly &8&l► &aTekil blok seçim kazması verildi! İstediğin bloklara tıkla."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("koruma")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("ac")) {
                Set<Location> selectedBlocks = blockSelectionMap.get(player.getUniqueId());
                Location p1 = pos1Map.get(player.getUniqueId());
                Location p2 = pos2Map.get(player.getUniqueId());

                int priority = 1;
                if (args.length >= 4) {
                    try {
                        priority = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(color("&b&lZornoEly &8&l► &cGeçersiz öncelik sayısı!"));
                        return true;
                    }
                }

                String tip = args[1].toLowerCase();
                String durum = args.length >= 3 ? args[2].toLowerCase() : "";

                if (selectedBlocks != null && !selectedBlocks.isEmpty()) {
                    if (tip.equals("blok")) {
                        if (durum.equals("kirma")) {
                            regions.add(new ProtectedRegion(selectedBlocks, true, false, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen " + selectedBlocks.size() + " blokta Kırma YASAKLANDI! &7(Öncelik: " + priority + ")"));
                        } else if (durum.equals("serbest")) {
                            regions.add(new ProtectedRegion(selectedBlocks, false, false, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen " + selectedBlocks.size() + " blokta Kırma SERBEST! &7(Öncelik: " + priority + ")"));
                        } else if (durum.equals("koyma")) {
                            regions.add(new ProtectedRegion(selectedBlocks, true, true, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen " + selectedBlocks.size() + " blokta Kırma ve Koyma YASAKLANDI! &7(Öncelik: " + priority + ")"));
                        }
                        blockSelectionMap.remove(player.getUniqueId());
                        return true;
                    }
                } else if (p1 != null && p2 != null) {
                    if (tip.equals("blok")) {
                        if (durum.equals("kirma")) {
                            regions.add(new ProtectedRegion(p1, p2, true, false, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aAlanda Blok Kırma YASAKLANDI! &7(Öncelik: " + priority + ")"));
                        } else if (durum.equals("serbest")) {
                            regions.add(new ProtectedRegion(p1, p2, false, false, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aAlanda Blok Kırma SERBEST! &7(Öncelik: " + priority + ")"));
                        } else if (durum.equals("koyma")) {
                            regions.add(new ProtectedRegion(p1, p2, true, true, priority));
                            player.sendMessage(color("&b&lZornoEly &8&l► &aAlanda Kırma ve Koyma YASAKLANDI! &7(Öncelik: " + priority + ")"));
                        }
                        return true;
                    }
                } else {
                    player.sendMessage(color("&b&lZornoEly &8&l► &cÖnce /balta veya /kazma ile seçim yapmalısın!"));
                    return true;
                }
            }

            player.sendMessage(color("&b&lZornoEly &8&l► &eKullanım Örnekleri:"));
            player.sendMessage(color("&f/koruma ac blok kirma [oncelik]"));
            player.sendMessage(color("&f/koruma ac blok serbest [oncelik]"));
            player.sendMessage(color("&f/koruma ac blok koyma [oncelik]"));
            return true;
        }

        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item.hasItemMeta() && item.getItemMeta() != null) {
            String name = item.getItemMeta().getDisplayName();

            if (item.getType() == Material.GOLDEN_AXE && name.equals(ChatColor.GOLD + "Koruma Baltası")) {
                if (event.getClickedBlock() == null) return;
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    pos1Map.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                    p.sendMessage(color("&b&lZornoEly &8&l► &a1. Pozisyon seçildi!"));
                    event.setCancelled(true);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    pos2Map.put(p.getUniqueId(), event.getClickedBlock().getLocation());
                    p.sendMessage(color("&b&lZornoEly &8&l► &a2. Pozisyon seçildi!"));
                    event.setCancelled(true);
                }
            }

            if (item.getType() == Material.DIAMOND_PICKAXE && name.equals(ChatColor.AQUA + "Koruma Kazması")) {
                Block b = event.getClickedBlock();
                if (b == null) return;

                event.setCancelled(true);
                Set<Location> set = blockSelectionMap.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
                Location loc = b.getLocation();

                if (set.contains(loc)) {
                    set.remove(loc);
                    p.sendMessage(color("&b&lZornoEly &8&l► &cBlok seçimden çıkarıldı. &7(Kalan: " + set.size() + ")"));
                } else {
                    set.add(loc);
                    p.sendMessage(color("&b&lZornoEly &8&l► &aBlok seçildi! &7(Toplam: " + set.size() + ")"));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("koruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        ProtectedRegion highestPriorityRegion = null;

        for (ProtectedRegion region : regions) {
            if (region.isInside(loc)) {
                if (highestPriorityRegion == null || region.getPriority() > highestPriorityRegion.getPriority()) {
                    highestPriorityRegion = region;
                }
            }
        }

        if (highestPriorityRegion != null) {
            if (highestPriorityRegion.isPreventBreak()) {
                event.setCancelled(true);
                p.sendMessage(color("&b&lZornoEly &8&l► &cBu alanda blok kıramazsın!"));
            } else {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("koruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        ProtectedRegion highestPriorityRegion = null;

        for (ProtectedRegion region : regions) {
            if (region.isInside(loc)) {
                if (highestPriorityRegion == null || region.getPriority() > highestPriorityRegion.getPriority()) {
                    highestPriorityRegion = region;
                }
            }
        }

        if (highestPriorityRegion != null && highestPriorityRegion.isPreventPlace()) {
            event.setCancelled(true);
            p.sendMessage(color("&b&lZornoEly &8&l► &cBu alanda blok koyamazsın!"));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
