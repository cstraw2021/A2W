package com.tofu26.a2w.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ChangeExperiencePacket {

    private final int xpChange;

    public ChangeExperiencePacket(int xpChange) {
        this.xpChange = xpChange;
    }


    public static void encode(ChangeExperiencePacket message, FriendlyByteBuf buf) {
        buf.writeInt(message.xpChange);
    }


    public static ChangeExperiencePacket decode(FriendlyByteBuf buf) {
        return new ChangeExperiencePacket(buf.readInt());
    }


    public static void handle(ServerPlayer player, ChangeExperiencePacket message) {
        int xpChange = message.xpChange;

        player.giveExperienceLevels(xpChange);

        // send the updated XP to the client to keep things in sync
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                player.experienceProgress, player.totalExperience, player.experienceLevel));
    }
}