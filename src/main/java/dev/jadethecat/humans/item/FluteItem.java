package dev.jadethecat.humans.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.jadethecat.humans.client.ui.FlutePartyGui;
import dev.jadethecat.humans.client.ui.FluteScreen;
import dev.jadethecat.humans.components.HumansComponents;
import dev.jadethecat.humans.entity.HumanEntity;
import dev.jadethecat.humans.mixin.WorldInvoker;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class FluteItem extends Item {
    public FluteItem() {
        super(new FabricItemSettings().maxCount(1).group(ItemGroup.TOOLS));
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        HitResult hitResult = user.raycast(3.0, 0.0F, false);
        if (world.isClient && hitResult.getType() != HitResult.Type.ENTITY) {
            List<UUID> party = HumansComponents.PARTY.get(user).getList();
            List<HumanEntity> humans = new ArrayList<>();
            for (UUID u : party) {
                Entity e = ((WorldInvoker)world).invokeEntityLookup().get(u);
                if (e instanceof HumanEntity) {
                    humans.add((HumanEntity)e);
                }
                MinecraftClient.getInstance().setScreen(new FluteScreen(new FlutePartyGui(user, humans)));
            }
        }
        return super.use(world, user, hand);
    }
}
