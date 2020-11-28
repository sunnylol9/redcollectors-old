package warfaremc.us.chunkcollectors.sunnyt;

import com.benzimmer123.outpost.data.OutpostData;
import com.benzimmer123.outpost.obj.Outpost;
import org.bukkit.entity.Player;

public class BenzimmerOutpostSupport {

    public static double getBoost(Player player, CC cc) {
        double boost = 1D;
        for (Outpost outpost : OutpostData.getInstance().getOutposts()) {
            if (outpost.getController().getOwner().getTeamPlayers().contains(player)) {
                boost *= cc.getConfig().getDouble("benzimmer_Outposts.boosts." + outpost.getName());
            }
        }
        return boost;
    }

}
