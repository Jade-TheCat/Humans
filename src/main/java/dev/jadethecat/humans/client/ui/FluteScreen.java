package dev.jadethecat.humans.client.ui;

import io.github.cottonmc.cotton.gui.GuiDescription;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;

public class FluteScreen extends CottonClientScreen {

    public FluteScreen(GuiDescription description) {
        super(description);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
