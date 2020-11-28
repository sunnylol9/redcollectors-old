package warfaremc.us.chunkcollectors.sunnyt;

import net.prosavage.outpost.api.OutpostAPI;
import net.prosavage.outpost.outpost.OutpostData;
import org.bukkit.entity.Player;

public class SavageOutpostsSupport {


    public static double getCappingBoost(Player player) {
        double boost = 0;
        for (String outpost : CC.getInstance().getConfig().getConfigurationSection("SavageOutposts.multipliers").getKeys(false)) {
            if (OutpostAPI.getOutpostData(outpost) == null) continue;
            OutpostData outpostData = OutpostAPI.getOutpostData(outpost);
            if (outpostData.getControlType() == OutpostData.ControlType.CONTROLLED) {
                if (CC.getInstance().isFactions()) {
                    String faction = FactionsSupport.getCurrentFaction(player);
                    if (outpostData.getFactionTag().equalsIgnoreCase(faction)) {
                        boost += CC.getInstance().getConfig().getDouble("SavageOutposts.multipliers." + outpost + ".boost");
                    }
                }
            }
        }
        return boost;
    }


}
