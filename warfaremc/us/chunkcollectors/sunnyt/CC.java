package warfaremc.us.chunkcollectors.sunnyt;

import com.google.common.collect.Maps;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import warfaremc.us.chunkcollectors.sunnyyt.FactionsUUIDSupport2;
import warfaremc.us.chunkcollectors.sunnyyt.LockedThreadFactionSupport;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;

public class CC extends JavaPlugin {
    private static CC instance;
    Map<UUID, ChunkCollector> playerEdittingMap;
    private Management management;
    private Util util;
    private Economy econ = null;
    private boolean CanChunkLoader;
    private boolean HasShopAddon;
    private boolean PluginReloading;
    private boolean HeadDatabase;
    private boolean SuperiorSkyBlock2Support;
    private boolean factions;
    private boolean massiveFactions;
    private boolean SavageOutposts;
    private Object HeadDatabaseSupport;
    private Map<Double, Double> sinArray, cosArray;

    public Material naturalMaterial, brokenMaterial;

    public static CC getInstance() {
        return instance;
    }

    public Util getUtil() {
        return util;
    }

    Management getManagement() {
        return management;
    }

    Map<Double, Double> getSinArray() {
        return sinArray;
    }

    Map<Double, Double> getCosArray() {
        return cosArray;
    }

    boolean isMassiveFactions() {
        return massiveFactions;
    }

    boolean isSuperiorSkyBlock2Support() {
        return SuperiorSkyBlock2Support;
    }

    Economy getEcon() {
        return econ;
    }

    @Override
    public void onEnable() {

        getLogger().info("================================");
        instance = this;

        getLogger().info(ChatColor.AQUA + "Downloader ID: %%__USER__%% %%__NONCE__%%");

        final long pluginEnable = System.currentTimeMillis();

        this.sinArray = Maps.newHashMap();
        this.cosArray = Maps.newHashMap();

        for (int x = 0; x < 365; x++) {
            double rads = Math.toRadians(x);

            sinArray.put((double) x, Math.sin(rads));
            cosArray.put((double) x, Math.cos(rads));
        }

        getLogger().info(ChatColor.GREEN + "Cached sin and cos values");

        // loading utilities
        this.util = new Util();
        this.management = new Management(this);
        this.playerEdittingMap = Maps.newHashMap();

        getLogger().info(ChatColor.GREEN + "Loaded Utilities");

        PluginReloading = false;

        loadConfigOptions();

        Bukkit.getPluginManager().registerEvents(new Events(util, this, management), this);
        getLogger().info(ChatColor.GREEN + "Loaded Events");

        loadData();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::saveData, getConfig().getInt("options.save-task-delay") * 20L, getConfig().getInt("options.save-task-delay") * 20L);
        getLogger().info(ChatColor.GREEN + "Scheduled saving task");

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        econ = rsp.getProvider();
        management.getChunkCollectors().forEach(ChunkCollector::check);

        // loading the command
        getCommand("collector").setExecutor(this);

        // Registering Packet Look

        getLogger().info(ChatColor.GOLD + "Plugin Enabled in " + (System.currentTimeMillis() - pluginEnable) + "ms");

