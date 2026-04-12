package com.lazify;

import com.lazify.command.CommandOv;
import com.lazify.overlay.OverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = LazifyMod.MODID, name = LazifyMod.NAME, version = LazifyMod.VERSION, clientSideOnly = true)
public class LazifyMod {

    public static final String MODID   = "lazify";
    public static final String NAME    = "Lazify";
    public static final String VERSION = "3.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    private static boolean initialized = false;

    @Mod.EventHandler
    public void onForgeInit(FMLInitializationEvent event) {
        doInit();
    }

    static void doInit() {
        if (initialized) return;
        initialized = true;
        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        MinecraftForge.EVENT_BUS.register(new com.lazify.EventHandler());
        ClientCommandHandler.instance.registerCommand(new CommandOv());
        OverlayManager.INSTANCE.init(configDir);
    }
}
