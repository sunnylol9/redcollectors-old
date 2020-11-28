package warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_14_R1;

import net.minecraft.server.v1_14_R1.*;

public class CustomPlayerConnection extends PlayerConnection {

    public CustomPlayerConnection(final MinecraftServer minecraftserver, final NetworkManager networkmanager, final EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);
    }

    public void a(final PacketPlayInWindowClick packetplayinwindowclick) {
    }

    public void a(final PacketPlayInTransaction packetplayintransaction) {
    }

    public void a(final PacketPlayInFlying packetplayinflying) {
    }

    public void a(final PacketPlayInUpdateSign packetplayinupdatesign) {
    }

    public void a(final PacketPlayInBlockDig packetplayinblockdig) {
    }

    public void a(final PacketPlayInBlockPlace packetplayinblockplace) {
    }

    public void disconnect(final String s) {
    }

    public void a(final PacketPlayInHeldItemSlot packetplayinhelditemslot) {
    }

    public void a(final PacketPlayInChat packetplayinchat) {
    }

    public void sendPacket(final Packet packet) {
    }

}
