package warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_15_R1;

import com.mojang.authlib.GameProfile;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import warfaremc.us.chunkcollectors.sunnyt.ChunkCollector;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.CustomPlayer;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.Manager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomPlayerManager extends Manager {

    @Override
    public void spawn(final ChunkCollector chunkCollector, final Location loc) {
        final org.bukkit.craftbukkit.v1_15_R1.CraftWorld w = (org.bukkit.craftbukkit.v1_15_R1.CraftWorld) loc.getWorld();
        final net.minecraft.server.v1_15_R1.WorldServer world = w.getHandle();
        final warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_15_R1.CustomNPC cp = new CustomNPC(this, world, new GameProfile(UUID.randomUUID(), "Loader-" + chunkCollector.getIdentifier()));
        final net.minecraft.server.v1_15_R1.EntityPlayer ep = (net.minecraft.server.v1_15_R1.EntityPlayer) cp.getEntityPlayer();
        ep.setInvisible(true);

        try {
            Field affectsSpawning = locateField(ep.getClass(), "affectsSpawning");
            affectsSpawning.setAccessible(true);
            affectsSpawning.setBoolean(ep, true);
        } catch (IllegalAccessException | NullPointerException e) {
        }

        try {
            Field viewDistance = locateField(ep.getClass(), "viewDistance");
            viewDistance.setAccessible(true);
            viewDistance.setInt(ep, 1);
        } catch (IllegalAccessException | NullPointerException e) {
        }

        try {
            Field loadChunks = locateField(ep.getClass(), "loadChunks");
            loadChunks.setAccessible(true);
            loadChunks.setBoolean(ep, true);
        } catch (IllegalAccessException | NullPointerException e) {
        }

        ep.setPositionRotation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getYaw(), loc.getPitch());
        world.addEntity(ep);
        world.getPlayers().add(ep);
        if (CustomPlayerManager.npcs.containsKey(chunkCollector.getIdentifier())) {
            List<CustomPlayer> list = CustomPlayerManager.npcs.get(chunkCollector.getIdentifier());
            list.add(cp);
            CustomPlayerManager.npcs.put(chunkCollector.getIdentifier(), list);
        } else {
            List<CustomPlayer> list = new ArrayList<>();
            list.add(cp);
            CustomPlayerManager.npcs.put(chunkCollector.getIdentifier(), list);
        }
    }

    @Override
    public void removeNPCs(ChunkCollector chunkCollector) {
        if (!CustomPlayerManager.npcs.containsKey(chunkCollector.getIdentifier())) {
            return;
        }
        for (final CustomPlayer cp : CustomPlayerManager.getNPCs().get(chunkCollector.getIdentifier())) {
            final net.minecraft.server.v1_15_R1.EntityPlayer ep = (net.minecraft.server.v1_15_R1.EntityPlayer) cp.getEntityPlayer();

            try {
                Field affectsSpawning = locateField(ep.getClass(), "affectsSpawning");
                affectsSpawning.setAccessible(true);
                affectsSpawning.setBoolean(ep, false);
            } catch (IllegalAccessException | NullPointerException e) {
            }

            try {
                Field affectsSpawning = locateField(ep.getClass(), "loadChunks");
                affectsSpawning.setAccessible(true);
                affectsSpawning.setBoolean(ep, false);
            } catch (IllegalAccessException | NullPointerException e) {
            }

            final net.minecraft.server.v1_15_R1.WorldServer world = ((org.bukkit.craftbukkit.v1_15_R1.CraftWorld) ep.getBukkitEntity().getLocation().getWorld()).getHandle();
            world.removeEntity(ep);
            world.getPlayers().remove(ep);
        }
        CustomPlayerManager.npcs.remove(chunkCollector.getIdentifier());
    }

    public Field locateField(Class<?> clazz, String name) {
        Validate.noNullElements(new Object[]{clazz, name});
        Class<?> current = clazz;
        do {
            try {
                return current.getDeclaredField(name);
            } catch (Exception e) {
            }
        } while ((current = current.getSuperclass()) != null);
        return null;
    }
}

