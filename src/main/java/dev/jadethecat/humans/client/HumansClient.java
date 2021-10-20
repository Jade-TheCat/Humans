package dev.jadethecat.humans.client;

import java.util.List;

import dev.jadethecat.humans.client.events.HumansClientPlay;
import dev.jadethecat.humans.client.ui.FluteHumanGui;
import dev.jadethecat.humans.client.ui.FlutePartyGui;
import dev.jadethecat.humans.client.ui.FluteScreen;
import dev.jadethecat.humans.entity.HumanEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class HumansClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HumansClientPlay.initClientPlayEvents();
    }
    
    @Environment(EnvType.CLIENT)
    public static void openFluteScreen(HumanEntity h) {
        MinecraftClient.getInstance().setScreen(new FluteScreen(new FluteHumanGui(h)));
    }
    @Environment(EnvType.CLIENT)
    public static void openFlutePartyScreen(PlayerEntity user, List<HumanEntity> humans) {
        MinecraftClient.getInstance().setScreen(new FluteScreen(new FlutePartyGui(user, humans)));
    }
}
