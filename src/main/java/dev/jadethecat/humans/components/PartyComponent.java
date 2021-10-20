package dev.jadethecat.humans.components;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class PartyComponent implements ListComponent<UUID>, AutoSyncedComponent {
    private List<UUID> value = new ArrayList<UUID>();
    private final Object provider;
    public PartyComponent(Object provider) {
        this.provider = provider;
    }
    @Override
    public void readFromNbt(NbtCompound tag) {
        if (tag.contains("Party") && tag.getType("Party") == NbtElement.LIST_TYPE) {
            NbtList nbtList = tag.getList("Party", 11);
            List<UUID> valTemp = new ArrayList<UUID>();
            for (NbtElement el : nbtList) {
                if (el.getType() == NbtElement.INT_ARRAY_TYPE) {
                    valTemp.add(NbtHelper.toUuid(el));
                }
            }
            this.value = valTemp;
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (UUID u : this.value) {
            list.add(NbtHelper.fromUuid(u));
        }
        tag.put("Party", list);
    }

    @Override
    public List<UUID> getList() {
        return this.value;
    }

    @Override
    public boolean add(UUID value) {
        boolean out = this.value.add(value);
        HumansComponents.PARTY.sync(this.provider);
        return out;
    }

    @Override
    public UUID get(int i) {
        return this.value.get(i);
    }

    @Override
    public UUID set(int i, UUID value) {
        UUID out = this.value.set(i, value);
        HumansComponents.PARTY.sync(this.provider);
        return out;
    }

    @Override
    public void writeSyncPacket(PacketByteBuf buf, ServerPlayerEntity player) {
        buf.writeInt(this.value.size());
        for (UUID uuid : this.value) {
            buf.writeUuid(uuid);
        }
    }

    @Override
    public void applySyncPacket(PacketByteBuf buf) {
        int size = buf.readInt();
        this.value = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.value.add(buf.readUuid());
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.provider;
    }

    @Override
    public boolean remove(UUID value) {
        if (this.value.contains(value)) {
            return this.value.removeIf((uuid) -> uuid.equals(value));
        } else {
            return false;
        }
    }
}

interface ListComponent<T> extends Component {
    List<T> getList();
    boolean add(T value);
    T get(int i);
    T set(int i, T value);
    boolean remove(T value);
}
