package warfaremc.us.chunkcollectors.sunnyt.chunkloader.v1_15_R1;

import io.netty.channel.*;
import net.minecraft.server.v1_15_R1.EnumProtocolDirection;
import net.minecraft.server.v1_15_R1.NetworkManager;

import java.lang.reflect.Field;
import java.net.SocketAddress;

public class CustomNetworkManager extends NetworkManager {

    public CustomNetworkManager() {
        super(EnumProtocolDirection.SERVERBOUND);
    }

    public void swapFields() {
        try {
            final Field channelField = NetworkManager.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            channelField.set(this, new AbstractChannel(null) {
                public ChannelConfig config() {
                    return null;
                }

                public boolean isActive() {
                    return false;
                }

                public boolean isOpen() {
                    return false;
                }

                public ChannelMetadata metadata() {
                    return null;
                }

                protected void doBeginRead() throws Exception {
                }

                protected void doBind(final SocketAddress arg0) throws Exception {
                }

                protected void doClose() throws Exception {
                }

                protected void doDisconnect() throws Exception {
                }

                protected void doWrite(final ChannelOutboundBuffer arg0) throws Exception {
                }

                protected boolean isCompatible(final EventLoop arg0) {
                    return false;
                }

                protected SocketAddress localAddress0() {
                    return null;
                }

                protected AbstractUnsafe newUnsafe() {
                    return null;
                }

                protected SocketAddress remoteAddress0() {
                    return null;
                }
            });
            channelField.setAccessible(false);
            final Field socketAddressField = NetworkManager.class.getDeclaredField("socketAddress");
            socketAddressField.setAccessible(true);
            socketAddressField.set(this, null);
            socketAddressField.setAccessible(true);
            socketAddressField.set(this, null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex4) {
            ex4.printStackTrace();
        }
    }
}
