package dev.jadethecat.humans.client.ui;

import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WPlainPanel;
import io.github.cottonmc.cotton.gui.widget.WSprite;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class HumanListItem extends WPlainPanel {
    WSprite sprite;
    WLabel name;
    
    public HumanListItem() {
        sprite = new WSprite(new Identifier("humans:flute"));
        this.add(sprite, 2, 2, 18, 18);
        name = new WLabel(new TranslatableText("gui.humans.flute.human_no_name"));
        this.add(name, 18+4, 2, 5*18, 18);
        this.setSize(7*18, 2*18);
    }
}
