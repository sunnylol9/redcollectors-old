package warfaremc.us.chunkcollectors.sunnyt;

import kr.kieran.outpost.OutpostPlugin;
import org.bukkit.entity.Player;

public class OutpostSupport {

    public static double getBoost(Player event) {
        if (OutpostPlugin.getInstance().getOutpost().getOwnerId() == null) return 1;
        if (OutpostPlugin.getInstance().getCompatHandler().isPlayerOwner(OutpostPlugin.getInstance().getOutpost().getOwnerId(), event)) {
            return 1.3;
        }
        return 1.0;
//        if (!OutpostPlugin.b().getCompatHandler().isPlayerOwner(OutpostPlugin.getInstance().getOutpost().getOwnerId(), event.getPlayer()) ) return;
    }
}
