package com.zornoely.koruma;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

public final class Main extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();
    private final Map<UUID, Set<Location>> singleBlocksMap = new HashMap<>();
    private final Map<String, Region> regions = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("koruma") != null) {
            getCommand("koruma").setExecutor(this);
        }
        if (getCommand("balta") != null) {
            getCommand("balta").setExecutor(this);
        }
        if (getCommand("kazma") != null) {
            getCommand("kazma").setExecutor(this);
        }
        loadRegions();
        getLogger().info("ZornoKoruma gelismis sistem aktif edildi!");
    }

    @Override
    public void onDisable() {
        saveRegions();
        getLogger().info("ZornoKoruma devre disi birakildi!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }

        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("balta")) {
            ItemStack axe = new ItemStack(Material.WOODEN_AXE);
            ItemMeta meta = axe.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Koruma Baltasi");
                meta.setLore(Arrays.asList(ChatColor.YELLOW + "Sol tik: 1. Kose", ChatColor.YELLOW + "Sag tik: 2. Kose"));
                axe.setItemMeta(meta);
            }
            player.getInventory().addItem(axe);
            player.sendMessage(ChatColor.GREEN + "Eline Koruma Baltasi verildi! Sol tikla 1, sag tikla 2. koseyi sec.");
            return true;
        }

        if (cmdName.equals("kazma")) {
            ItemStack pickaxe = new ItemStack(Material.WOODEN_PICKAXE);
            ItemMeta meta = pickaxe.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "Tekli Blok Secici");
                meta.setLore(Collections.singletonList(ChatColor.YELLOW + "Sag tiklayarak tek tek blok ekle"));
                pickaxe.setItemMeta(meta);
            }
            player.getInventory().addItem(pickaxe);
            player.sendMessage(ChatColor.GREEN + "Eline Tekli Blok Secici kazma verildi! Sag tikladigin bloklar listeye eklenecek.");
            return true;
        }

        if (cmdName.equals("koruma")) {
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "--- ZornoKoruma Yardim ---");
                player.sendMessage(ChatColor.YELLOW + "/balta " + ChatColor.WHITE + "- Bolge secim baltasi al");
                player.sendMessage(ChatColor.YELLOW + "/kazma " + ChatColor.WHITE + "- Tekli blok secim kazmasi al");
                player.sendMessage(ChatColor.YELLOW + "/koruma ac <isim> <kirma/all/maden/serbest> <oncelik> " + ChatColor.WHITE + "- Bolgeyi korumaya al");
                player.sendMessage(ChatColor.YELLOW + "/koruma sil <isim> " + ChatColor.WHITE + "- Bolgeyi sil");
                player.sendMessage(ChatColor.YELLOW + "/koruma liste " + ChatColor.WHITE + "- Bolgeleri listele");
                return true;
            }

            if (args[0].equalsIgnoreCase("ac")) {
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Kullanim: /koruma ac <isim> <kirma/all/maden/serbest> <oncelik>");
                    return true;
                }

                String name = args[1];
                String type = args[2].toLowerCase();
                int priority;

                try {
                    priority = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Oncelik degeri bir sayi olmalidir!");
                    return true;
                }

                if (!type.equals("kirma") && !type.equals("all") && !type.equals("maden") && !type.equals("serbest")) {
                    player.sendMessage(ChatColor.RED + "Tur sadece su olabilir: kirma, all, maden, serbest");
                    return true;
                }

                Location p1 = pos1Map.get(player.getUniqueId());
                Location p2 = pos2Map.get(player.getUniqueId());
                Set<Location> customBlocks = singleBlocksMap.get(player.getUniqueId());

                if ((p1 == null || p2 == null) && (customBlocks == null || customBlocks.isEmpty())) {
                    player.sendMessage(ChatColor.RED + "Once /balta ile alan secin veya /kazma ile bloklar secin!");
                    return true;
                }

                Region region;
                if (p1 != null && p2 != null) {
                    region = new Region(name, p1, p2, type, priority);
                } else {
                    region = new Region(name, customBlocks, type, priority);
                }

                regions.put(name, region);
                saveRegions();

                // Temizlik
                pos1Map.remove(player.getUniqueId());
                pos2Map.remove(player.getUniqueId());
                singleBlocksMap.remove(player.getUniqueId());

                player.sendMessage(ChatColor.GREEN + "'" + name + "' korumasi basariyla olusturuldu! (Tur: " + type + ", Oncelik: " + priority + ")");
                return true;
            }

            if (args[0].equalsIgnoreCase("sil")) {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanim: /koruma sil <isim>");
                    return true;
                }
                String name = args[1];
                if (regions.remove(name) != null) {
                    saveRegions();
                    player.sendMessage(ChatColor.GREEN + "'" + name + "' korumasi silindi.");
                } else {
                    player.sendMessage(ChatColor.RED + "Bu isimde bir koruma bulunamadi!");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("liste")) {
                if (regions.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Kayitli hicbir koruma bolgesi yok.");
                    return true;
                }
                player.sendMessage(ChatColor.GOLD + "--- Koruma Listesi ---");
                for (Region reg : regions.values()) {
                    player.sendMessage(ChatColor.YELLOW + "- " + reg.name + " | Tur: " + reg.type + " | Oncelik: " + reg.priority);
                }
                return true;
            }
        }

        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) return;

        // Balta ile alan seçimi
        if (item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Koruma Baltasi")) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true);
                pos1Map.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "1. Kose secildi!");
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true);
                pos2Map.put(player.getUniqueId(), event.getClickedBlock().getLocation());
                player.sendMessage(ChatColor.GREEN + "2. Kose secildi!");
            }
        }

        // Kazma ile tekli blok seçimi
        if (item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Tekli Blok Secici")) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                event.setCancelled(true);
                Location loc = event.getClickedBlock().getLocation();
                singleBlocksMap.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(loc);
                player.sendMessage(ChatColor.GREEN + "Blok listeye eklendi! Toplam: " + singleBlocksMap.get(player.getUniqueId()).size());
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zornokoruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        Region targetRegion = getEffectiveRegion(loc);

        if (targetRegion != null) {
            if (targetRegion.type.equals("serbest") || targetRegion.type.equals("maden")) {
                // Serbest veya maden bölgelerinde kırma serbest
                return;
            } else if (targetRegion.type.equals("all") || targetRegion.type.equals("kirma")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Bu alanda blok kirmak yasak! (" + targetRegion.name + ")");
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zornokoruma.bypass")) return;

        Location loc = event.getBlock().getLocation();
        Region targetRegion = getEffectiveRegion(loc);

        if (targetRegion != null) {
            if (targetRegion.type.equals("serbest")) {
                // Serbest bölgede blok koymak da serbest
                return;
            } else if (targetRegion.type.equals("all") || targetRegion.type.equals("maden")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Bu alanda blok koymak yasak! (" + targetRegion.name + ")");
            }
            // kirma türünde koyma serbest kalır
        }
    }

    // Önceliğe göre en yüksek olan korumayı seçer
    private Region getEffectiveRegion(Location loc) {
        List<Region> matching = new ArrayList<>();
        for (Region reg : regions.values()) {
            if (reg.contains(loc)) {
                matching.add(reg);
            }
        }
        if (matching.isEmpty()) return null;

        // Önceliğe göre büyükten küçüğe sırala
        matching.sort((r1, r2) -> Integer.compare(r2.priority, r1.priority));
        return matching.get(0);
    }

    private void saveRegions() {
        getConfig().set("regions", null);
        for (Region reg : regions.values()) {
            String path = "regions." + reg.name;
            getConfig().set(path + ".type", reg.type);
            getConfig().set(path + ".priority", reg.priority);
            if (reg.isCuboid) {
                getConfig().set(path + ".world", reg.worldName);
                getConfig().set(path + ".minX", reg.minX);
                getConfig().set(path + ".minY", reg.minY);
                getConfig().set(path + ".minZ", reg.minZ);
                getConfig().set(path + ".maxX", reg.maxX);
                getConfig().set(path + ".maxY", reg.maxY);
                getConfig().set(path + ".maxZ", reg.maxZ);
            } else {
                List<String> blockStrs = new ArrayList<>();
                for (Location l : reg.customBlocks) {
                    blockStrs.add(l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                getConfig().set(path + ".blocks", blockStrs);
            }
        }
        saveConfig();
    }

    private void loadRegions() {
        regions.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("regions");
        if (sec == null) return;

        for (String name : sec.getKeys(false)) {
            String path = "regions." + name;
            String type = getConfig().getString(path + ".type", "all");
            int priority = getConfig().getInt(path + ".priority", 0);

            if (getConfig().contains(path + ".world")) {
                String world = getConfig().getString(path + ".world");
                int minX = getConfig().getInt(path + ".minX");
                int minY = getConfig().getInt(path + ".minY");
                int minZ = getConfig().getInt(path + ".minZ");
                int maxX = getConfig().getInt(path + ".maxX");
                int maxY = getConfig().getInt(path + ".maxY");
                int maxZ = getConfig().getInt(path + ".maxZ");
                regions.put(name, new Region(name, world, minX, minY, minZ, maxX, maxY, maxZ, type, priority));
            } else {
                List<String> blockStrs = getConfig().getStringList(path + ".blocks");
                Set<Location> blocks = new HashSet<>();
                for (String bStr : blockStrs) {
                    String[] parts = bStr.split(",");
                    if (parts.length == 4) {
                        try {
                            org.bukkit.World w = Bukkit.getWorld(parts[0]);
                            if (w != null) {
                                blocks.add(new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
                            }
                        } catch (Exception ignored) {}
                    }
                }
                regions.put(name, new Region(name, blocks, type, priority));
            }
        }
    }

    private static class Region {
        private String name;
        private String type;
        private int priority;
        private boolean isCuboid;
        
        // Alan (Cuboid) bilgileri
        private String worldName;
        private int minX, minY, minZ, maxX, maxY, maxZ;
        
        // Tekli blok bilgileri
        private Set<Location> customBlocks;

        // Koni / Küp constructor
        public Region(String name, Location l1, Location l2, String type, int priority) {
            this.name = name;
            this.type = type;
            this.priority = priority;
            this.isCuboid = true;
            this.worldName = l1.getWorld().getName();
            this.minX = Math.min(l1.getBlockX(), l2.getBlockX());
            this.minY = Math.min(l1.getBlockY(), l2.getBlockY());
            this.minZ = Math.min(l1.getBlockZ(), l2.getBlockZ());
            this.maxX = Math.max(l1.getBlockX(), l2.getBlockX());
            this.maxY = Math.max(l1.getBlockY(), l2.getBlockY());
            this.maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
        }

        // Yükleme constructor (Cuboid)
        public Region(String name, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String type, int priority) {
            this.name = name;
            this.worldName = worldName;
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.type = type;
            this.priority = priority;
            this.isCuboid = true;
        }

        // Tekli blok constructor
        public Region(String name, Set<Location> customBlocks, String type, int priority) {
            this.name = name;
            this.customBlocks = customBlocks;
            this.type = type;
            this.priority = priority;
            this.isCuboid = false;
        }

        public boolean contains(Location loc) {
            if (isCuboid) {
                if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) return false;
                int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
                return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
            } else {
                if (customBlocks == null) return false;
                for (Location l : customBlocks) {
                    if (l.getWorld().equals(loc.getWorld()) && l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
        }
