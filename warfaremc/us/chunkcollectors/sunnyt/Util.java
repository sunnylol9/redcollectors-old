package warfaremc.us.chunkcollectors.sunnyt;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class Util {

    private String server_version;

    public Util() {
        try {
            Server server = Bukkit.getServer();
            Method serverHandle;
            serverHandle = server.getClass().getDeclaredMethod("getHandle");
            serverHandle.setAccessible(true);
            Object nmsServer = null;
            nmsServer = serverHandle.invoke(server);
            Class serverClass;
            serverClass = nmsServer.getClass();
            String className = serverClass.getPackage().getName();
            String[] args = className.split("\\.");
            server_version = args[3];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /***
     * API Taken from SpigotMC.Org Resource thread on how to send titles using reflection since it was not really possible in 1.8 spigot,
     *      I made minor alterations so it was not version dependant but I do not claim the methods send() and sendPacket() as mine!
     * fields server_version, contstructors and method translate() is mine so I do claim those
     */

    public int returnInt(String numberConfig) {
        return numberConfig.hashCode();
    }

    public String translate(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public void send(Player player, String title, String subtitle, int fadeInTime, int showTime, int fadeOutTime) {
        Class IChatBaseComponent = null;
        Class PacketPlayOutTitle = null;

        try {
            IChatBaseComponent = Class.forName("net.minecraft.server." + server_version + ".IChatBaseComponent");
            PacketPlayOutTitle = Class.forName("net.minecraft.server." + server_version + ".PacketPlayOutTitle");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        try {
            Object chatTitle = IChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class)
                    .invoke(null, "{\"text\": \"" + title + "\"}");

            Constructor<?> titleConstructor = PacketPlayOutTitle.getConstructor(
                    PacketPlayOutTitle.getDeclaredClasses()[0], IChatBaseComponent,
                    int.class, int.class, int.class);

            Object packet;
            packet = titleConstructor.newInstance(
                    PacketPlayOutTitle.getDeclaredClasses()[0].getField("TITLE").get(null), chatTitle,
                    fadeInTime, showTime, fadeOutTime);
            Object chatsTitle = IChatBaseComponent.getDeclaredClasses()[0].getMethod("a", String.class)
                    .invoke(null, "{\"text\": \"" + subtitle + "\"}");

            Constructor<?> timingTitleConstructor = PacketPlayOutTitle.getConstructor(
                    PacketPlayOutTitle.getDeclaredClasses()[0], IChatBaseComponent,
                    int.class, int.class, int.class);

            Object timingPacket = timingTitleConstructor.newInstance(
                    PacketPlayOutTitle.getDeclaredClasses()[0].getField("SUBTITLE").get(null), chatsTitle,
                    fadeInTime, showTime, fadeOutTime);
            sendPacket(player, packet);
            sendPacket(player, timingPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPacket(Player player, Object packet) {
        Class Packet = null;

        try {
            Packet = Class.forName("net.minecraft.server." + server_version + ".Packet");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", Packet).invoke(playerConnection, packet);
        } catch (Exception ex) {
            //Do something
        }
    }

    public Method locateMethod(Class<?> clazz, String name, Class[] params) {
        Validate.noNullElements(new Object[]{clazz, name});
        Class<?> current = clazz;
        do {
            try {
                return current.getDeclaredMethod(name, params);
            } catch (Exception e) {
            }
        } while ((current = current.getSuperclass()) != null);
        return null;
    }
}
