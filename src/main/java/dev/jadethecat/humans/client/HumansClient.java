package dev.jadethecat.humans.client;

import dev.jadethecat.humans.client.events.HumansClientPlay;
import net.fabricmc.api.ClientModInitializer;

public class HumansClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HumansClientPlay.initClientPlayEvents();
    }
    
}
