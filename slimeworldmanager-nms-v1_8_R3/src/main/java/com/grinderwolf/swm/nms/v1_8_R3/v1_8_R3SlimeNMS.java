package com.grinderwolf.swm.nms.v1_8_R3;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.nms.CraftSlimeWorld;
import com.grinderwolf.swm.nms.SlimeNMS;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.WorldServer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.tinylog.Logger;

@Getter
public class v1_8_R3SlimeNMS implements SlimeNMS {

    private final byte worldVersion = 0x01;

    private boolean loadingDefaultWorlds = true; // If true, the addWorld method will not be skipped

    private WorldServer defaultWorld;
    private WorldServer defaultNetherWorld;
    private WorldServer defaultEndWorld;

    public v1_8_R3SlimeNMS() {
        try {
            CraftCLSMBridge.initialize(this);
        }  catch (NoClassDefFoundError ex) {
            Logger.error("Failed to find ClassModifier classes. Are you sure you installed it correctly?");
            System.exit(1); // No ClassModifier, no party
        }
    }

    @Override
    public void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) {
        if (normalWorld != null) {
            World.Environment env = World.Environment.valueOf(normalWorld.getPropertyMap().getString(SlimeProperties.ENVIRONMENT).toUpperCase());

            if (env != World.Environment.NORMAL) {
                Logger.warn("The environment for the default world must always be 'NORMAL'.");
            }

            defaultWorld = new CustomWorldServer((CraftSlimeWorld) normalWorld, new CustomDataManager(normalWorld), 0);
        }

        loadingDefaultWorlds = false;
    }

    @Override
    public Object createNMSWorld(SlimeWorld world) {
        CustomDataManager dataManager = new CustomDataManager(world);
        MinecraftServer mcServer = MinecraftServer.getServer();

        int dimension = CraftWorld.CUSTOM_DIMENSION_OFFSET + mcServer.worlds.size();
        boolean used = false;

        do {
            for (WorldServer server : mcServer.worlds) {
                used = server.dimension == dimension;

                if (used) {
                    dimension++;
                    break;
                }
            }
        } while (used);

        return new CustomWorldServer((CraftSlimeWorld) world, dataManager, dimension);
    }

    @Override
    public void generateWorld(SlimeWorld world) {
        addWorldToServerList(createNMSWorld(world));
    }

    @Override
    public void addWorldToServerList(Object worldObject) {
        if (!(worldObject instanceof WorldServer)) {
            throw new IllegalArgumentException("World object must be an instance of WorldServer!");
        }

        CustomWorldServer server = (CustomWorldServer) worldObject;
        String worldName = server.getWorldData().getName();

        if (Bukkit.getWorld(worldName) != null) {
            throw new IllegalArgumentException("World " + worldName + " already exists! Maybe it's an outdated SlimeWorld object?");
        }

        Logger.info("Loading world " + worldName);
        long startTime = System.currentTimeMillis();

        server.setReady(true);
        MinecraftServer mcServer = MinecraftServer.getServer();

        mcServer.server.addWorld(server.getWorld());
        mcServer.worlds.add(server);

        Bukkit.getPluginManager().callEvent(new WorldInitEvent(server.getWorld()));
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(server.getWorld()));

        Logger.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - startTime) + "ms.");
    }

    @Override
    public SlimeWorld getSlimeWorld(World world) {
        CraftWorld craftWorld = (CraftWorld) world;

        if (!(craftWorld.getHandle() instanceof CustomWorldServer)) {
            return null;
        }

        CustomWorldServer worldServer = (CustomWorldServer) craftWorld.getHandle();

        return worldServer.getSlimeWorld();
    }
}
