package warfaremc.us.chunkcollectors.sunnyt;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.inventory.ItemStack;

public class HeadDatabaseSupport {

    private HeadDatabaseAPI headDatabaseAPI;

    public HeadDatabaseSupport() {
        this.headDatabaseAPI = new HeadDatabaseAPI();
    }

    public ItemStack getHead(int id) {
        return headDatabaseAPI.getItemHead(String.valueOf(id));
    }

}
