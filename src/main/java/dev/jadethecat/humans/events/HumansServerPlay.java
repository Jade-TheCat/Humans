package dev.jadethecat.humans.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.components.HumansComponents;
import dev.jadethecat.humans.entity.HumanEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class HumansServerPlay {
    public static void initServerPlayEvents() {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "party_teleport"), 
			(server2, player, handler2, buf, responseSender) -> {
                Humans.LOGGER.info("Recieved party_teleport");
				PlayerEntity pEntity = player.getEntityWorld().getPlayerByUuid(player.getUuid());
				int entityCount = buf.readInt();
                Humans.LOGGER.info(entityCount);
                List<HumanEntity> entitiesToTeleport = new ArrayList<>();
				for (int i = 0; i < entityCount; i++) {
					UUID u = buf.readUuid();
                    Entity e = player.getWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToTeleport.add((HumanEntity)e);
                    }
				}
                Humans.LOGGER.info(entitiesToTeleport);
				for (HumanEntity h : entitiesToTeleport) {
                    h.tryTeleportToBestFriend(pEntity);
                }
			});
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "party_stay"), 
			(server2, player, handler2, buf, responseSender) -> {
				int entityCount = buf.readInt();
                List<HumanEntity> entitiesToWait = new ArrayList<>();
				for (int i = 0; i < entityCount; i++) {
					UUID u = buf.readUuid();
                    Entity e = player.getWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToWait.add((HumanEntity)e);
                    }
				}
				for (HumanEntity h : entitiesToWait) {
                    h.setState(HumanEntity.WAITING_STATE);
                }
			});
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "party_follow"), 
			(server2, player, handler2, buf, responseSender) -> {
				int entityCount = buf.readInt();
                List<HumanEntity> entitiesToFollow = new ArrayList<>();
				for (int i = 0; i < entityCount; i++) {
					UUID u = buf.readUuid();
                    Entity e = player.getWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToFollow.add((HumanEntity)e);
                    }
				}
				for (HumanEntity h : entitiesToFollow) {
                    h.setState(HumanEntity.FOLLOWING_STATE);
                }
			});
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "update_human_state"), 
			(server2, player, handler2, buf, responseSender) -> {
				UUID uuid = buf.readUuid();
                String newState = buf.readString();
                Entity e = player.getWorld().getEntity(uuid);
                if (e instanceof HumanEntity) {
                    HumanEntity h = (HumanEntity)e;
                    h.setState(newState);
                    if (newState == HumanEntity.SENTRY_STATE) {
                        h.setHomePos(e.getBlockPos());
                        if (h.getBestFriend().isPresent() && h.getBestFriend().get() == player.getUuid()) {
                            HumansComponents.PARTY.get(player.getEntityWorld().getPlayerByUuid(player.getUuid())).remove(h.getUuid());
                        }
                    } else if (newState != HumanEntity.NONE_STATE) {
                        if (h.getBestFriend().isPresent() && h.getBestFriend().get() == player.getUuid()) {
                            HumansComponents.PARTY.get(player.getEntityWorld().getPlayerByUuid(player.getUuid())).add(h.getUuid());
                        }
                    }
                } else {
                    Humans.LOGGER.error("Entity is not human, cannot set state: " + uuid.toString());
                }
			});
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "update_human_flags"), 
            (server2, player, handler2, buf, responseSender) -> {
                UUID uuid = buf.readUuid();
                byte b = buf.readByte();
                Entity e = player.getWorld().getEntity(uuid);
                if (e instanceof HumanEntity) {
                    ((HumanEntity)e).setFlags(b);
                }
            });
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "rename_human"), 
            (server2, player, handler2, buf, responseSender) -> {
                UUID uuid = buf.readUuid();
                String newName = buf.readString();
                Entity e = player.getWorld().getEntity(uuid);
                Humans.LOGGER.info("Renaming entity to " + newName);
                if (e instanceof HumanEntity) {
                    HumanEntity h = (HumanEntity)e;
                    h.setCustomName(new LiteralText(newName));
                }
            });
		});
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerEntity p = handler.player.getEntityWorld().getPlayerByUuid(handler.player.getUuid());
            if (HumansComponents.PARTY.get(p) == null) return;
            HumansComponents.PARTY.get(p).getList().forEach(human -> {
                Entity e = handler.player.getWorld().getEntity(human);
                if (e instanceof HumanEntity) {
                    HumanEntity h = ((HumanEntity)e);
                    h.setState(HumanEntity.WAITING_STATE);
                } else {
                    HumansComponents.PARTY.get(p).remove(human);
                }
            });
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PlayerEntity p = newPlayer.getEntityWorld().getPlayerByUuid(newPlayer.getUuid());
            if (HumansComponents.PARTY.get(p) != null) {
                List<UUID> party = HumansComponents.PARTY.get(p).getList();
                for (UUID uuid : party) {
                    Entity e = newPlayer.getWorld().getEntity(uuid);
                    if (e instanceof HumanEntity) {
                        HumanEntity h = (HumanEntity)e;
                        if (h.getBestFriend().isPresent() && h.getBestFriend().get() != newPlayer.getUuid()) {
                            party.remove(uuid);
                        }
                    } else {
                        party.remove(uuid);
                    }
                }
            }
        });
    }
}
