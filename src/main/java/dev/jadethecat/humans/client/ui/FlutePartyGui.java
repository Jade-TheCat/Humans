package dev.jadethecat.humans.client.ui;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import dev.jadethecat.humans.components.HumansComponents;
import dev.jadethecat.humans.entity.HumanEntity;
import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.data.Texture;
import io.netty.buffer.ByteBufAllocator;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FlutePartyGui extends LightweightGuiDescription {
    

    public FlutePartyGui(PlayerEntity player, List<HumanEntity> humans) {
        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setSize(252, 234);
        root.setInsets(Insets.ROOT_PANEL);
        WLabel title = new WLabel(new TranslatableText("gui.humans.flute.party_management"))
                            .setHorizontalAlignment(HorizontalAlignment.CENTER);
        root.add(title, 6, 0, 2, 1);
        WButton summonPartyButton = new WButton(new TranslatableText("gui.humans.flute.summon_party"));
        List<UUID> party = HumansComponents.PARTY.get(player).getList();
        summonPartyButton.setOnClick(() -> {
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeInt(party.size());
            for (UUID u : party) {
                buf.writeUuid(u);
            }
            ClientPlayNetworking.send(new Identifier("humans", "party_teleport"), buf);
            MinecraftClient client = MinecraftClient.getInstance();
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.PLAYERS, 100.0f, 1.5f);
            client.setScreen(null);
        });
        root.add(summonPartyButton, 0, 10, 4, 1);

        WButton partyStayButton = new WButton(new TranslatableText("gui.humans.flute.party_stay"));
        partyStayButton.setOnClick(() -> {
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeInt(party.size());
            for (UUID u : party) {
                buf.writeUuid(u);
            }
            ClientPlayNetworking.send(new Identifier("humans", "party_stay"), buf);
            MinecraftClient client = MinecraftClient.getInstance();
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.PLAYERS, 100.0f, 0.5f);
            client.setScreen(null);
        });
        root.add(partyStayButton, 5, 10, 4, 1);
        
        WButton partyFollowButton = new WButton(new TranslatableText("gui.humans.flute.party_follow"));
        partyFollowButton.setOnClick(() -> {
            PacketByteBuf buf = new PacketByteBuf(ByteBufAllocator.DEFAULT.buffer());
            buf.writeInt(party.size());
            for (UUID u : party) {
                buf.writeUuid(u);
            }
            ClientPlayNetworking.send(new Identifier("humans", "party_follow"), buf);
            MinecraftClient client = MinecraftClient.getInstance();
            client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.PLAYERS, 100.0f, 1.5f);
            client.setScreen(null);
        });
        root.add(partyFollowButton, 10, 10, 4, 1);

        BiConsumer<HumanEntity, HumanListItem> configurator = (HumanEntity e, HumanListItem item) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            Identifier textureId;
            if (e.getSkinProfile() != null) {
                Map<Type,MinecraftProfileTexture> map = client.getSkinProvider().getTextures(e.getSkinProfile());
                textureId = map.containsKey(Type.SKIN) ? client.getSkinProvider().loadSkin(map.get(Type.SKIN), Type.SKIN) 
                    : DefaultSkinHelper.getTexture(PlayerEntity.getUuidFromProfile(e.getSkinProfile()));
                item.name.setText(e.getCustomName());
            } else if (e.usesSlimSkin()) {
                textureId = new Identifier("minecraft", "textures/entity/alex.png");
            } else if (e.usesLegacyAnim() || e.usesLegacySound()) {
                textureId = new Identifier("humans", "textures/entity/human/legasteve.png");
            } else {
                textureId = new Identifier("minecraft", "textures/entity/steve.png");
            }
            
            item.sprite.setImage(new Texture(textureId).withUv(0.128f, 0.128f, 0.25f, 0.25f));
            Identifier stateId = new Identifier(e.getState());
            item.state.setText(new TranslatableText("entity."+stateId.getNamespace()+".human_state."+stateId.getPath()));
        };
        WListPanel<HumanEntity, HumanListItem> humanList = new WListPanel<>(humans, HumanListItem::new, configurator);
        humanList.setListItemHeight(2*18);
        root.add(humanList, 0, 3, 14, 6);
        root.validate(this);
    }
}
