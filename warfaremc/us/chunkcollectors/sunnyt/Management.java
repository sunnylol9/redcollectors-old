package warfaremc.us.chunkcollectors.sunnyt;

import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.google.common.collect.Maps;
import de.tr7zw.redcollectors.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Management {
    public List<ChunkCollector> chunkCollectors;

    public Map<ChunkCollector, Long> cachedLastTimeFrozen;
    public Map<ChunkCollector, Long> cachedLastSystemTime;

    public Map<ChunkCollector, String> cachedLastCollectorValue;

    public ItemStack ccItem;
    public CC pl;

    public Management(CC pl) {
        this.pl = pl;
        this.ccItem = loadItem();
        this.chunkCollectors = new ArrayList<>();
        this.cachedLastTimeFrozen = Maps.newHashMap();
        this.cachedLastSystemTime = Maps.newHashMap();
        this.cachedLastCollectorValue = Maps.newHashMap();
    }

    public ItemStack loadItem() {
        ConfigurationSection item = pl.getConfig().getConfigurationSection("collector-item");
        String name = ChatColor.translateAlternateColorCodes('&', item.getString("name"));
        List<String> lore = new ArrayList<>();
        List<String> enchants = item.getStringList("enchants");
        Material type = Material.getMaterial(item.getString("material"));

        boolean hide = item.getBoolean("hide-enchants");

        ItemStack itemStack = new ItemStack(type, 1);
        for (String l : item.getStringList("lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l));
        }
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(type);
        meta.setLore(lore);
        meta.setDisplayName(name);

        for (String s : enchants) {
            String[] ss = s.split(":");
            Enchantment enchant = Enchantment.getByName(ss[0]);
            int level;
            try {
                level = Integer.parseInt(ss[1]);
            } catch (NumberFormatException ex) {
                level = 1;
            }
            meta.addEnchant(enchant, level, true);
        }

        if (hide) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.setBoolean("redCollector", true);

        return nbtItem.getItem();
    }

    public ItemStack getItem() {
        return ccItem;
    }

    public List<ChunkCollector> getChunkCollectors() {
        return this.chunkCollectors;
    }

    public ChunkCollector findCollector(int identifier) {
        for (ChunkCollector chunkCollector : chunkCollectors) {
            if (chunkCollector.getIdentifier() == identifier) {
                return chunkCollector;
            }
        }
        return null;
    }

    public void addCollector(String holder, int identifier, UUID placer, Location location, int level, HashMap<String, Number> data) {

        ChunkCollector cc = new ChunkCollector(holder, identifier, placer, location, level, data);
        if (!cc.isConfirmed()) return;

        if (CC.getInstance().isCanChunkLoader() && data.getOrDefault("timeRemaining", -1).longValue() == -1) {
            cc.setData("timeRemaining", System.currentTimeMillis());
            cc.addData("timeRemaining", pl.getConfig().getInt("chunk-loader.default-time") * 1000);
        }

        BukkitTask holoTask = Bukkit.getScheduler().runTaskTimer(pl, () -> {

            if (cc.getHologram() != null && !cc.getHologram().isDeleted()) {
                if (cachedLastCollectorValue.containsKey(cc)) {
                    if (cachedLastCollectorValue.get(cc).equalsIgnoreCase(pl.getData(cc))) {
                        return;// AVOID UPDATES
                    }
                }

                /*
                (ChatColor.translateAlternateColorCodes('&', pl.getConfig().getString("Hologram.holo-text")
                        .replace("%total%", DecimalFormat.getInstance().format(pl.getTotal(cc))).replace("%tnt%", cc.getData().get("tnt").toString())
                        .replace("%time%", pl.getTimeFormat(cc.getData().get("timeRemaining"), cc))));
                 */
                List<String> newData = pl.getConfig().getStringList("Hologram.holo-text").stream()
                        .map(text -> text.replace("%total%", DecimalFormat.getInstance().format(pl.getTotal(cc))))
                        .map(text -> text.replace("%tnt%", cc.getData().get("tnt").toString()))
                        .map(text -> text.replace("%time%", pl.getTimeFormat(cc.getData().get("timeRemaining"), cc)))
                        .map(text -> ChatColor.translateAlternateColorCodes('&', text)).collect(Collectors.toList());

                for (int i = 0; i < newData.size(); i++) {
                    if (cc.getHologram().size() > i) {
                        if (ChatColor.stripColor(cc.getHologram().getLine(i).toString()) != newData.get(i)) {
                            TextLine textLine = (TextLine) cc.getHologram().getLine(i);
                            textLine.setText(newData.get(i));
                        }
                    } else {
                        cc.getHologram().appendTextLine(newData.get(i));
                    }
                }

                cachedLastCollectorValue.put(cc, pl.getData(cc));
            }

        }, 0L, 20L);

        cc.setHologramTask(holoTask);

        chunkCollectors.add(cc);

        cc.check();
    }


    public void delCollector(int identifier) {
        ChunkCollector chunkCollector = findCollector(identifier);
        //    chunkCollector.cancelTask();
        chunkCollectors.remove(chunkCollector);
    }

//    public boolean isNearby(Location location) {
//        for (ChunkCollector chunkCollector : chunkCollectors) {
//            if (!chunkCollector.isConfirmed()) continue;
//            if (!location.getWorld().getName().equalsIgnoreCase(chunkCollector.getLocation().getWorld().getName()))
//                continue;
//            if (location.getChunk().getX() == chunkCollector.getLocation().getChunk().getX() &&
//                    location.getChunk().getZ() == chunkCollector.getLocation().getChunk().getZ()) {
//                return true;
//            }
//        }
//        return false;
//    }


    public ChunkCollector getNearby(Location location) {
        for (ChunkCollector chunkCollector : chunkCollectors) {
            if (!chunkCollector.isConfirmed()) continue;
            if (!location.getWorld().getUID().equals(chunkCollector.getWorld()))
                continue;
            Chunk chunk = location.getChunk();
            if (chunk.getX() == chunkCollector.getChunkX() &&
                    chunk.getZ() == chunkCollector.getChunkZ()) {
                return chunkCollector;
            }
        }
        return null;
    }

    public ItemStack getItem(HashMap<String, Number> data) {
        ConfigurationSection item = pl.getConfig().getConfigurationSection("collector-item with saved time");
        String name = ChatColor.translateAlternateColorCodes('&', item.getString("name").replace("%time%", pl.getTimeFormat(data.get("timeRemaining"), null)));
        List<String> lore = new ArrayList<>();
        List<String> enchants = item.getStringList("enchants");
        Material type = Material.getMaterial(item.getString("material"));

        boolean hide = item.getBoolean("hide-enchants");

        ItemStack itemStack = new ItemStack(type, 1);
        for (String l : item.getStringList("lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l.replace("%time%", pl.getTimeFormat(data.get("timeRemaining"), null))));
        }
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(type);
        meta.setLore(lore);
        meta.setDisplayName(name);

        for (String s : enchants) {
            String[] ss = s.split(":");
            Enchantment enchant = Enchantment.getByName(ss[0]);
            int level;
            try {
                level = Integer.parseInt(ss[1]);
            } catch (NumberFormatException ex) {
                level = 1;
            }
            meta.addEnchant(enchant, level, true);
        }

        if (hide) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(itemStack);
        nbtItem.setBoolean("redCollector", true);

        return nbtItem.getItem();
    }
}
