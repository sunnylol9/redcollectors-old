package warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_8_R3;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import warfaremc.us.chunkcollectors.sunnyt.CC;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.CustomPlayer;
import warfaremc.us.chunkcollectors.sunnyt.chunkloader.Manager;

public class CustomNPC extends net.minecraft.server.v1_8_R3.EntityPlayer implements CustomPlayer {

    static net.minecraft.server.v1_8_R3.MinecraftServer minecraftServer;

    static {
        CustomNPC.minecraftServer = ((org.bukkit.craftbukkit.v1_8_R3.CraftServer) Bukkit.getServer()).getServer();
    }

    private int lastTargetId;
    private long lastBounceTick;
    private int lastBounceId;

    public CustomNPC(final Manager manager, final net.minecraft.server.v1_8_R3.WorldServer worldserver, final GameProfile gameprofile) {
        super(CustomNPC.minecraftServer, worldserver, gameprofile, new net.minecraft.server.v1_8_R3.PlayerInteractManager(worldserver));
        final net.minecraft.server.v1_8_R3.NetworkManager nm = new CustomNetworkManager();
        this.playerConnection = new CustomPlayerConnection(this.server, nm, this);
        this.playerInteractManager.b(net.minecraft.server.v1_8_R3.WorldSettings.EnumGamemode.CREATIVE);
        this.lastTargetId = -1;
        this.lastBounceId = -1;
        this.lastBounceTick = 0L;
        this.fauxSleeping = true;
        this.invulnerableTicks = Integer.MAX_VALUE;
        new BukkitRunnable() {
            public void run() {
                final net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy packet = new net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy(CustomNPC.this.getBukkitEntity().getEntityId());
                for (final Player p : Bukkit.getOnlinePlayers()) {
                    ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
                }
            }
        }.runTaskLater(CC.getInstance(), 20L);
    }

    @Override
    public void a(final net.minecraft.server.v1_8_R3.EntityLiving entityliving) {
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