        getLogger().info("================================");

    }

    private boolean loadConfigOptions() {

        if (PluginReloading) {
            return false;
        }

        saveDefaultConfig();
        reloadConfig();

        naturalMaterial = Material.getMaterial(getConfig().getString("collector-item.material"));
        brokenMaterial = Material.getMaterial(CC.getInstance().getConfig().getString("collector-item with saved time.material"));

        getLogger().info(ChatColor.GREEN + "Reloaded configuration");

        PluginReloading = true;

        getLogger().info(ChatColor.GREEN + "Unregistering listeners");

        getLogger().info(ChatColor.GREEN + "Confirming chunk loader status");
        this.confirmChunkLoaders();

        getLogger().info(ChatColor.AQUA + "ChunkLoaders: " + String.valueOf(CanChunkLoader).toUpperCase());

        factions = Bukkit.getPluginManager().isPluginEnabled("Factions");

        getLogger().info("Factions plugin found: " + factions);

        if (factions) {
            getLogger().info("Confirming factions");

            try {
                Class.forName("com.massivecraft.factions.FPlayer");
                getLogger().info("Confirmed.");
                massiveFactions = false;
            } catch (ClassNotFoundException e) {
                factions = false;
                massiveFactions = true;
                getLogger().info("Couldn't find core class so no factions?");
            }
        }
        if (factions) {
            Bukkit.getPluginManager().registerEvents(new FactionEvents(management), this);
        }

        if (!factions && massiveFactions) {
            try {
                getLogger().info("Looking for massivecore factions");
                Class.forName("com.massivecraft.factions.TerritoryAccess");
                Class.forName("com.massivecraft.factions.mixin.PowerMixin");
                getLogger().info("Found massivecore factions");
            } catch (ClassNotFoundException ex) {
                getLogger().info("Couldn't find massivecore factions");
                massiveFactions = false;
            }
        }

        if (massiveFactions) {
            Bukkit.getPluginManager().registerEvents(new MassiveFactionEvents(management), this);
        }

        getLogger().info(ChatColor.AQUA + "MassiveFactions: " + String.valueOf(massiveFactions).toUpperCase());
        getLogger().info(ChatColor.AQUA + "Factions: " + String.valueOf(factions).toUpperCase());

        this.HasShopAddon = getConfig().getBoolean("options.shopguiplus.enabled");

        if (this.HasShopAddon) {
            this.HasShopAddon = Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus");
        }

        getLogger().info(ChatColor.AQUA + "ShopGUIPlus: " + String.valueOf(this.HasShopAddon).toUpperCase());

        this.SuperiorSkyBlock2Support = getConfig().getBoolean("SuperiorSkyBlock2.enabled");
        if (SuperiorSkyBlock2Support) {
            this.SuperiorSkyBlock2Support = Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyBlock2");
        }

        getLogger().info(ChatColor.AQUA + "SuperiorSkyBlock2: " + String.valueOf(SuperiorSkyBlock2Support).toUpperCase());

        HeadDatabaseSupport = null;
        this.HeadDatabase = Bukkit.getPluginManager().isPluginEnabled("HeadDatabase");
        if (HeadDatabase) {
            try {
                this.HeadDatabaseSupport = HeadDatabaseSupport.class.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }


        getLogger().info(ChatColor.AQUA + "HeadDatabase: " + String.valueOf(HeadDatabase).toUpperCase());

        SavageOutposts = Bukkit.getPluginManager().isPluginEnabled("Outpost") && Bukkit.getPluginManager().getPlugin("Outpost").getDescription().getMain().equalsIgnoreCase("net.prosavage.outpost.Outpost");

        saveResource("data.yml", false);
        getLogger().info(ChatColor.GREEN + "Loaded data storage file");

        getLogger().info(ChatColor.YELLOW + "(Re)Loaded configuration");

        PluginReloading = false;
        return true;

    }

    private void confirmChunkLoaders() {

        CanChunkLoader = getServer().getVersion().toLowerCase().contains("taco") ||
                getServer().getVersion().toLowerCase().contains("paper") ||
                Package.getPackage("org.github.paperspigot") != null ||
                Package.getPackage("org.github.paper") != null;

        if (CanChunkLoader) {
            try {
                Server server = Bukkit.getServer();
                Method serverHandle;
                serverHandle = server.getClass().getDeclaredMethod("getHandle");
                serverHandle.setAccessible(true);
                Object nmsServer;
                nmsServer = serverHandle.invoke(server);
                Class serverClass;
                serverClass = nmsServer.getClass();
                String className = serverClass.getPackage().getName();
                String[] args = className.split("\\.");
                Class.forName("warfaremc.us.chunkcollectors.sunnyt.chunkloader." + args[3] + ".CustomNPC");
            } catch (Exception ex) {
                CanChunkLoader = false;
            }

        }

        if (CanChunkLoader && !getConfig().getBoolean("chunk-loaders")) {
            CanChunkLoader = false;
        }

    }

    private void loadData() {
        management.getChunkCollectors().clear();

        File data = new File(this.getDataFolder() + File.separator + "data.yml");
        YamlConfiguration dataconfig = YamlConfiguration.loadConfiguration(data);

        ConfigurationSection blocks = dataconfig.getConfigurationSection("blocks");
        if (blocks == null || blocks.getKeys(false).isEmpty()) return;
        for (String key : blocks.getKeys(false)) {
            World world = Bukkit.getWorld(blocks.getString(key + ".world"));
            if (world == null) continue;
            String faction = blocks.getString(key + ".faction");
            int blockx = blocks.getInt(key + ".x");
            int blocky = blocks.getInt(key + ".y");
            int blockz = blocks.getInt(key + ".z");
            int id = blocks.getInt(key + ".id");
            int level = blocks.getInt(key + ".level");
            UUID uuid = UUID.fromString(blocks.getString(key + ".placer"));
            Location loc = new Location(world, blockx, blocky, blockz);
            if (loc.getBlock().getType() == Material.AIR) {
                continue;
            }
            ConfigurationSection da = blocks.getConfigurationSection(key + ".data");

            HashMap<String, Number> d = new HashMap<>();

            List<String> set = new ArrayList<>();
            set.add("timeRemaining");
            set.add("tnt");
            set.addAll(getConfig().getConfigurationSection("collection-prices").getKeys(false));

            for (String ent : set) {
                Number amount = (Number) da.get(ent);
                if (amount == null) {
                    amount = 0;
                }
                d.put(ent, amount);
            }

            management.addCollector(faction, id, uuid, loc, level,
                    d);
        }

    }

    private void saveData() {
        File data = new File(this.getDataFolder() + File.separator + "data.yml");
        YamlConfiguration dataconfig = YamlConfiguration.loadConfiguration(data);
        dataconfig.set("blocks", null);

        ConfigurationSection blocks = dataconfig.createSection("blocks");
        Set<String> loot = getConfig().getConfigurationSection("collection-prices").getKeys(false);
        boolean found = false;
        for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
            found = true;
            if (!chunkCollector.isConfirmed()) continue;
            ConfigurationSection cc = blocks.createSection("" + chunkCollector.getIdentifier());
            cc.set("world", chunkCollector.getLocation().getWorld().getName());
            cc.set("x", chunkCollector.getLocation().getBlockX());
            cc.set("y", chunkCollector.getLocation().getBlockY());
            cc.set("z", chunkCollector.getLocation().getBlockZ());
            cc.set("id", chunkCollector.getIdentifier());
            cc.set("level", chunkCollector.getLevel());
            cc.set("faction", chunkCollector.getOwner());
            cc.set("placer", chunkCollector.getPlacer().toString());

            Map<String, Number> dataMap = Maps.newHashMap();

            dataMap.putAll(chunkCollector.getData());

            for (String l : loot) {
                dataMap.put(l, chunkCollector.getData().getOrDefault(l, 0));
            }

            cc.set("data", dataMap);
        }

        if (found) {
            try {
                dataconfig.save(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {

        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("Cancelled all tasks");
        for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
            getLogger().info("Saving collector: " + chunkCollector.getIdentifier());
            if(chunkCollector.getHologram() != null) {
                chunkCollector.getHologram().delete();
            }
            if (isCanChunkLoader()) {
                chunkCollector.disable();
            }
            chunkCollector.cancelTask();
        }
        final long now = System.currentTimeMillis();
        saveData();
        getLogger().info("Saved data in " + (System.currentTimeMillis() - now) + "ms");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            int amount;
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(util.translate("&4Player is offline"));
                return true;
            }
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                amount = 1;
            }
            // Did checks on player and item#amount

            if (amount > 1000) {
                sender.sendMessage(util.translate(getConfig().getString("too-much-collector")));
                return true;
            }

            for (int x = 0; x < amount; x++) {
                if (target.getInventory().firstEmpty() == -1) {
                    target.getWorld().dropItemNaturally(target.getLocation(), management.getItem());
                } else {
                    target.getInventory().addItem(management.getItem());
                }
            }

            sender.sendMessage(util.translate(getConfig().getString("received").replaceAll("%amount%", String.valueOf(amount))));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            long now = System.currentTimeMillis();
            boolean response = loadConfigOptions();
            if (response) {
                sender.sendMessage(ChatColor.GREEN + "Plugin reloaded successfully in " + ChatColor.RED + (System.currentTimeMillis() - now) + "ms");
            } else {
                sender.sendMessage(ChatColor.RED + "Plugin is already being reloaded, please wait!");
            }
        } else {
            getConfig().getStringList("help").forEach(message -> {
                sender.sendMessage(util.translate(message));
            });
        }
        return false;
    }

    public String getTimeFormat(Number t, ChunkCollector chunkCollector) {
        long secs;

        if (chunkCollector == null) {
            secs = t.longValue();
            secs = secs - System.currentTimeMillis();
        } else {
            if (chunkCollector.isFrozen()) {
                secs = management.cachedLastTimeFrozen.get(chunkCollector);
            } else {
                secs = t.longValue();
                secs = secs - System.currentTimeMillis();
            }
        }
        secs = secs / 1000;

        if (secs <= 0) {
            return "0s";
        }
        // int secs = (int) Math.round((double) milliseconds / 1000); // for millisecs arg instead of secs
        if (secs < 60)
            return secs + "s";
        else {
            long mins = secs / 60;
            long remainderSecs = secs - (mins * 60);
            if (mins < 60) {
                return (mins < 10 ? "0" : "") + mins + "m "
                        + (remainderSecs < 10 ? "0" : "") + remainderSecs + "s";
            } else {
                long hours = mins / 60;
                long remainderMins = mins - (hours * 60);
                return (hours < 10 ? "0" : "") + hours + "h "
                        + (remainderMins < 10 ? "0" : "") + remainderMins + "m "
                        + (remainderSecs < 10 ? "0" : "") + remainderSecs + "s";
            }
        }
    }

    public Inventory getChunkCollectorMenu(ChunkCollector chunkCollector) {
        chunkCollector.check();
        Inventory inventory = Bukkit.createInventory(null, getConfig().getInt("menu.size"),
                ChatColor.translateAlternateColorCodes('&', getConfig().getString("gui-name")));
        if (getConfig().getBoolean("menu.fill.enabled")) {
            for (int x = 0; x < inventory.getSize(); x++) {
                ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) getConfig().getInt("menu.fill.glass-pane-color"));
                if (getConfig().getBoolean("menu.fill.enchanted")) {
                    ItemMeta meta = Bukkit.getItemFactory().getItemMeta(pane.getType());
                    // This bit will not work with 1.7.x lol
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    pane.setItemMeta(meta);
                }
                inventory.setItem(x, pane);
            }
        }

        int mobLootSlot = getConfig().getInt("collection-types.mobloot.slot");
        int chunkLoaderSlot = getConfig().getInt("chunk-loader.menu.slot");
        int tntLootSlot = getConfig().getInt("collection-types.tnt.slot");
        int enableDisableSlot = getConfig().getInt("chunk loader enable disable item.menu.slot");

        if (enableDisableSlot > -1) {
            String string = chunkCollector.isFrozen() ? "enable" : "disable";
            ConfigurationSection section = getConfig().getConfigurationSection("chunk loader enable disable item.menu." + string);
            String name = util.translate(section.getString("name"));
            List<String> lore = new ArrayList<>();
            section.getStringList("lore").forEach(s -> lore.add(util.translate(s)));
            ItemStack itemStack = new ItemStack(Material.getMaterial(section.getString("type")), 1);
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(name);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
            inventory.setItem(enableDisableSlot, itemStack);
        }

        if (chunkLoaderSlot > -1) {
            ItemStack chunkloader = new ItemStack(Material.getMaterial(getConfig().getString("chunk-loader.menu.item")), 1);

            ItemMeta meta = chunkloader.getItemMeta();

            meta.setDisplayName(util.translate(getConfig().getString("chunk-loader.name")));
            List<String> lore = new ArrayList<>();
            for (String l : getConfig().getStringList("chunk-loader.lore")) {
                lore.add(util.translate(StringUtils.replace(l, "%time%", getTimeFormat(chunkCollector.getData().get("timeRemaining"), chunkCollector))));
            }
            meta.setLore(lore);
            chunkloader.setItemMeta(meta);
            inventory.setItem(chunkLoaderSlot, chunkloader);
        }

        if (mobLootSlot > -1) {
            boolean UsedAddon = false;
            ItemStack drops = new ItemStack(Material.getMaterial(getConfig().getString("collection-types.mobloot.item")), 1);
            if (HeadDatabase && getConfig().getInt("collection-types.mobloot.hdb") != -1) {
                try {
                    drops = (ItemStack) HeadDatabaseSupport.getClass().getDeclaredMethod("getHead", int.class).invoke(HeadDatabaseSupport,
                            getConfig().getInt("collection-types.mobloot.hdb"));
                    UsedAddon = true;
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }


            ItemMeta meta = drops.getItemMeta();

            meta.setDisplayName(util.translate(getConfig().getString("collection-type-format.name")));
            List<String> lore = new ArrayList<>();
            for (String l : getConfig().getStringList("collection-type-format.lore")) {
                double total = 0;
                for (String v : chunkCollector.getData().keySet()) {
                    double sale = chunkCollector.getData().get(v).doubleValue();
                    if (!v.equalsIgnoreCase("tnt") && !v.equalsIgnoreCase("timeRemaining")) {
                        if (!HasShopAddon) {
                            total += sale *= (getConfig().getInt("collection-prices." + v + ".price"));
                        } else {
                            total += sale;
                        }
                    }
                    l = StringUtils.replace(l, "%" + v + "sellvalue%", NumberFormat(sale));
                }
                l = StringUtils.replace(l, "%totallootvalue%", NumberFormat(total));
                lore.add(util.translate(l));
            }
            meta.setLore(lore);
            for (String s : getConfig().getStringList("collection-type-format.enchants")) {
                String[] ss = s.split(":");
                int level;
                Enchantment enchant = Enchantment.getByName(ss[0]);
                try {
                    level = Integer.parseInt(ss[1]);
                } catch (NumberFormatException ex) {
                    level = 1;
                }
                meta.addEnchant(enchant, level, true);
            }

            if (getConfig().getBoolean("collection-type-format.hide-enchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (meta instanceof SkullMeta && !UsedAddon) {
                drops.setDurability((short) 3);
                if (getConfig().isString("collection-types.mobloot.owner")) {
                    ((SkullMeta) meta).setOwner(getConfig().getString("collection-types.mobloot.owner"));
                }
            }

            drops.setItemMeta(meta);
            inventory.setItem(mobLootSlot, drops);
        }

        if (tntLootSlot > -1) {
            ItemStack tnt = new ItemStack(Material.getMaterial(getConfig().getString("collection-types.tnt.item")), 1);

            boolean UsedAddon = false;
            if (HeadDatabase && getConfig().getInt("collection-types.tnt.hdb") != -1) {
                try {
                    tnt = (ItemStack) HeadDatabaseSupport.getClass().getDeclaredMethod("getHead", int.class).invoke(HeadDatabaseSupport,
                            getConfig().getInt("collection-types.tnt.hdb"));
                    UsedAddon = true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }

            ItemMeta meta = tnt.getItemMeta();
            meta.setDisplayName(util.translate(getConfig().getString("tnt-type-format.name")));
            List<String> lore = new ArrayList<>();
            for (String l : getConfig().getStringList("tnt-type-format.lore")) {
                int sale = chunkCollector.getData().get("tnt").intValue();
                l = StringUtils.replace(l, "%tntamount%", String.valueOf(sale));
                lore.add(util.translate(l));
            }
            meta.setLore(lore);
            for (String s : getConfig().getStringList("tnt-type-format.enchants")) {
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

            if (getConfig().getBoolean("tnt-type-format.hide-enchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (meta instanceof SkullMeta && !UsedAddon) {
                tnt.setDurability((short) 3);
                if (getConfig().isString("collection-types.tnt.owner")) {
                    ((SkullMeta) meta).setOwner(getConfig().getString("collection-types.tnt.owner"));
                }
            }

            tnt.setItemMeta(meta);
            inventory.setItem(tntLootSlot, tnt);
        }

        return inventory;
    }

    public boolean isShopGuiPlus() {
        return HasShopAddon;
    }

    public String NumberFormat(Number value) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(value);
    }

    public double depositLoot(Player event, ChunkCollector cc, boolean shift) {
        cc.check();
        double total = getTotal(cc);

        cc.resetData(false);

        if (SavageOutposts) {
            double boost = SavageOutpostsSupport.getCappingBoost(event);
            if (boost > 0) {
                total *= boost;
                event.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("SavageOutposts.message").replace("%boost%", "x" + boost)));
            }
        }

        if (Bukkit.getPluginManager().getPlugin("Outpost") != null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("Outpost");
            if (plugin.getDescription().getMain().equalsIgnoreCase("kr.kieran.outpost.OutpostPlugin")) {
                if (plugin.isEnabled()) {
                    total *= OutpostSupport.getBoost(event);
                }
            } else if (plugin.getDescription().getMain().equalsIgnoreCase("com.benzimmer123.outpost.Main")) {
                if (plugin.isEnabled()) {
                    total *= BenzimmerOutpostSupport.getBoost(event, this);
                }
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("WildTools")) {
            total *= WildToolsSupport.getBoost(event);
        }

        econ.depositPlayer(event, total);

        if (!shift) {
//            event.closeInventory();
//            event.openInventory(getChunkCollectorMenu(cc));
//            playerEdittingMap.put(event.getPlayer().getUniqueId(), cc);
            reset(event.getPlayer().getOpenInventory().getTopInventory(), cc);

            if (total > 0) {
                event.sendMessage(util.translate(getConfig().getString("depositing.collectors.deposit-loot.text").replace("%totalsellvalue%", NumberFormat(total))));
                event.playSound(event.getLocation(), Sound.valueOf(getConfig().getString("depositing.collectors.deposit-loot.sound")), 5, 10);
            }
        }

        return total;
    }

    public int depositTnt(Player event, ChunkCollector cc, boolean shift) {
        cc.check();
        int amount = cc.getData().get("tnt").intValue();
        int received = 0;

        if (amount == 0) {
            if (!shift) {
//                event.closeInventory();
//                event.openInventory(getChunkCollectorMenu(cc));
//                playerEdittingMap.put(event.getPlayer().getUniqueId(), cc);
                reset(event.getPlayer().getOpenInventory().getTopInventory(), cc);

            }
            return 0;
        }

        int physical = 1;

        if (getConfig().getBoolean("options.tnt-deposit") && instance.isFactions()) {
            if (getConfig().getBoolean("LockedThreadFactions")) {
                physical = LockedThreadFactionSupport.depositTnt(event, amount, util, this);
            } else if (getConfig().getBoolean("FactionsUUID")) {
                physical = FactionsUUIDSupport2.depositTnt(event, amount, util, this);
            } else {
                physical = FactionsSupport.depositTnt(event, amount, util, this);
            }
        }


        if (physical == 1) {
            int stacksOfTnt = amount / 64;
            int remainingTnt = amount % 64;
            ItemStack remaining = new ItemStack(Material.TNT, remainingTnt);
            ItemStack stack = new ItemStack(Material.TNT, 64);

            int amountUnreceived = 0;

            for (int x = 0; x < stacksOfTnt; x++) {
                if (event.getInventory().firstEmpty() == -1) {
                    amountUnreceived += 64;
                } else {
                    event.getInventory().addItem(stack);
                }
            }

            if (event.getInventory().firstEmpty() == -1) {
                amountUnreceived += remainingTnt;
            } else {
                event.getInventory().addItem(remaining);
            }

            received = amount - amountUnreceived;

            cc.setData("tnt", amountUnreceived);
        } else {
            cc.resetData(true);
        }

        if (!shift) {
//            event.closeInventory();
//            event.openInventory(getChunkCollectorMenu(cc));
//            playerEdittingMap.put(event.getPlayer().getUniqueId(), cc);

            reset(event.getPlayer().getOpenInventory().getTopInventory(), cc);

            event.sendMessage(util.translate(getConfig().getString("deposit-tnt.text").replaceAll("%tntamount%", String.valueOf(physical == 1 ? received : amount))));
            event.playSound(event.getLocation(), Sound.valueOf(getConfig().getString("deposit-tnt.sound")), 5, 10);
        }

        return physical == 1 ? received : amount;

    }

    private void reset(Inventory clickedInventory, ChunkCollector cc) {
        Inventory newInventory = getChunkCollectorMenu(cc);

        for (int i = 0; i < clickedInventory.getSize(); i++) {
            clickedInventory.setItem(i, newInventory.getItem(i));
        }
    }

    public boolean isFactions() {
        return factions;
    }

    public boolean isCanChunkLoader() {
        return CanChunkLoader;
    }

    public double getTotal(ChunkCollector cc) {
        double[] total = new double[]{0};
        if (!HasShopAddon) {
            cc.getData().forEach((s, integer) -> total[0] += (integer.doubleValue() * (getConfig().getInt("collection-prices." + s + ".price"))));
        } else {
            cc.getData().forEach((s, integer) -> {
                if (!s.equalsIgnoreCase("timeRemaining") && !s.equalsIgnoreCase("tnt")) {
                    total[0] += integer.doubleValue();
                }
            });
        }
        return total[0];
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug-mode");
    }

    public String getData(ChunkCollector cc) {
        long timeRemaining = (cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis());
        if (timeRemaining <= 0) {
            timeRemaining = 0;
        }
        return getTotal(cc) + ":" + cc.getData().get("tnt") + ":" + timeRemaining;
    }
}