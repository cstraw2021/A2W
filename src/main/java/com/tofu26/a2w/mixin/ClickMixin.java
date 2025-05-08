package com.tofu26.a2w.mixin;

import com.tofu26.a2w.network.ChangeExperiencePacket;
import com.tofu26.a2w.network.TeleportToWaystoneMessage;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneTeleportContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.client.screen.MapWidget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import static net.blay09.mods.waystones.core.WarpMode.WARP_STONE;

@Mixin(MapWidget.class)
public class ClickMixin {


    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void printCoordinatesOnClick(double mouseX, double mouseY, int pButton, CallbackInfoReturnable<Boolean> cir) {

        MapWidget widget = (MapWidget) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Collection<IWaystone> activatedWaystones = WaystonesAPI.getActivatedWaystones(player);

        try {
            Field mapScreenField = MapWidget.class.getDeclaredField("mapScreen");
            mapScreenField.setAccessible(true); // Make the private field accessible
            Object mapScreen = mapScreenField.get(widget);
            if (mapScreen == null) return;

            Field isHoveredField = MapWidget.class.getDeclaredField("isHovered");
            isHoveredField.setAccessible(true);
            boolean isHovered = (boolean) isHoveredField.get(widget);

            if (!isHovered) return;

            java.lang.reflect.Method getHoveredPosMethod = MapWidget.class.getDeclaredMethod("getHoveredPos", double.class, double.class);
            getHoveredPosMethod.setAccessible(true);
            ColumnPos playerPos = (ColumnPos) getHoveredPosMethod.invoke(widget, mouseX, mouseY);

            Method getSelectedSliceMethod = mapScreen.getClass().getDeclaredMethod("getSelectedSlice");
            getSelectedSliceMethod.setAccessible(true); // Make the private method accessible
            Object selectedSlice = getSelectedSliceMethod.invoke(mapScreen); // Invoke the method
            if (selectedSlice == null) {
                throw new NullPointerException("selectedSlice is null");
            }
            // Get zoom level
            Field zoomLevelField = MapWidget.class.getDeclaredField("targetZoomLevel");
            zoomLevelField.setAccessible(true);
            float zoomLevel = zoomLevelField.getFloat(widget);
            int hoverAreaRadius = 4 + Math.round(4 * zoomLevel);

            Method dimensionMethod = selectedSlice.getClass().getDeclaredMethod("dimension");
            dimensionMethod.setAccessible(true); // Make the method accessible
            Object dimension = dimensionMethod.invoke(selectedSlice); // Invoke to get the dimension

            // Reflect to access decorationBookmarks from AtlasOverviewScreen
            Field decorationBookmarksField = mapScreen.getClass().getDeclaredField("decorationBookmarks");
            decorationBookmarksField.setAccessible(true);
            List<?> decorationBookmarks = (List<?>) decorationBookmarksField.get(mapScreen);

            for (IWaystone waystone : activatedWaystones) {
                BlockPos waystonePos = waystone.getPos();

                boolean withinHover = waystone.getDimension() == dimension &&
                        playerPos.x() >= waystonePos.getX() - hoverAreaRadius &&
                        playerPos.x() <= waystonePos.getX() + hoverAreaRadius &&
                        playerPos.z() >= waystonePos.getZ() - hoverAreaRadius &&
                        playerPos.z() <= waystonePos.getZ() + hoverAreaRadius;

                if (!withinHover) continue;

                // Check for matching decoration button
                boolean foundMatch = false;
                for (Object obj : decorationBookmarks) {
                    if (!(obj instanceof DecorationBookmarkButton button)) continue;
                    if ((int) Math.floor(button.getWorldX()) == waystonePos.getX() && (int) Math.floor(button.getWorldZ()) == waystonePos.getZ()) {
                        foundMatch = true;
                        break;
                    }
                }

                if (foundMatch) {
                    IWaystoneTeleportContext context = new WaystoneTeleportContext(null, null, null);
                    int xpLevelCost = PlayerWaystoneManager.getExperienceLevelCost(player, waystone, WARP_STONE, context);

                    assert player != null;
                    if (player.experienceLevel >= xpLevelCost) {
                        Balm.getNetworking().sendToServer(new TeleportToWaystoneMessage(waystone.getWaystoneUid()));
                        ChangeExperiencePacket packet = new ChangeExperiencePacket(-xpLevelCost);
                        Balm.getNetworking().sendToServer(packet); // Send the packet to the server
                    } else {
                        player.displayClientMessage(
                                Component.translatable("message.a2w.no_experience", xpLevelCost),
                                true
                        );
                    }
                    player.closeContainer();
                    return;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();  // Handle any reflection issues (e.g., access exceptions)
        }
    }
}