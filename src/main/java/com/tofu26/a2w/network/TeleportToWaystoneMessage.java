package com.tofu26.a2w.network;

import com.mojang.datafixers.util.Either;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystoneTeleportError;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneProxy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static net.blay09.mods.waystones.core.WarpMode.WARP_STONE;

public class TeleportToWaystoneMessage {

    private final UUID waystoneUid;

    public TeleportToWaystoneMessage(UUID waystoneUid) {
        this.waystoneUid = waystoneUid;
    }

    public static void encode(TeleportToWaystoneMessage message, FriendlyByteBuf buf) {
        buf.writeUUID(message.waystoneUid);
    }

    public static TeleportToWaystoneMessage decode(FriendlyByteBuf buf) {
        return new TeleportToWaystoneMessage(buf.readUUID());
    }

    public static void handle(ServerPlayer player, TeleportToWaystoneMessage message) {
        WaystoneProxy waystone = new WaystoneProxy(player.server, message.waystoneUid);
        Either<IWaystoneTeleportContext, WaystoneTeleportError> result = WaystonesAPI.createCustomTeleportContext(player, waystone);

        result.ifLeft(context -> {
            PlayerWaystoneManager.getExperienceLevelCost(player, waystone, WARP_STONE, context); // optionally use result
            WaystonesAPI.tryTeleport(context);
        });

        result.ifRight(error -> {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Teleport failed: " + error.getClass().getSimpleName()),
                    true
            );
        });
    }
}