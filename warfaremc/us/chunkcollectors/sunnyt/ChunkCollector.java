package warfaremc.us.chunkcollectors.sunnyt;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.Manager;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class ChunkCollector {

    private int identifier;
    private String owner;
    private UUID placer;
    private Location location;
    private int level;
    private HashMap<String, Number> data;

    private boolean frozen;
    private Location holoLoc;
    private BukkitTask hologramTask;
    private boolean ChunkLoading;
    private Hologram hologram;


    /* LOCATION DETAILS */
    private UUID world;
    private int chunkX, chunkZ;

    public ChunkCollector(String holder, int identifier, UUID placer, Location location, int level, HashMap<String, Number> data) {
        this.owner = holder;
        this.placer = placer;
        this.location = location;
        this.level = level;
        this.identifier = identifier;
        this.data = data;
        this.frozen = false;
        this.ChunkLoading = this.data.containsKey("timeRemaining") && (this.data.get("timeRemaining").longValue() - System.currentTimeMillis()) > 0;
        this.holoLoc = location.clone().add(0.5, 2, 0.5);

//        this.hologram.setCustomNameVisible(!CC.getInstance().getConfig().getBoolean("directional hologram"));
        if (CC.getInstance().getConfig().getBoolean("show hologram")) {
            hologram = HologramsAPI.createHologram(CC.getInstance(), holoLoc);
//            hologram.setAllowPlaceholders(true);
//            hologram.appendTextLine("Collector");
        }
        world = location.getWorld().getUID();
        chunkX = location.getChunk().getX();
        chunkZ = location.getChunk().getZ();
    }

    public Location getHoloLoc() {
        return holoLoc;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String faction) {
        owner = faction;
    }

    public int getIdentifier() {
        return identifier;
    }

    public UUID getPlacer() {
        return placer;
    }

    public Location getLocation() {
        return location;
    }

    public int getLevel() {
        return level;
    }

    public HashMap<String, Number> getData() {
        return data;
    }

    public void disable() {
        try {
            Manager.getInstance().removeNPCs(this);
        } catch (Exception ex) {
            System.err.println("[RedCollectors] Attempted to despawn NPCS on a version that does not support this!");
        }
    }

    public void check() {
        boolean hasTime = this.data.containsKey("timeRemaining") && (this.data.get("timeRemaining").longValue() - System.currentTimeMillis()) > 0;
        if (frozen) {
            hasTime = false;
        }
        if (hasTime) {
            if (!Manager.getNPCs().containsKey(this.getIdentifier())) {
                this.spawnNPC();
            }
        } else if (Manager.getNPCs().containsKey(this.getIdentifier())) {
            this.disable();
        }
        ChunkLoading = hasTime;
        if (frozen) {
            ChunkLoading = false;
        }
    }

    public Hologram getHologram() {
        return hologram;
    }

    private void spawnNPC() {
        Chunk chunk = location.clone().getChunk();

        try {
            for (int y = 0; y < 256; y += 16) {
                int x = (chunk.getX() * 16) + 8;
                int z = (chunk.getZ() * 16) + 8;
                Location l = new Location(chunk.getWorld(), x, y, z);

                Objects.requireNonNull(Manager.getInstance()).spawn(this, l);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("[RedCollectors] Attempted to spawn NPCS on a version that does not support this!");
        }
    }

    public void addData(String value, Number amount) {
        data.put(value, data.getOrDefault(value, 0).longValue() + amount.longValue());
    }

    public void resetData(boolean tnt) {
        if (tnt) {
            data.put("tnt", 0);
        } else {
            for (String value : data.keySet()) {
                if (value.equalsIgnoreCase("timeRemaining") || value.equalsIgnoreCase("tnt")) {
                    continue;
                }
                data.put(value, 0);
            }
        }
    }

    @Override
    public String toString() {
        return "{" + "faction=" + owner + ",identifier=" + identifier + ",placer=" + placer + ",data=" + data + "}";
    }

    public void setData(String value, Number amount) {
        data.put(value, amount);
    }

    public boolean isConfirmed() {
        Material type = location.getBlock().getType();
        boolean confirmed = type == CC.getInstance().naturalMaterial ||
                type == CC.getInstance().brokenMaterial;
        if (!confirmed) {
//            if (this.hologramTask != null ) this.hologramTask.cancel();
//
//            if (hologram != null) {
//                for (Entity nearbyHolograms : hologram.getNearbyEntities(8, 2, 8)) {
//                    if (nearbyHolograms.hasMetadata("chunkCollector") && nearbyHolograms.getMetadata("chunkCollector").get(0).asInt() == identifier) {
//                        nearbyHolograms.remove();
//                        continue;
//                    }
//                    if (nearbyHolograms.hasMetadata("chunkCollector") && CC.getInstance().getManagement().findCollector(nearbyHolograms.getMetadata("chunkCollector").get(0).asInt())
//                            == null) {
//                        nearbyHolograms.remove();
//                    }
//                }
//
//                hologram.remove();
//            }
            cancelTask();
        }
        return confirmed;
    }

    public void setHologramTask(BukkitTask hologramTask) {
        this.hologramTask = hologramTask;
    }

    public void cancelTask() {
        try {
            if (this.hologramTask != null) {
                this.hologramTask.cancel();
            }

            if (hologram != null && !hologram.isDeleted()) {
//                for (Entity nearbyHolograms : hologram.getNearbyEntities(8, 2, 8)) {
//                    if (nearbyHolograms.hasMetadata("chunkCollector") && nearbyHolograms.getMetadata("chunkCollector").get(0).asInt() == identifier) {
//                        nearbyHolograms.remove();
//                        continue;
//                    }
//                    if (nearbyHolograms.hasMetadata("chunkCollector") && CC.getInstance().getManagement().findCollector(nearbyHolograms.getMetadata("chunkCollector").get(0).asInt())
//                            == null) {
//                        nearbyHolograms.remove();
//                    }
//                }
                hologram.delete();
                hologram = null;
            }
        } catch (Exception ex) {
            CC.getInstance().getLogger().warning(ex.getLocalizedMessage());
        }

    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean bol) {
        if (!CC.getInstance().isCanChunkLoader()) return;
        frozen = bol;
        if (bol) {
            CC.getInstance().getManagement().cachedLastSystemTime.put(this, System.currentTimeMillis());
            CC.getInstance().getManagement().cachedLastTimeFrozen.put(this, data.get("timeRemaining").longValue() - CC.getInstance().getManagement().cachedLastSystemTime.get(this));
        } else {
            try {
                // C = A - B
                // C + B = A
                long d = System.currentTimeMillis() - CC.getInstance().getManagement().cachedLastSystemTime.get(this);
                data.put("timeRemaining", CC.getInstance().getManagement().cachedLastTimeFrozen.get(this) + CC.getInstance().getManagement().cachedLastSystemTime.get(this) + d);
            } catch (NullPointerException ex) {
                // ignore
            }
        }
    }

    public UUID getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }
}
