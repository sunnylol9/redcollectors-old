package warfaremc.us.chunkcollectors.sunnyt;

import com.bgsoftware.wildtools.api.WildToolsAPI;
import com.bgsoftware.wildtools.api.objects.tools.SellTool;
import com.bgsoftware.wildtools.api.objects.tools.Tool;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WildToolsSupport {

    public static double getBoost(Player player) {
        ItemStack itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.hasItemMeta()) {
            for (Tool tool : WildToolsAPI.getWildTools().getToolsManager().getTools()) {
//                boolean owns = WildToolsAPI.getWildTools().getToolsManager().(tool.getItemStack(), player);
                if (!(tool instanceof SellTool)) continue;
                SellTool sellTool = (SellTool) tool;
                if (itemInHand.getItemMeta().hasDisplayName() && itemInHand.getItemMeta().hasLore()) {
                    if (itemInHand.getItemMeta().getDisplayName().equals(sellTool.getItemStack().getItemMeta().getDisplayName())) {
                        int matches = 0;
                        for (String s : itemInHand.getItemMeta().getLore()) {
                            if (sellTool.getItemStack().getItemMeta().getLore().contains(s)) {
                                matches++;
                            }
                        }
                        if (matches >= itemInHand.getItemMeta().getLore().size() - 2) {
                            return sellTool.getMultiplier();
                        }
                    }
                }
            }
        }

        return 1D;
    }

}
