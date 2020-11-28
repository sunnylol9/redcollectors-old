package warfaremc.us.chunkcollectors.sunnyt;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import org.bukkit.block.CreatureSpawner;

public class WildStackerSupport {

    public static int getAmount(CreatureSpawner creatureSpawner) {
        return WildStackerAPI.getSpawnersAmount(creatureSpawner);
    }

}
