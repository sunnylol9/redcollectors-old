package warfaremc.us.chunkcollectors.sunnyt;

import gcspawners.ASAPI;
import org.bukkit.block.CreatureSpawner;

public class AdvancedSpawnerSupport {

    public static int getAmount(CreatureSpawner creatureSpawner) {
        return ASAPI.getSpawnerAmount(creatureSpawner.getLocation());
    }

}
