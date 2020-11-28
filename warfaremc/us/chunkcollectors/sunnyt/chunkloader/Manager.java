package warfaremc.us.chunkcollectors.sunnyt.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import warfaremc.us.chunkcollectors.sunnyt.ChunkCollector;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class Manager {
    protected static Map<Integer, List<CustomPlayer>> npcs;
    protected static Random random;

    static {
        Manager.npcs = new HashMap<Integer, List<CustomPlayer>>();
        Manager.random = new Random();
    }

    public static Manager getInstance() {
        try {
            Server server = Bukkit.getServer();
            Method serverHandle;
            serverHandle = server.getClass().getDeclaredMethod("getHandle");
            serverHandle.setAccessible(true);
            Object nmsServer;
            nmsServer = serverHandle.invoke(server);
            Class serverClass;
            serverClass = nmsServer.getClass();
            String className = serverClass.getPackage().getName();
            String[] args = className.split("\\.");

            String serverVersion = args[3];
            Class CustomPlayerManager = Class.forName("warfaremc.us.chunkcollectors.sunnyt.chunkloader." + serverVersion + ".CustomPlayerManager");
            return (Manager) CustomPlayerManager.newInstance();

        } catch (Exception ex) {
            System.err.println(ex.getLocalizedMessage());
        }

        return null;
    }

    public static Map<Integer, List<CustomPlayer>> getNPCs() {
        return Manager.npcs;
    }

    public abstract void spawn(ChunkCollector chunkCollector, final Location p0);

    public abstract void removeNPCs(ChunkCollector chunkCollector);
}