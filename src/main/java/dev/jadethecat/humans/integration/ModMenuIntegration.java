package dev.jadethecat.humans.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.jadethecat.humans.HumansConfig;
import me.shedaniel.autoconfig.AutoConfig;


public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(HumansConfig.class, parent).get();
    }
}
