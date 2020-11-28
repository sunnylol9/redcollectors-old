package warfaremc.us.chunkcollectors.sunnyt;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPermission;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SuperiorSkyblockSupport {

    // SuperiorSkyblock2
    public static boolean isConfirmed(Player player, ChunkCollector chunkCollector, CC plugin, Util util) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player);

        boolean hasPermission = SuperiorSkyblockAPI.getIslandAt(chunkCollector.getLocation()) != null && SuperiorSkyblockAPI.getIslandAt(chunkCollector.getLocation()).hasPermission(superiorPlayer, IslandPermission.valueOf(plugin.getConfig().getString("SuperiorSkyBlock2.IslandPermission")));

        boolean options_other = plugin.getConfig().getBoolean("options.can-access-not-yours");

        String other = util.translate(plugin.getConfig().getString("cannot-access-others"));

        if (options_other || hasPermission || chunkCollector.getOwner().equalsIgnoreCase(player.getUniqueId().toString())) {
            return true;
        }

        player.sendMessage(other);
        return false;
    }

    public static double getMobLootMultiplier(Location loc) {
        Island island = SuperiorSkyblockAPI.getIslandAt(loc);
        if (island != null) {
            return island.getMobDropsMultiplier() < 1 ? 1 : island.getMobDropsMultiplier();
        }

        return 1;
    }
}
