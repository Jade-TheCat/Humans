package dev.jadethecat.humans.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.jadethecat.humans.Humans;
import dev.jadethecat.humans.entity.HumanEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

public class HumansServerPlay {
    public static void initReceiviers() {
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
                    Entity e = player.getServerWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToTeleport.add((HumanEntity)e);
                    }
				}
                Humans.LOGGER.info(entitiesToTeleport);
				for (HumanEntity h : entitiesToTeleport) {
                    h.tryTeleportToBestFriend(pEntity);
                }
			});
		});
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "party_stay"), 
			(server2, player, handler2, buf, responseSender) -> {
				int entityCount = buf.readInt();
                List<HumanEntity> entitiesToWait = new ArrayList<>();
				for (int i = 0; i < entityCount; i++) {
					UUID u = buf.readUuid();
                    Entity e = player.getServerWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToWait.add((HumanEntity)e);
                    }
				}
				for (HumanEntity h : entitiesToWait) {
                    h.setState(HumanEntity.WAITING_STATE);
                }
			});
		});
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "party_follow"), 
			(server2, player, handler2, buf, responseSender) -> {
				int entityCount = buf.readInt();
                List<HumanEntity> entitiesToFollow = new ArrayList<>();
				for (int i = 0; i < entityCount; i++) {
					UUID u = buf.readUuid();
                    Entity e = player.getServerWorld().getEntity(u);
                    if (e instanceof HumanEntity) {
                        entitiesToFollow.add((HumanEntity)e);
                    }
				}
				for (HumanEntity h : entitiesToFollow) {
                    h.setState(HumanEntity.FOLLOWING_STATE);
                }
			});
		});
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
			ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "update_human_state"), 
			(server2, player, handler2, buf, responseSender) -> {
				UUID uuid = buf.readUuid();
                String newState = buf.readString();
                Entity e = player.getServerWorld().getEntity(uuid);
                if (e instanceof HumanEntity) {
                    ((HumanEntity)e).setState(newState);
                    ((HumanEntity)e).setHomePos(e.getBlockPos());
                } else {
                    Humans.LOGGER.error("Entity is not human, cannot set state: " + uuid.toString());
                }
			});
		});
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "update_human_flags"), 
            (server2, player, handler2, buf, responseSender) -> {
                UUID uuid = buf.readUuid();
                byte b = buf.readByte();
                Entity e = player.getServerWorld().getEntity(uuid);
                if (e instanceof HumanEntity) {
                    ((HumanEntity)e).setFlags(b);
                }
            });
        });
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ServerPlayNetworking.registerReceiver(handler, new Identifier("humans", "rename_human"), 
            (server2, player, handler2, buf, responseSender) -> {
                UUID uuid = buf.readUuid();
                String newName = buf.readString();
                Entity e = player.getServerWorld().getEntity(uuid);
                Humans.LOGGER.info("Renaming entity to " + newName);
                if (e instanceof HumanEntity) {
                    HumanEntity h = (HumanEntity)e;
                    h.setCustomName(new LiteralText(newName));
                }
            });
        });
    }
}
