package warfaremc.us.chunkcollectors.sunnyt;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.google.common.collect.Maps;
import de.tr7zw.redcollectors.nbtapi.NBTItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Events implements Listener {

    private Management management;
    private CC plugin;
    private Util util;

    private Map<UUID, ChunkCollector> lastViewed;
    private Map<UUID, Long> lastShiftClick;
    private long now;
    private int i;

    public Events(Util util, CC plugin, Management management) {
        this.management = management;
        this.plugin = plugin;
        this.lastViewed = Maps.newHashMap();
        this.lastShiftClick = Maps.newHashMap();
        this.util = util;
    }

    public String translate(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!event.isCancelled() && event.getPlayer().getItemInHand() != null) {
            NBTItem itemInHand = new NBTItem(event.getPlayer().getItemInHand());
            if (!itemInHand.hasNBTData()) return;
            if (!itemInHand.getItem().hasItemMeta()) return;
            if (!itemInHand.hasKey("redCollectorTimeRemaining") &&
                    !itemInHand.hasKey("redCollector")) return;

            long timeInside = itemInHand.hasKey("redCollectorTimeRemaining") ? itemInHand.getLong("redCollectorTimeRemaining") : 0;
            HashMap<String, Number> dataa = Maps.newHashMap();
            dataa.put("timeRemaining", timeInside);

            boolean normalItem = event.getPlayer().getItemInHand().getType().equals(management.getItem().getType()) &&
                    event.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(management.getItem().getItemMeta().getDisplayName());
            boolean brokenItem = event.getPlayer().getItemInHand().getType().equals(management.getItem(dataa).getType()) &&
                    event.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(management.getItem(dataa).getItemMeta().getDisplayName());

            if ((normalItem) || brokenItem) {

                List<ChunkCollector> collectorsToRemove = new ArrayList<>();

                for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
                    // CHeck for errors/bugs incase a collector was not removed from hashmap
                    if (!chunkCollector.isConfirmed()) {
                        collectorsToRemove.add(chunkCollector);
                    }
                }

                collectorsToRemove.forEach(chunkCollector -> {
                    chunkCollector.cancelTask();
                    chunkCollector.setFrozen(false);
                    management.delCollector(chunkCollector.getIdentifier());
                });

                if (plugin.getConfig().getBoolean("options.only-in-claim-placement")) {
                    if (plugin.isFactions()) {
                        boolean inClaim = FactionsSupport.isInOwnClaim(event.getPlayer());
                        if (!inClaim) {
                            event.getPlayer().sendMessage(util.translate(plugin.getConfig().getString("can only place in faction claims")));
                            event.setCancelled(true);
                            event.setBuild(false);
                            return;
                        }
                    } else if (plugin.isMassiveFactions()) {
                        boolean inClaim = MassiveFactionEvents.isClaimed(event.getPlayer());
                        if (!inClaim) {
                            event.getPlayer().sendMessage(util.translate(plugin.getConfig().getString("can only place in faction claims")));
                            event.setCancelled(true);
                            event.setBuild(false);
                            return;
                        }
                    }
                }

                int currentlyPlaced = 0;
                int maxAllowed = 0;

                // Can player place a collector?
                for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
                    Chunk chunk = chunkCollector.getLocation().getChunk();
                    if (chunk.equals(event.getBlock().getLocation().getChunk())) {
                        event.getPlayer().sendMessage(translate(plugin.getConfig().getString("too-many")));
                        event.setCancelled(true);
                        return;
                    }
                    if (chunkCollector.getPlacer().equals(event.getPlayer().getUniqueId())) {
                        currentlyPlaced++;
                    }
                }


                if (!event.getPlayer().isOp()) {

                    for (PermissionAttachmentInfo permission : event.getPlayer().getEffectivePermissions()) {
                        if (!permission.getValue()) continue;
                        String perm = permission.getPermission();
                        if (perm.startsWith("redcollectors.canplace.")) {
                            int x = Integer.parseInt(perm.replace("redcollectors.canplace.", ""));
                            if (x > maxAllowed) {
                                maxAllowed = x;
                            }
                        }
                    }
                } else {
                    maxAllowed = Integer.MAX_VALUE;
                }

                if (currentlyPlaced >= maxAllowed) {
                    event.getPlayer().sendMessage(translate(plugin.getConfig().getString("no permission for more collectors")));
                    event.setCancelled(true);
                    return;
                }

                String tag = "None";
                if (plugin.isFactions()) {
                    tag = FactionsSupport.getCurrentFaction(event.getPlayer());
                } else if (plugin.isMassiveFactions()) {
                    tag = MassiveFactionsSupport.getCurrentFaction(event.getPlayer());
                }

                HashMap<String, Number> data = new HashMap<>();

                List<String> set = new ArrayList<>();
                set.add("timeRemaining");
                set.add("tnt");
                set.addAll(plugin.getConfig().getConfigurationSection("collection-prices").getKeys(false));

                for (String ent : set) {
                    data.put(ent, 0);
                }

                ItemStack itemStack = event.getItemInHand();
                NBTItem nbtItem = new NBTItem(itemStack);

                if (nbtItem.hasKey("redCollectorTimeRemaining")) {
                    data.put("timeRemaining", System.currentTimeMillis() + nbtItem.getLong("redCollectorTimeRemaining"));
                } else {
                    data.put("timeRemaining", -1);
                }

                management.addCollector(tag,
                        ThreadLocalRandom.current().nextInt(0, 1000000), event.getPlayer().getUniqueId(), event.getBlock().getLocation(), 1,
                        data);
                event.getPlayer().sendMessage(translate(plugin.getConfig().getString("place")));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {


//        if (event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.getMaterial(CC.getInstance().getConfig().getString("collector-item.material")) ||
//                event.getClickedBlock().getType() == Material.getMaterial(CC.getInstance().getConfig().getString("collector-item with saved time.material"))) &&
//                event.getClickedBlock().isLiquid() && event.getClickedBlock().isEmpty()) {
//            sendMessage(event.getPlayer(), "Message: 100");
//            return;
//        }

        if (event.isCancelled()) {
            sendMessage(event.getPlayer(), "Message: 101");
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_BLOCK) {

            Location location = event.getClickedBlock().getLocation();
            ChunkCollector cc = management.getNearby(location);

//            ChunkCollector cc = null;
//            for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
//                if (nearby != null && nearby.equals(chunkCollector)) {
//                    if (!chunkCollector.getLocation().getWorld().getName().equalsIgnoreCase(location.getWorld().getName()))
//                        continue;
//                    int ax = location.getBlockX();
//                    int ay = location.getBlockY();
//                    int az = location.getBlockZ();
//                    int bx = chunkCollector.getLocation().getBlockX();
//                    int by = chunkCollector.getLocation().getBlockY();
//                    int bz = chunkCollector.getLocation().getBlockZ();
//
//                    if (ax == bx && ay == by && az == bz) {
//                        cc = chunkCollector;
//                        break;
//                    }
//                }
//            }

            if (cc != null && location.equals(cc.getLocation())) {

                if (!cc.isConfirmed()) {
                    sendMessage(event.getPlayer(), "Message: 103");
                    return;
                }

                boolean confirmed;

                if (plugin.isFactions()) {
                    confirmed = FactionsSupport.isConfirmed(event.getPlayer(), cc, plugin, util);
                    sendMessage(event.getPlayer(), "Message: 104");
                } else if (plugin.isSuperiorSkyBlock2Support()) {
                    confirmed = SuperiorSkyblockSupport.isConfirmed(event.getPlayer(), cc, plugin, util);
                    sendMessage(event.getPlayer(), "Message: 105");
                } else if (plugin.isMassiveFactions()) {
                    confirmed = MassiveFactionsSupport.isConfirmed(event.getPlayer(), cc, plugin, util);
                    sendMessage(event.getPlayer(), "Message: N104");
                } else {
                    confirmed = NonFactionSupport.isConfirmed(event.getPlayer(), cc, plugin, util);
                    sendMessage(event.getPlayer(), "Message: 106");
                }

                if (!confirmed) {
                    sendMessage(event.getPlayer(), "Message: 108");
                    event.setCancelled(true);
                    return;
                }
                if (!event.getPlayer().isSneaking() && event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    return;
                }

                if (!event.getPlayer().isSneaking() || (event.getPlayer().isSneaking() && plugin.getConfig().getBoolean("disable shiftclicking"))) {
                    sendMessage(event.getPlayer(), "Message: 111");
                    Inventory inventory = plugin.getChunkCollectorMenu(cc);
                    sendMessage(event.getPlayer(), "Message: 112");
                    event.setCancelled(true);
                    event.setUseItemInHand(Event.Result.DENY);
                    event.setUseInteractedBlock(Event.Result.DENY);
                    event.getPlayer().openInventory(inventory);
                    plugin.playerEdittingMap.put(event.getPlayer().getUniqueId(), cc);
                } else {

                    if (event.getPlayer().hasPermission("redcollectors.shiftclick") && !plugin.getConfig().getBoolean("disable shiftclicking")) {
                        sendMessage(event.getPlayer(), "Message: 113");
                        //
                        if (lastShiftClick.containsKey(event.getPlayer().getUniqueId())) {
                            if (System.currentTimeMillis() - lastShiftClick.get(event.getPlayer().getUniqueId()) <=
                                    plugin.getConfig().getInt("shift click delay in milliseconds")) {
                                event.getPlayer().sendMessage(translate(plugin.getConfig().getString("shift click delay")));
                                event.setCancelled(true);
                                return;
                            }
                        }

                        lastShiftClick.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());

                        double value = plugin.depositLoot(event.getPlayer(), cc, true);
                        int tnt = plugin.depositTnt(event.getPlayer(), cc, true);
                        event.setCancelled(true);

                        util.send(event.getPlayer(), translate(plugin.getConfig().getString("depositing.collectors.deposit-all.message.title")
                                        .replace("%totalsellvalue%", plugin.NumberFormat(value))
                                        .replace("%tntamount%", String.valueOf(tnt))), translate(plugin.getConfig().getString("depositing.collectors.deposit-all.message.subtitle")
                                        .replace("%totalsellvalue%", plugin.NumberFormat(value))
                                        .replace("%tntamount%", String.valueOf(tnt))),
                                plugin.getConfig().getInt("depositing.collectors.deposit-all.message.fadeIn"),
                                plugin.getConfig().getInt("depositing.collectors.deposit-all.message.show"),
                                plugin.getConfig().getInt("depositing.collectors.deposit-all.message.fadeOut"));
                        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.valueOf(plugin.getConfig().getString("depositing.collectors.deposit-all.sound")), 5, 10);

                        for (String particle : plugin.getConfig().getStringList("depositing.collectors.deposit-all.particles")) {
                            for (double x = 0; x < 360; x++) {
                                event.getPlayer().playEffect(event.getPlayer().getLocation().clone().add(plugin.getSinArray().get(x),
                                        event.getPlayer().getLocation().getY() - cc.getLocation().getY(), plugin.getCosArray().get(x)), Effect.valueOf(particle), null);
                            }
                        }

                    } else if (event.getPlayer().isSneaking() && !event.getPlayer().hasPermission("redcollectors.shiftclick") && !plugin.getConfig().getBoolean("disable shiftclicking")) {
                        event.getPlayer().sendMessage(translate(plugin.getConfig().getString("no-donater-perk")));
                        event.setCancelled(true);
                        sendMessage(event.getPlayer(), "Message: 114");
                    }
                }
            }
        }
    }

    private void sendMessage(Player player, String s) {
        // DEBUGGING PURPOSES
//        if (!player.getName().equalsIgnoreCase("KoidUserHere")) return;
//        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&4DEBUG &8> &7" + s + " &c(SHOW TO DEV)"));
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {

        if (!plugin.getConfig().getBoolean("enable-explosion-check")) return;

        if (!event.isCancelled()) {
            List<Block> removalFromExplosion = new ArrayList<>();

            for (Block block : event.blockList()) {
                Location location = block.getLocation();
                ChunkCollector cc = CC.getInstance().getManagement().getNearby(location);
//                boolean nearby = management.isNearby(block.getLocation());
//                if (!nearby) continue;
//                for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
//                    if (!chunkCollector.isConfirmed()) continue;
//                    if (chunkCollector.getLocation().equals(location)) {
//                        cc = chunkCollector;
//                    }
//                }

                if (cc != null && cc.getLocation().equals(location)) {
                    removalFromExplosion.add(block);
                    cc.setFrozen(false);
                    if (ThreadLocalRandom.current().nextInt(0, 101) <= plugin.getConfig().getInt("options.drop-rate")) {
                        if (cc.getData().get("timeRemaining").longValue() > -1) {
                            Item item = block.getLocation().getWorld().dropItemNaturally(block.getLocation(), management.getItem(cc.getData()));
                            ItemStack itemStack = item.getItemStack();

                            NBTItem nbtItem = new NBTItem(itemStack);
                            nbtItem.setLong("redCollectorTimeRemaining", cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis());
                            nbtItem.setBoolean("redCollector", true);

                            item.setItemStack(nbtItem.getItem());

                        } else {
                            block.getLocation().getWorld().dropItemNaturally(block.getLocation(), management.getItem());
                        }

                    }
                    cc.disable();
                    cc.cancelTask();
                    management.delCollector(cc.getIdentifier());
                }
            }

            for (Block block : removalFromExplosion) {
                event.blockList().remove(block);
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!plugin.playerEdittingMap.containsKey(event.getPlayer().getUniqueId())) return;
        plugin.playerEdittingMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) {

//            ChunkCollector cc = null;
            Location location = event.getBlock().getLocation();
            ChunkCollector cc = management.getNearby(location);
//            for (ChunkCollector chunkCollector : management.getChunkCollectors()) {
//                if (nearby != null && nearby.equals(chunkCollector)) {
//                    if (!chunkCollector.getLocation().getWorld().getName().equalsIgnoreCase(location.getWorld().getName()))
//                        continue;
//                    int ax = location.getBlockX();
//                    int ay = location.getBlockY();
//                    int az = location.getBlockZ();
//                    int bx = chunkCollector.getLocation().getBlockX();
//                    int by = chunkCollector.getLocation().getBlockY();
//                    int bz = chunkCollector.getLocation().getBlockZ();
//                    if (ax == bx && ay == by && az == bz) {
//                        cc = chunkCollector;
//                        break;
//                    }
//                }
//            }

            if (cc != null && cc.getLocation().equals(location)) {

                if (!cc.isConfirmed()) return;
                event.setCancelled(true);

                if (event.getPlayer().getGameMode() != GameMode.CREATIVE && plugin.getConfig().getBoolean("silk-mining")) {

                    ItemStack hand = event.getPlayer().getItemInHand();
                    if (!hand.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
                        event.getPlayer().sendMessage(util.translate(plugin.getConfig().getString("silktouch is required")));
                        if (plugin.getConfig().getBoolean("no silk so break collector")) {
                            cc.setFrozen(false);
                            event.getBlock().setType(Material.AIR);
                            if (plugin.isCanChunkLoader()) {
                                cc.disable();
                            }
                            cc.cancelTask();
                            management.delCollector(cc.getIdentifier());
                        }
                        return;
                    }
                    if (plugin.getConfig().getBoolean("silk mining type.enabled")) {
                        boolean foundItem = false;
                        for (String type : plugin.getConfig().getStringList("silk mining type.tools")) {
                            if (hand.getType().equals(Material.getMaterial(type))) {
                                foundItem = true;
                                break;
                            }
                        }
                        if (!foundItem) {
                            event.getPlayer().sendMessage(util.translate(plugin.getConfig().getString("silk mining type.could not perform")));
                            if (plugin.getConfig().getBoolean("no silk so break collector")) {
                                cc.setFrozen(false);
                                event.getBlock().setType(Material.AIR);
                                if (plugin.isCanChunkLoader()) {
                                    cc.disable();
                                }
                                cc.cancelTask();
                                management.delCollector(cc.getIdentifier());
                            }
                            return;
                        }
                    }
                }
                cc.setFrozen(false);
                long time = cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis();
                if (time > 0) {

                    Item item = event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), management.getItem(cc.getData()));
                    ItemStack itemStack = item.getItemStack();

                    NBTItem nbtItem = new NBTItem(itemStack);
                    nbtItem.setLong("redCollectorTimeRemaining", time);
                    nbtItem.setBoolean("redCollector", true);

                    item.setItemStack(nbtItem.getItem());
                } else {
                    event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), management.getItem());
                }

                event.getBlock().setType(Material.AIR);
                if (plugin.isCanChunkLoader()) {
                    cc.disable();
                }
                cc.cancelTask();
                management.delCollector(cc.getIdentifier());
                event.getPlayer().sendMessage(translate(plugin.getConfig().getString("break")));
            }
        }
    }

    private void outputLag() {
//        if (now == 0) {
//            now = System.currentTimeMillis();
//        }
//
//        Bukkit.broadcastMessage((System.currentTimeMillis() - now) + "ms " + i);
//        i++;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(SpawnerSpawnEvent event) {
        now = System.currentTimeMillis();
        Location loc = event.getSpawner().getLocation();
        outputLag();
//        sendBroadcast("SpawnerPreSpawn", loc.getChunk());
        if (event.isCancelled()) return;
//        sendBroadcast("SpawnerPreSpawn Not Cancelled", loc.getChunk());
        outputLag();

        ChunkCollector chunkCollector = management.getNearby(loc);
        if (chunkCollector != null) {
//            sendBroadcast("Found ChunkCollector", loc.getChunk());
            outputLag();
            if (!chunkCollector.isConfirmed()) return;
//            sendBroadcast("Found Confirmed ChunkCollector", loc.getChunk());
            outputLag();
            chunkCollector.check();
            String type = event.getEntityType().name();
            int amount;
            outputLag();
            if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
//                sendBroadcast("Found WildStacker", loc.getChunk());
                amount = WildStackerSupport.getAmount(event.getSpawner()) == 0 ? 1 : WildStackerSupport.getAmount(
                        event.getSpawner());
            } else if (Bukkit.getPluginManager().isPluginEnabled("AdvancedSpawners")) {
                amount = AdvancedSpawnerSupport.getAmount(event.getSpawner()) == 0 ? 1 : AdvancedSpawnerSupport.getAmount(event.getSpawner());
            } else {
                amount = 1;
            }

            if (CC.getInstance().isSuperiorSkyBlock2Support()) {
                amount *= SuperiorSkyblockSupport.getMobLootMultiplier(loc);
            }

            outputLag();
//            sendBroadcast("Spawn Amount: " + amount, loc.getChunk());
            boolean canApply = applyLoot(type, chunkCollector.getData().keySet(), chunkCollector, amount, loc.getChunk());
//            sendBroadcast("Add to collector: " + canApply, loc.getChunk());
            outputLag();
            event.setCancelled(canApply || event.isCancelled());
        } else {
//            sendBroadcast("COULDNT FIND CHUNK COLLECTOR", loc.getChunk());

            ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawners dont work unless collector is in chunk");
            if (section.getKeys(false).isEmpty()) return;
            if (!section.getConfigurationSection("worlds").getKeys(false).contains(loc.getWorld().getName())) return;
//            sendBroadcast(section.getConfigurationSection("worlds").getKeys(false).toString(), loc.getChunk());
//            sendBroadcast("looking at the 'spawners dont work unless...'", loc.getChunk());
//
            for (String s : section.getStringList("worlds." + loc.getWorld().getName() + ".whitelist")) {
//                sendBroadcast("s:"+s, loc.getChunk());
                if (s.equalsIgnoreCase(event.getEntityType().name())) {
//                    sendBroadcast("worlds does whitelist the mob: " + event.getEntityType(), loc.getChunk());
                    return;
                }
            }
//            sendBroadcast("worlds does not whitelist the mob: " + event.getEntityType() + " so DELETE", loc.getChunk());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpawnerSpawn(com.bgsoftware.wildstacker.api.events.SpawnerSpawnEvent event) {
        SpawnerSpawnEvent spawnerSpawnEvent = new SpawnerSpawnEvent(event.getEntity().getLivingEntity(), event.getSpawner().getSpawner());
        onSpawn(spawnerSpawnEvent);
        if (spawnerSpawnEvent.isCancelled()) {
            event.getEntity().remove();
        }
    }

    private void sendBroadcast(String couldnt_find_chunk_collector, Chunk chunk) {
////        Bukkit.getOnlinePlayers().forEach(player -> sendMessage(player, couldnt_find_chunk_collector));
//        if (Bukkit.getPlayer("KoidUserHere") != null && Bukkit.getPlayer("KoidUserHere").getLocation().getChunk().equals(chunk)) {
//            sendMessage(Bukkit.getPlayer("KoidUserHere"), couldnt_find_chunk_collector);
//        }
    }

    private boolean applyLoot(String type, Set<String> keys, ChunkCollector chunkCollector, int amount, Chunk chunk) {
        outputLag();
        String nicename = getNiceName(type);
        if (plugin.getConfig().getBoolean("options.tnt-drop") && type.equalsIgnoreCase(EntityType.CREEPER.name()) &&
                ThreadLocalRandom.current().nextInt(0, 100 + 1) <= plugin.getConfig().getInt("options.tnt-drop-chance")) {
            int tnt = 1;
            if (plugin.getConfig().getInt("options.tnt-per-creeper") > 1) {
                tnt = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.tnt-per-creeper"));
            }
            outputLag();
            chunkCollector.addData("tnt", amount * tnt);
            return true;
        }
        outputLag();
//        sendBroadcast("MobName: " + nicename, chunk);
        outputLag();


        if (keys.contains(nicename)) {
//            sendBroadcast("Found: " + nicename, chunk);
            if (!plugin.isShopGuiPlus()) {
//                sendBroadcast("Added!!", chunk);
                chunkCollector.addData(nicename, amount);
                outputLag();
                return true;
            } else {
                try {
                    List<ItemStack> drops = new ArrayList<>();

                    int maxdrop = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.shopguiplus.mobs." + nicename + ".max-items-dropped") + 1);

                    maxdrop = maxdrop * amount;

                    List<String> list = plugin.getConfig().getStringList("options.shopguiplus.mobs." + nicename + ".items");

                    for (String mat : list) {
                        if (ThreadLocalRandom.current().nextInt(0, 100) <= 100 / list.size()) {
                            Material material;
                            int amt;

                            try {
                                material = Material.getMaterial(mat.split(":")[0]);
                                amt = Integer.parseInt(mat.split(":")[1]);
                                amt = ThreadLocalRandom.current().nextInt(1, amt + 1);
                            } catch (Exception ex) {
                                material = Material.AIR;
                                amt = 1;
                            }

                            drops.add(new ItemStack(material, amt * maxdrop));
                        }
                    }

                    double a = ShopGuiPlusSupport.getSellPrice(Bukkit.getOnlinePlayers().stream().findFirst().get(), drops);
                    chunkCollector.addData(nicename, a);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        }
        return false;
    }

    private String getNiceName(String type) {
        type = type.replace("_", "");
        type = type.toLowerCase();
        return type;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLootDrop(ItemSpawnEvent event) {
        if (event.isCancelled()) return;
        if (plugin.getConfig().getBoolean("disable-throwing")) return;
        if (event.getEntity().getItemStack().hasItemMeta()) return;
        // CACTUS

        String nicename = getNiceName(event.getEntity().getItemStack().getType().name());
        ChunkCollector chunkCollector = management.getNearby(event.getLocation());
        if (chunkCollector != null) {
            if (!chunkCollector.isConfirmed()) return;
            if (!chunkCollector.getData().containsKey(nicename)) return;
            chunkCollector.check();
            if (!plugin.isShopGuiPlus()) {
                chunkCollector.addData(nicename, event.getEntity().getItemStack().getAmount());
                event.getEntity().remove();
            } else {
                try {
                    List<ItemStack> drops = new ArrayList<>();

                    int maxdrop = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.shopguiplus.mobs." + nicename + ".max-items-dropped") + 1);

                    List<String> list = plugin.getConfig().getStringList("options.shopguiplus.mobs." + nicename + ".items");

                    do {
                        for (String mat : list) {
                            if (ThreadLocalRandom.current().nextInt(0, 100) <= 100 / list.size()) {
                                Material material;
                                int amount;

                                try {
                                    material = Material.getMaterial(mat.split(":")[0]);
                                    amount = Integer.parseInt(mat.split(":")[1]);
                                    amount = ThreadLocalRandom.current().nextInt(1, amount + 1);
                                } catch (Exception ex) {
                                    material = Material.AIR;
                                    amount = 1;
                                }

                                drops.add(new ItemStack(material, amount));
                            }
                        }
                    } while (drops.size() < maxdrop);

                    double amount = ShopGuiPlusSupport.getSellPrice(Bukkit.getOnlinePlayers().stream().findFirst().get(), drops);
                    chunkCollector.addData(nicename, amount);
                    event.getEntity().remove();

                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlantGrow(BlockGrowEvent event) {
        if (event.isCancelled()) return;

        Location location = event.getNewState().getLocation();

//        if (event.getNewState().getType().name().startsWith("SUGAR_CANE")) {
//            ChunkCollector chunkCollector = management.getNearby(location);
//            if (chunkCollector != null) {
//                if (!chunkCollector.isConfirmed()) return;
//                if (!chunkCollector.getData().containsKey("sugarcane")) return;
//                chunkCollector.check();
//                if (!plugin.isShopGuiPlus()) {
//                    chunkCollector.addData("sugarcane", 1);
//                    event.setCancelled(true);
//                } else {
//                    try {
//                        List<ItemStack> drops = new ArrayList<>();
//
//                        int maxdrop = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.shopguiplus.mobs." + "sugarcane" + ".max-items-dropped") + 1);
//
//                        List<String> list = plugin.getConfig().getStringList("options.shopguiplus.mobs." + "sugarcane" + ".items");
//
//                        do {
//                            for (String mat : list) {
//                                if (ThreadLocalRandom.current().nextInt(0, 100) <= 100 / list.size()) {
//                                    Material material;
//                                    int amount;
//
//                                    try {
//                                        material = Material.getMaterial(mat.split(":")[0]);
//                                        amount = Integer.parseInt(mat.split(":")[1]);
//                                        amount = ThreadLocalRandom.current().nextInt(1, amount + 1);
//                                    } catch (Exception ex) {
//                                        material = Material.AIR;
//                                        amount = 1;
//                                    }
//
//                                    drops.add(new ItemStack(material, amount));
//                                }
//                            }
//                        } while (drops.size() < maxdrop);
//
//                        double amount = ShopGuiPlusSupport.getSellPrice(Bukkit.getOnlinePlayers().stream().findFirst().get(), drops);
//                        chunkCollector.addData("sugarcane", amount);
//                        event.setCancelled(true);
//                    } catch (Exception ignored) {}
//                }
//            }
//        } else if (event.getNewState().getType().name().startsWith("CACTUS")) {
//            ChunkCollector chunkCollector = management.getNearby(location);
//            if (chunkCollector != null) {
//                if (!chunkCollector.isConfirmed()) return;
//                if (!chunkCollector.getData().containsKey("cactus")) return;
//                chunkCollector.check();
//                if (!plugin.isShopGuiPlus()) {
//                    chunkCollector.addData("cactus", 1);
//                    event.setCancelled(true);
//                } else {
//                    try {
//                        List<ItemStack> drops = new ArrayList<>();
//
//                        int maxdrop = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.shopguiplus.mobs.cactus.max-items-dropped") + 1);
//
//                        List<String> list = plugin.getConfig().getStringList("options.shopguiplus.mobs.cactus.items");
//
//                        do {
//                            for (String mat : list) {
//                                if (ThreadLocalRandom.current().nextInt(0, 100) <= 100 / list.size()) {
//                                    Material material;
//                                    int amount;
//
//                                    try {
//                                        material = Material.getMaterial(mat.split(":")[0]);
//                                        amount = Integer.parseInt(mat.split(":")[1]);
//                                        amount = ThreadLocalRandom.current().nextInt(1, amount + 1);
//                                    } catch (Exception ex) {
//                                        material = Material.AIR;
//                                        amount = 1;
//                                    }
//
//                                    drops.add(new ItemStack(material, amount));
//                                }
//                            }
//                        } while (drops.size() < maxdrop);
//
//                        double amount = ShopGuiPlusSupport.getSellPrice(Bukkit.getOnlinePlayers().stream().findFirst().get(), drops);
//                        chunkCollector.addData("cactus", amount);
//                        event.setCancelled(true);
//                    } catch (Exception ignored) {}
//                }
//            }
//        } else {
//            return;
//        }
        ChunkCollector chunkCollector = management.getNearby(location);
        String nicename = getNiceName(event.getNewState().getType().name());

        if (chunkCollector != null) {
            if (!chunkCollector.isConfirmed()) return;
            if (!chunkCollector.getData().containsKey(nicename)) return;
            chunkCollector.check();
            if (!plugin.isShopGuiPlus()) {
                chunkCollector.addData(nicename, 1);
                event.setCancelled(true);
            } else {
                try {
                    List<ItemStack> drops = new ArrayList<>();

                    int maxdrop = ThreadLocalRandom.current().nextInt(1, plugin.getConfig().getInt("options.shopguiplus.mobs." + nicename + ".max-items-dropped") + 1);

                    List<String> list = plugin.getConfig().getStringList("options.shopguiplus.mobs." + nicename + ".items");

                    do {
                        for (String mat : list) {
                            if (ThreadLocalRandom.current().nextInt(0, 100) <= 100 / list.size()) {
                                Material material;
                                int amount;

                                try {
                                    material = Material.getMaterial(mat.split(":")[0]);
                                    amount = Integer.parseInt(mat.split(":")[1]);
                                    amount = ThreadLocalRandom.current().nextInt(1, amount + 1);
                                } catch (Exception ex) {
                                    material = Material.AIR;
                                    amount = 1;
                                }

                                drops.add(new ItemStack(material, amount));
                            }
                        }
                    } while (drops.size() < maxdrop);

                    double amount = ShopGuiPlusSupport.getSellPrice(Bukkit.getOnlinePlayers().stream().findFirst().get(), drops);
                    chunkCollector.addData(nicename, amount);
                    event.setCancelled(true);

                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!plugin.playerEdittingMap.containsKey(event.getWhoClicked().getUniqueId())) return;
        ChunkCollector cc = plugin.playerEdittingMap.get(event.getWhoClicked().getUniqueId());
        cc.check();
        event.setResult(Event.Result.DENY);
        event.setCancelled(true);
        if (event.getSlot() == plugin.getConfig().getInt("collection-types.mobloot.slot")) {
            // mobloot
            plugin.depositLoot((Player) event.getWhoClicked(), cc, false);
        } else if (event.getSlot() == plugin.getConfig().getInt("collection-types.tnt.slot")) {
            // tnt
            plugin.depositTnt((Player) event.getWhoClicked(), cc, false);
        } else if (event.getSlot() == plugin.getConfig().getInt("chunk loader enable disable item.menu.slot")) {
            cc.setFrozen(!cc.isFrozen());
//            event.getWhoClicked().closeInventory();
//            event.getWhoClicked().openInventory(plugin.getChunkCollectorMenu(cc));

            reset(event.getClickedInventory(), cc);

//            plugin.playerEdittingMap.put(event.getWhoClicked().getUniqueId(), cc);
            cc.check();
        } else if (event.getSlot() == plugin.getConfig().getInt("chunk-loader.menu.slot")) {
            // chunkloader
            if (!plugin.isCanChunkLoader()) return;
            Player player = (Player) event.getWhoClicked();
            if (!plugin.getConfig().getBoolean("chunk-loader.use-cash") && plugin.getConfig().getBoolean("chunk-loader.use-exp")) {
                ExperienceUtil exp = new ExperienceUtil();
                if (exp.getTotalExperience(player) < plugin.getConfig().getInt("chunk-loader.exp")) {
                    player.sendMessage(translate(plugin.getConfig().getString("chunk-loader.insufficient-exp").replace("%required-exp%",
                            plugin.NumberFormat(plugin.getConfig().getInt("chunk-loader.exp") - exp.getTotalExperience(player)))));
                } else {
                    exp.setTotalExperience(player, exp.getTotalExperience(player) - plugin.getConfig().getInt("chunk-loader.exp"));
                    if (cc.getData().getOrDefault("timeRemaining", 0).intValue() == 0 ||
                            cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis() <= 0) {
                        cc.setData("timeRemaining", System.currentTimeMillis());
                    }
                    cc.addData("timeRemaining", plugin.getConfig().getInt("chunk-loader.time-per-click") * 1000);
                    event.getWhoClicked().sendMessage(util.translate(plugin.getConfig().getString("chunk-loader.bought").replace("%time%", plugin.getTimeFormat(cc.getData().get("timeRemaining"), cc))));

//                    event.getWhoClicked().closeInventory();
//                    event.getWhoClicked().openInventory(plugin.getChunkCollectorMenu(cc));
//                    plugin.playerEdittingMap.put(event.getWhoClicked().getUniqueId(), cc);

                    reset(event.getClickedInventory(), cc);

                    cc.check();
                }
            } else if (plugin.getConfig().getBoolean("chunk-loader.use-cash") && !plugin.getConfig().getBoolean("chunk-loader.use-exp")) {
                if (plugin.getEcon().getBalance(player) < plugin.getConfig().getInt("chunk-loader.money")) {
                    player.sendMessage(translate(plugin.getConfig().getString("chunk-loader.insufficient-money").replace("%required-money%",
                            plugin.NumberFormat(plugin.getConfig().getInt("chunk-loader.money") - plugin.getEcon().getBalance(player)))));
                } else {
                    plugin.getEcon().withdrawPlayer(player, plugin.getConfig().getInt("chunk-loader.money"));
                    if (cc.getData().getOrDefault("timeRemaining", 0).intValue() == 0 ||
                            cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis() <= 0) {
                        cc.setData("timeRemaining", System.currentTimeMillis());
                    }
                    cc.addData("timeRemaining", plugin.getConfig().getInt("chunk-loader.time-per-click") * 1000);
                    event.getWhoClicked().sendMessage(util.translate(plugin.getConfig().getString("chunk-loader.bought").replace("%time%", plugin.getTimeFormat(cc.getData().get("timeRemaining"), cc))));

//                    event.getWhoClicked().closeInventory();
//                    event.getWhoClicked().openInventory(plugin.getChunkCollectorMenu(cc));
//                    plugin.playerEdittingMap.put(event.getWhoClicked().getUniqueId(), cc);

                    reset(event.getClickedInventory(), cc);

                    cc.check();
                }
            } else if (plugin.getConfig().getBoolean("chunk-loader.use-cash") && plugin.getConfig().getBoolean("chunk-loader.use-exp")) {
                ExperienceUtil exp = new ExperienceUtil();
                if (exp.getTotalExperience(player) < plugin.getConfig().getInt("chunk-loader.exp")) {
                    player.sendMessage(translate(plugin.getConfig().getString("chunk-loader.insufficient-exp").replace("%required-exp%",
                            plugin.NumberFormat(plugin.getConfig().getInt("chunk-loader.exp") - exp.getTotalExperience(player)))));
                } else {
                    if (plugin.getEcon().getBalance(player) < plugin.getConfig().getInt("chunk-loader.money")) {
                        player.sendMessage(translate(plugin.getConfig().getString("chunk-loader.insufficient-money").replace("%required-money%",
                                plugin.NumberFormat(plugin.getConfig().getInt("chunk-loader.money") - plugin.getEcon().getBalance(player)))));
                    } else {
                        plugin.getEcon().withdrawPlayer(player, plugin.getConfig().getInt("chunk-loader.money"));
                        exp.setTotalExperience(player, exp.getTotalExperience(player) - plugin.getConfig().getInt("chunk-loader.exp"));

                        if (cc.getData().getOrDefault("timeRemaining", 0).intValue() == 0 ||
                                cc.getData().get("timeRemaining").longValue() - System.currentTimeMillis() <= 0) {
                            cc.setData("timeRemaining", System.currentTimeMillis());
                        }
                        cc.addData("timeRemaining", plugin.getConfig().getInt("chunk-loader.time-per-click") * 1000);
                        event.getWhoClicked().sendMessage(util.translate(plugin.getConfig().getString("chunk-loader.bought").replace("%time%", plugin.getTimeFormat(cc.getData().get("timeRemaining"), cc))));
//                        event.getWhoClicked().closeInventory();
//                        event.getWhoClicked().openInventory(plugin.getChunkCollectorMenu(cc));
//                        plugin.playerEdittingMap.put(event.getWhoClicked().getUniqueId(), cc);

                        reset(event.getClickedInventory(), cc);

                        cc.check();
                    }
                }
            }
        }
    }

    private void reset(Inventory clickedInventory, ChunkCollector cc) {
        Inventory newInventory = plugin.getChunkCollectorMenu(cc);

        for (int i = 0; i < clickedInventory.getSize(); i++) {
            clickedInventory.setItem(i, newInventory.getItem(i));
        }
    }


    class ExperienceUtil {

        /***
         * API Taken from Essentials open-source from github in order to calculate Experience levels correctly
         * I take no ownership of this specific ExperienceUtil class
         */

        //This method is used to update both the recorded total experience and displayed total experience.

        //We reset both types to prevent issues.
        private void setTotalExperience(final Player player, final int exp) {
            if (exp < 0) {
                throw new IllegalArgumentException("Experience is negative!");
            }
            player.setExp(0);
            player.setLevel(0);
            player.setTotalExperience(0);

            int amount = exp;
            while (amount > 0) {
                final int expToLevel = getExpAtLevel(player);
                amount -= expToLevel;
                if (amount >= 0) {
                    // give until next level
                    player.giveExp(expToLevel);
                } else {
                    // give the rest
                    amount += expToLevel;
                    player.giveExp(amount);
                    amount = 0;
                }
            }
        }

        private int getExpAtLevel(final Player player) {
            return getExpAtLevel(player.getLevel());
        }

        //new Exp Math from 1.8
        public int getExpAtLevel(final int level) {
            if (level <= 15) {
                return (2 * level) + 7;
            }
            if ((level >= 16) && (level <= 30)) {
                return (5 * level) - 38;
            }
            return (9 * level) - 158;
        }

        public int getExpToLevel(final int level) {
            int currentLevel = 0;
            int exp = 0;
            while (currentLevel < level) {
                exp += getExpAtLevel(currentLevel);
                currentLevel++;
            }
            if (exp < 0) {
                exp = Integer.MAX_VALUE;
            }
            return exp;
        }
        //This method is required because the bukkit player.getTotalExperience() method, shows exp that has been 'spent'.
        //Without this people would be able to use exp and then still sell it.

        public int getTotalExperience(final Player player) {
            int exp = Math.round(getExpAtLevel(player) * player.getExp());
            int currentLevel = player.getLevel();
            while (currentLevel > 0) {
                currentLevel--;
                exp += getExpAtLevel(currentLevel);
            }
            if (exp < 0) {
                exp = Integer.MAX_VALUE;
            }
            return exp;
        }

        public int getExpUntilNextLevel(final Player player) {
            int exp = Math.round(getExpAtLevel(player) * player.getExp());
            int nextLevel = player.getLevel();
            return getExpAtLevel(nextLevel) - exp;
        }
    }

}
