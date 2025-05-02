package com.knoxhack.ezi;

import com.knoxhack.ezi.client.EziClient;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import org.slf4j.Logger;

@Mod(EZIMod.MODID)
public class EZIMod {
    public static final String MODID = "ezi";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EZIMod(ModContainer container) {
        IEventBus modBus = container.getEventBus();
        modBus.addListener(FMLClientSetupEvent.class, this::onClientSetup);
        modBus.addListener(FMLLoadCompleteEvent.class, this::onLoadComplete);
    }

    private void onClientSetup(FMLClientSetupEvent evt) {
        EziClient.init();
        //OverlayManager.init();
    }

    private void onLoadComplete(FMLLoadCompleteEvent evt) {
        IngredientRegistry.bootstrap();
        RecipeRegistry.bootstrap();
    }
}