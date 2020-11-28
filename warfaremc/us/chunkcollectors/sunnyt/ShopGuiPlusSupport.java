package warfaremc.us.chunkcollectors.sunnyt;

import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopGuiPlusSupport {

    public static double getSellPrice(Player player, List<ItemStack> drop) {
        double money = 0;
        for (ItemStack itemStack : drop) {
            for (Shop shop : ShopGuiPlugin.getInstance().getShopManager().shops.values()) {
                for (ShopItem shopItem : shop.getShopItems()) {
                    if (shopItem.getItem().getType() != itemStack.getType()) continue;
                    if (!shop.hasAccess(player, shopItem, false)) {
                        continue;
                    }
                    money += (shopItem.getSellPrice() / shopItem.getItem().getAmount()) * itemStack.getAmount();
                }
            }
        }
        return money;
    }
}
