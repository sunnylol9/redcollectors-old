package warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_12_R1;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_12_R1.EnumGamemode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import warfaremc.us.chunkcollectors.sunnyt.CC;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.CustomPlayer;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.Manager;

public class CustomNPC extends net.minecraft.server.v1_12_R1.EntityPlayer implements CustomPlayer {

    static net.minecraft.server.v1_12_R1.MinecraftServer minecraftServer;

    static {
        CustomNPC.minecraftServer = ((org.bukkit.craftbukkit.v1_12_R1.CraftServer) Bukkit.getServer()).getServer();
    }

    private int lastTargetId;
    private long lastBounceTick;
    private int lastBounceId;

    public CustomNPC(final Manager manager, final net.minecraft.server.v1_12_R1.WorldServer worldserver, final GameProfile gameprofile) {
        super(CustomNPC.minecraftServer, worldserver, gameprofile, new net.minecraft.server.v1_12_R1.PlayerInteractManager(worldserver));
        final net.minecraft.server.v1_12_R1.NetworkManager nm = new CustomNetworkManager();
        this.playerConnection = new CustomPlayerConnection(this.server, nm, this);
        this.playerInteractManager.b(EnumGamemode.CREATIVE);
        this.lastTargetId = -1;
        this.lastBounceId = -1;
        this.lastBounceTick = 0L;
        this.fauxSleeping = true;
        this.invulnerableTicks = Integer.MAX_VALUE;
        new BukkitRunnable() {
            public void run() {
                final net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy packet = new net.minecraft.server.v1_12_R1.PacketPlayOutEntityDestroy(CustomNPC.this.getBukkitEntity().getEntityId());
                for (final Player p : Bukkit.getOnlinePlayers()) {
                    ((org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
                }
            }
        }.runTaskLater(JavaPlugin.getProvidingPlugin(CC.class), 20L);
    }

    public void a(final net.minecraft.server.v1_12_R1.EntityLiving entityliving) {
        if ((this.lastBounceId != entityliving.getId() || System.currentTimeMillis() - this.lastBounceTick > 10000L) && entityliving.getBukkitEntity().getLocation().distanceSquared(this.getBukkitEntity().getLocation()) <= 1.0) {
            this.lastBounceTick = System.currentTimeMillis();
            this.lastBounceId = entityliving.getId();
        }

        if (this.lastTargetId == -1 || this.lastTargetId != entityliving.getId()) {
            this.lastTargetId = entityliving.getId();
        }
        super.a(entityliving);
    }

    public Object getEntityPlayer() {
        return this;
    }
}