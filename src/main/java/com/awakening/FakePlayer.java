package com.awakening;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NPC pemain palsu (packet-only, lewat ProtocolLib). Cuma ada di sisi client.
 * Dibuat buat Paper 1.21.1 (packet 1.21.1; 1.21.2+ format teleport packet beda).
 */
public final class FakePlayer {

    private static final AtomicInteger IDS = new AtomicInteger(1_800_000_000);

    private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
    private final int id = IDS.incrementAndGet();
    private final UUID uuid = UUID.randomUUID();
    private final String name = "awk" + Integer.toHexString(id & 0xFFFFFF);
    private final WrappedGameProfile profile;
    private final List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = new ArrayList<>();
    private final Set<Player> viewers = new LinkedHashSet<>();
    private Team team;
    private Location loc;

    public FakePlayer(String textureValue, String textureSignature) {
        profile = new WrappedGameProfile(uuid, name);
        if (textureValue != null) {
            profile.getProperties().put("textures", new WrappedSignedProperty("textures", textureValue,
                    textureSignature == null ? "" : textureSignature));
        }
        // sembunyiin nametag (pakai team di main scoreboard)
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            Team old = sb.getTeam(name);
            if (old != null) old.unregister();
            team = sb.registerNewTeam(name);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.addEntry(name);
        } catch (Throwable ignored) {
            team = null;
        }
    }

    public void equip(EnumWrappers.ItemSlot slot, ItemStack item) {
        if (item != null && !item.getType().isAir()) equipment.add(new Pair<>(slot, item.clone()));
    }

    // ------------------------------------------------------------------

    /** Munculin NPC di lokasi ini ke sekumpulan player. */
    public void spawn(Location at, Iterable<? extends Player> to) {
        this.loc = at.clone();
        for (Player p : to) show(p);
    }

    /** Munculin ke satu player (dipakai juga buat yang baru join). */
    public void show(Player p) {
        if (loc == null || !viewers.add(p)) return;

        PacketContainer info = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
        info.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(name));
        info.getPlayerInfoDataLists().write(1, List.of(data));
        send(p, info);

        PacketContainer sp = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        sp.getIntegers().write(0, id);
        sp.getUUIDs().write(0, uuid);
        sp.getEntityTypeModifier().write(0, EntityType.PLAYER);
        sp.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());
        sp.getBytes().write(0, angle(loc.getPitch())).write(1, angle(loc.getYaw())).write(2, angle(loc.getYaw()));
        send(p, sp);

        // semua layer skin (jaket, topi, dll) nyala
        PacketContainer meta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        meta.getIntegers().write(0, id);
        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x7F));
        meta.getDataValueCollectionModifier().write(0, values);
        send(p, meta);

        if (!equipment.isEmpty()) {
            PacketContainer eq = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
            eq.getIntegers().write(0, id);
            eq.getSlotStackPairLists().write(0, new ArrayList<>(equipment));
            send(p, eq);
        }
    }

    /** Pindah + hadap (yaw/pitch dari Location). Dikirim ke semua yang lagi liat. */
    public void move(Location to) {
        this.loc = to.clone();
        PacketContainer tp = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        tp.getIntegers().write(0, id);
        tp.getDoubles().write(0, to.getX()).write(1, to.getY()).write(2, to.getZ());
        tp.getBytes().write(0, angle(to.getYaw())).write(1, angle(to.getPitch()));
        tp.getBooleans().write(0, false);

        PacketContainer head = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, id);
        head.getBytes().write(0, angle(to.getYaw()));

        for (Player p : viewers) {
            send(p, tp);
            send(p, head);
        }
    }

    /** Hapus NPC dari semua yang liat. */
    public void remove() {
        PacketContainer destroy = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, List.of(id));
        PacketContainer rm = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        rm.getUUIDLists().write(0, List.of(uuid));
        for (Player p : viewers) {
            if (!p.isOnline()) continue;
            send(p, destroy);
            send(p, rm);
        }
        viewers.clear();
        if (team != null) {
            try {
                team.unregister();
            } catch (Throwable ignored) {
            }
            team = null;
        }
    }

    private void send(Player p, PacketContainer packet) {
        try {
            pm.sendServerPacket(p, packet);
        } catch (Exception ex) {
            Bukkit.getLogger().warning("[AwakeningCutscene] Gagal kirim packet NPC ke " + p.getName() + ": " + ex);
        }
    }

    private static byte angle(float deg) {
        return (byte) Math.floor(deg * 256.0f / 360.0f);
    }
}
