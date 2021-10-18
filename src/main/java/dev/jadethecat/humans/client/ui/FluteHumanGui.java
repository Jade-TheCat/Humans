package dev.jadethecat.humans.client.ui;

import java.util.Iterator;
import java.util.Map;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import dev.jadethecat.humans.entity.HumanEntity;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WSprite;
import io.github.cottonmc.cotton.gui.widget.WTextField;
import io.github.cottonmc.cotton.gui.widget.WToggleButton;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import io.netty.buffer.ByteBufAllocator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FluteHumanGui extends LightweightGuiDescription{
    public FluteHumanGui(HumanEntity h) {
        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setSize(252, 234);
        root.setInsets(Insets.ROOT_PANEL);
        WLabel title = new WLabel(
            new TranslatableText("gui.humans.flute.human_management", 
                h.hasCustomName() ? h.getCustomName() : new TranslatableText("gui.humans.flute.human_no_name"))
            ).setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(title, 0, 0, 14, 1);

        MinecraftClient client = MinecraftClient.getInstance();
        Identifier textureId;
        if (h.getSkinProfile() != null) {
            Map<Type,MinecraftProfileTexture> map = client.getSkinProvider().getTextures(h.getSkinProfile());
            textureId = map.containsKey(Type.SKIN) ? client.getSkinProvider().loadSkin(map.get(Type.SKIN), Type.SKIN) 
                : DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(h.getSkinProfile()));
        } else if (h.usesSlimSkin()) {
            textureId = new Identifier("minecraft", "textures/entity/alex.png");
        } else if (h.usesLegacyAnim() || h.usesLegacySound()) {
            textureId = new Identifier("humans", "textures/entity/human/legasteve.png");
        } else {
            textureId = new Identifier("minecraft", "textures/entity/steve.png");
        }
        WSprite sprite = new WSprite(new Texture(textureId).withUv(0.128f, 0.128f, 0.25f, 0.25f));
        root.add(sprite, 0, 1, 2, 2);

        String curState = h.getState();
        Identifier state = new Identifier(curState);
        TranslatableText stateText = new TranslatableText("entity."+state.getNamespace()+".human_state."+state.getPath());
        TranslatableText stateLabel = new TranslatableText("gui.humans.flute.human_state", stateText);

        WButton stateButton = new WButton(stateLabel);
        stateButton.setOnClick(() -> {
            if (HumanEntity.humanStates.contains(curState)) {
                String newState = curState;
                for (Iterator<String> it = HumanEntity.humanStates.iterator(); it.hasNext();) {
                    String s = it.next();
                    if (s.contentEquals(h.getState()) && it.hasNext()) {
                        newState = it.next();
                        break;
                    } else if (s.contentEquals(h.getState())) {
                        newState = (String)HumanEntity.humanStates.toArray()[0];
                    }
                }
                PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
                buf.writeUuid(h.getUuid());
                buf.writeString(newState);
                ClientPlayNetworking.send(new Identifier("humans", "update_human_state"), buf);
                Identifier t = new Identifier(newState);
                TranslatableText newLabel = new TranslatableText("gui.humans.flute.human_state", 
                    new TranslatableText("entity."+t.getNamespace()+".human_state."+t.getPath())
                );
                stateButton.setLabel(newLabel);
            }
        });
        root.add(stateButton, 3, 1, 6, 1);
        byte flags = h.getFlags();

        WTextField renameTextField = new WTextField(h.hasCustomName() ? h.getCustomName() : new TranslatableText("gui.humans.flute.enter_name"));
        root.add(renameTextField, 0, 3, 10, 1);

        WButton renameButton = new WButton(new TranslatableText("gui.humans.flute.rename"));
        renameButton.setOnClick(() -> {
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeUuid(h.getUuid());
            buf.writeString(renameTextField.getText());
            ClientPlayNetworking.send(new Identifier("humans", "change_human_name"), buf);
            MinecraftClient.getInstance().setScreen(null);
        });
        root.add(renameButton, 10, 3, 4, 1);

        WToggleButton legacySoundToggle = new WToggleButton(new TranslatableText("gui.humans.flute.legacy_sound"));
        legacySoundToggle.setToggle(((flags >> 0) & 1) == 1);
        legacySoundToggle.setOnToggle(on -> {
            byte flags2 = h.getFlags();
            if (on)
                flags2 |= (1 << 0);
            else
                flags2 &= ~(1 << 0);
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeUuid(h.getUuid());
            buf.writeByte(flags2);
            ClientPlayNetworking.send(new Identifier("humans", "update_human_flags"), buf);
        });
        root.add(legacySoundToggle, 0, 4, 4, 1);
        WToggleButton legacyAnimToggle = new WToggleButton(new TranslatableText("gui.humans.flute.legacy_animation"));
        legacyAnimToggle.setToggle(((flags >> 1) & 1) == 1);
        legacyAnimToggle.setOnToggle(on -> {
            byte flags2 = h.getFlags();
            if (on)
                flags2 |= (1 << 1);
            else
                flags2 &= ~(1 << 1);
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeUuid(h.getUuid());
            buf.writeByte(flags2);
            ClientPlayNetworking.send(new Identifier("humans", "update_human_flags"), buf);
        });
        root.add(legacyAnimToggle, 0, 5, 4, 1);
        WToggleButton slimSkinToggle = new WToggleButton(new TranslatableText("gui.humans.flute.slim_skin"));
        slimSkinToggle.setToggle(((flags >> 2) & 1) == 1);
        slimSkinToggle.setOnToggle(on -> {
            byte flags2 = h.getFlags();
            if (on)
                flags2 |= (1 << 2);
            else
                flags2 &= ~(1 << 2);
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeUuid(h.getUuid());
            buf.writeByte(flags2);
            ClientPlayNetworking.send(new Identifier("humans", "update_human_flags"), buf);
        });
        root.add(slimSkinToggle, 0, 6, 4, 1);

        WToggleButton skinLockToggle = new WToggleButton(new TranslatableText("gui.humans.flute.skin_lock"));
        skinLockToggle.setToggle(((flags >> 3) & 1) == 1);
        skinLockToggle.setOnToggle(on -> {
            byte flags2 = h.getFlags();
            if (on)
                flags2 |= (1 << 3);
            else
                flags2 &= ~(1 << 3);
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeUuid(h.getUuid());
            buf.writeByte(flags2);
            ClientPlayNetworking.send(new Identifier("humans", "update_human_flags"), buf);
        });
        root.add(skinLockToggle, 0, 7, 4, 1);

        root.validate(this);
    }
}
