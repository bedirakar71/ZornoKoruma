package com.zornoely.koruma;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
        private final String typeName;

        public ProtectedRegion(Location pos1, Location pos2, boolean preventBreak, boolean preventPlace, int priority, String typeName) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.specificBlocks = null;
            this.preventBreak = preventBreak;
            this.preventPlace = preventPlace;
            this.priority = priority;
            this.typeName = typeName;
        }

        public ProtectedRegion(Set<Location> specificBlocks, boolean preventBreak, boolean preventPlace, int priority, String typeName) {
            this.pos1 = null;
            this.pos2 = null;
            this.specificBlocks = new HashSet<>(specificBlocks);
            this.preventBreak = preventBreak;
            this.preventPlace = preventPlace;
            this.priority = priority;
            this.typeName = typeName;
        }

        public boolean isInside(Location loc) {
            if (specificBlocks != null) {
                for (Location bLoc : specificBlocks) {
                    if (bLoc != null && bLoc.getWorld() != null && loc != null && loc.getWorld() != null &&
                        bLoc.getWorld().equals(loc.getWorld()) &&
                        bLoc.getBlockX() == loc.getBlockX() &&
                        bLoc.getBlockY() == loc.getBlockY() &&
                        bLoc.getBlockZ() == loc.getBlockZ()) {
                        return true;
                    }
                }
                return false;
            }

            if (pos1 == null || pos2 == null || loc == null || loc.getWorld() == null || pos1.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) {
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
        public String getTypeName() { return typeName; }
        public boolean isSpecific() { return specificBlocks != null; }
        public Set<Location> getSpecificBlocks() { return specificBlocks; }
        public Location getPos1() { return pos1; }
        public Location getPos2() { return pos2; }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRegionsFromConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("balta") != null) getCommand("balta").setExecutor(this);
        if (getCommand("kazma") != null) getCommand("kazma").setExecutor(this);
        if (getCommand("koruma") != null) getCommand("koruma").setExecutor(this);
        getLogger().info("ZornoKoruma aktif edildi! (" + regions.size() + " alan yuklendi)");
    }

    @Override
    public void onDisable() {
        saveRegionsToConfig();
    }

    private void saveRegionsToConfig() {
        FileConfiguration config = getConfig();
        config.set("regions", null);

        for (int i = 0; i < regions.size(); i++) {
            ProtectedRegion region = regions.get(i);
            String path = "regions." + i;

            config.set(path + ".preventBreak", region.isPreventBreak());
            config.set(path + ".preventPlace", region.isPreventPlace());
            config.set(path + ".priority", region.getPriority());
            config.set(path + ".typeName", region.getTypeName());
            config.set(path + ".isSpecific", region.isSpecific());

            if (region.isSpecific()) {
                List<String> locList = new ArrayList<>();
                for (Location l : region.getSpecificBlocks()) {
                    if (l != null && l.getWorld() != null) {
                        locList.add(l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                    }
                }
                config.set(path + ".blocks", locList);
            } else {
                if (region.getPos1() != null && region.getPos2() != null && region.getPos1().getWorld() != null) {
                    config.set(path + ".world", region.getPos1().getWorld().getName());
                    config.set(path + ".pos1", region.getPos1().getX() + "," + region.getPos1().getY() + "," + region.getPos1().getZ());
                    config.set(path + ".pos2", region.getPos2().getX() + "," + region.getPos2().getY() + "," + region.getPos2().getZ());
                }
            }
        }
        saveConfig();
    }

    private void loadRegionsFromConfig() {
        regions.clear();
        FileConfiguration config = getConfig();
        if (!config.contains("regions") || config.getConfigurationSection("regions") == null) return;

        for (String key : config.getConfigurationSection("regions").getKeys(false)) {
            String path = "regions." + key;
            boolean preventBreak = config.getBoolean(path + ".preventBreak");
            boolean preventPlace = config.getBoolean(path + ".preventPlace");
            int priority = config.getInt(path + ".priority", 1);
            String typeName = config.getString(path + ".typeName", "Koruma");
            boolean isSpecific = config.getBoolean(path + ".isSpecific");

            if (isSpecific) {
                List<String> blockStrs = config.getStringList(path + ".blocks");
                Set<Location> locs = new HashSet<>();
                for (String s : blockStrs) {
                    String[] parts = s.split(",");
                    if (parts.length == 4 && Bukkit.getWorld(parts[0]) != null) {
                        locs.add(new Location(
                            Bukkit.getWorld(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3])
                        ));
                    }
                }
                if (!locs.isEmpty()) {
                    regions.add(new ProtectedRegion(locs, preventBreak, preventPlace, priority, typeName));
                }
            } else {
                String worldName = config.getString(path + ".world");
                String p1Str = config.getString(path + ".pos1");
                String p2Str = config.getString(path + ".pos2");

                if (worldName != null && p1Str != null && p2Str != null && Bukkit.getWorld(worldName) != null) {
                    String[] p1Parts = p1Str.split(",");
                    String[] p2Parts = p2Str.split(",");

                    Location p1 = new Location(Bukkit.getWorld(worldName), Double.parseDouble(p1Parts[0]), Double.parseDouble(p1Parts[1]), Double.parseDouble(p1Parts[2]));
                    Location p2 = new Location(Bukkit.getWorld(worldName), Double.parseDouble(p2Parts[0]), Double.parseDouble(p2Parts[1]), Double.parseDouble(p2Parts[2]));

                    regions.add(new ProtectedRegion(p1, p2, preventBreak, preventPlace, priority, typeName));
                }
            }
        }
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
            player.sendMessage(color("&b&lZornoEly &8&l► &aSeçim baltası verildi!"));
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
                meta.setLore(Arrays.asList(ChatColor.YELLOW + "Tıkla: " + ChatColor.WHITE + "Blok Seç / Seçimi Kaldır"));
                pick.setItemMeta(meta);
            }
            player.getInventory().addItem(pick);
            player.sendMessage(color("&b&lZornoEly &8&l► &aTekil blok seçim kazması verildi!"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("koruma")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("liste")) {
                if (regions.isEmpty()) {
                    player.sendMessage(color("&b&lZornoEly &8&l► &cKayıtlı koruma/alan bulunamadı."));
                    return true;
                }
                player.sendMessage(color("&b&lZornoEly &8&l► &eKayıtlı Alanlar (" + regions.size() + "):"));
                for (int i = 0; i < regions.size(); i++) {
                    ProtectedRegion r = regions.get(i);
                    player.sendMessage(color("&7" + (i + 1) + ". [&b" + r.getTypeName() + "&7] &fÖncelik: &e" + r.getPriority() + " &f| Kırma: " + (r.isPreventBreak() ? "&cYasak" : "&aSerbest") + " &f| Koyma: " + (r.isPreventPlace() ? "&cYasak" : "&aSerbest")));
                }
                return true;
            }

            if (args.length >= 2 && args[0].equalsIgnoreCase("sil")) {
                try {
                    int index = Integer.parseInt(args[1]) - 1;
                    if (index >= 0 && index < regions.size()) {
                        regions.remove(index);
                        saveRegionsToConfig();
                        player.sendMessage(color("&b&lZornoEly &8&l► &a" + (index + 1) + " numaralı alan silindi!"));
                    } else {
                        player.sendMessage(color("&b&lZornoEly &8&l► &cGeçersiz numara!"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(color("&b&lZornoEly &8&l► &cLütfen sayı girin: /koruma sil <no>"));
                }
                return true;
            }

            if (args.length >= 1) {
                String subCommand = args[0].toLowerCase();
                Set<Location> selectedBlocks = blockSelectionMap.get(player.getUniqueId());
                Location p1 = pos1Map.get(player.getUniqueId());
                Location p2 = pos2Map.get(player.getUniqueId());

                int priority = 1;
                boolean preventBreak = true;
                boolean preventPlace = false; 
                String typeName = "Koruma";

                if (subCommand.equals("maden")) {
                    preventBreak = false;
                    preventPlace = true;
                    typeName = "Maden";
                    priority = 10;
                    if (args.length >= 2) {
                        try { priority = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                    }
                } 
                else if (subCommand.equals("serbest")) {
                    preventBreak = false;
                    preventPlace = false;
                    typeName = "Serbest";
                    if (args.length >= 2) {
                        try { priority = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                    }
                } 
                else if (subCommand.equals("ac")) {
                    if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
                        preventBreak = true;
                        preventPlace = true; 
                        typeName = "Tam Koruma (Kilitli)";
                        if (args.length >= 3) {
                            try { priority = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) {}
                        }
                    } else {
                        preventBreak = true;
                        preventPlace = false; 
                        typeName = "Koruma (Kırma Yasak)";
                        if (args.length >= 2) {
                            try { priority = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                if (selectedBlocks != null && !selectedBlocks.isEmpty()) {
                    regions.add(new ProtectedRegion(selectedBlocks, preventBreak, preventPlace, priority, typeName));
                    blockSelectionMap.remove(player.getUniqueId());
                    saveRegionsToConfig();
                    player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen " + selectedBlocks.size() + " blok " + typeName + " olarak ayarlandı! &7(Öncelik: " + priority + ")"));
                    return true;
                } else if (p1 != null && p2 != null) {
                    regions.add(new ProtectedRegion(p1, p2, preventBreak, preventPlace, priority, typeName));
                    saveRegionsToConfig();
                    player.sendMessage(color("&b&lZornoEly &8&l► &aSeçilen alan " + typeName + " olarak ayarlandı! &7(Öncelik: " + priority + ")"));
                    return true;
                } else {
                    player.sendMessage(color("&b&lZornoEly &8&l► &cÖnce /balta ile alan seçmelisin!"));
                    return true;
                }
            }

            player.sendMessage(color("&b&lZornoEly &8&l► &eKullanım:"));
            player.sendMessage(color("&f/koruma ac <öncelik> &7- Sadece kırma koruması açar (Koyma serbest)."));
            player.sendMessage(color("&f/koruma ac all <öncelik> &7- Hem kırma hem koyma korumasını açar."));
            player.sendMessage(color("&f/koruma maden <öncelik> &7- Maden alanı yapar (Kırma serbest)."));
            player.sendMessage(color("&f/koruma serbest <öncelik> &7- Tamamen serbest alan yapar."));
            player.sendMessage(color("&f/koruma liste &7- Kayıtlı alanları gösterir."));
            player.sendMessage(color("&f/koruma sil <no> &7- Alanı siler."));
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
                    p.sendMessage(color("&b&lZornoEly &8&l► &cBlok seçimden çıkarıldı."));
                } else {
                    set.add(loc);
                    p.sendMessage(color("&b&lZornoEly &8&l► &aBlok seçildi!"));
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

        if (highestPriorityRegion != null && highestPrio
