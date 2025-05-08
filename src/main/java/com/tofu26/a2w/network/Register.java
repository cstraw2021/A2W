package com.tofu26.a2w.network;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

import static com.tofu26.a2w.A2W.MOD_ID;


public class Register {

    public static void initialize() {
        BalmNetworking networking = Balm.getNetworking();

        networking.registerServerboundPacket(
                id("teleport_waystone"),
                TeleportToWaystoneMessage.class,
                TeleportToWaystoneMessage::encode,
                TeleportToWaystoneMessage::decode,
                TeleportToWaystoneMessage::handle
        );

        networking.registerServerboundPacket(
                id("change_experience"),
                ChangeExperiencePacket.class,
                ChangeExperiencePacket::encode,
                ChangeExperiencePacket::decode,
                ChangeExperiencePacket::handle
        );
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}