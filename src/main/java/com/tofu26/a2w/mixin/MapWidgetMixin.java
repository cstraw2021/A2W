package com.tofu26.a2w.mixin;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WarpMode;
import net.blay09.mods.waystones.core.Waystone;
import net.blay09.mods.waystones.core.WaystoneTeleportContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.MapWidget;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static net.blay09.mods.waystones.core.PlayerWaystoneManager.getExperienceLevelCost;
import static net.blay09.mods.waystones.core.WarpMode.WARP_STONE;
import static net.blay09.mods.waystones.core.WarpMode.WAYSTONE_TO_WAYSTONE;

@Mixin(MapWidget.class)
public class MapWidgetMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void addCoordinatesToTeleportTooltip(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {

        MapWidget widget = (MapWidget) (Object) this;  // Get the current MapWidget instance
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;  // Get the current player
        Collection<IWaystone> activatedWaystones = WaystonesAPI.getActivatedWaystones(player);

        // Using reflection to access the private mapScreen field
        try {
            Field mapScreenField = MapWidget.class.getDeclaredField("mapScreen");
            mapScreenField.setAccessible(true); // Make the private field accessible
            Object mapScreen = mapScreenField.get(widget);

            // Using reflection to access the private isHovered field
            Field isHoveredField = MapWidget.class.getDeclaredField("isHovered");
            isHoveredField.setAccessible(true);
            boolean isHovered = (boolean) isHoveredField.get(widget);

            // Only proceed if the widget is hovered and teleporting is allowed
            if (isHovered && mapScreen != null){
                // Using reflection to access the private getHoveredPos method
                java.lang.reflect.Method getHoveredPosMethod = MapWidget.class.getDeclaredMethod("getHoveredPos", double.class, double.class);
                getHoveredPosMethod.setAccessible(true);
                ColumnPos playerPos = (ColumnPos) getHoveredPosMethod.invoke(widget, pMouseX, pMouseY);

                // Use reflection to access the private getSelectedSlice() method
                Method getSelectedSliceMethod = mapScreen.getClass().getDeclaredMethod("getSelectedSlice");
                getSelectedSliceMethod.setAccessible(true); // Make the private method accessible
                Object selectedSlice = getSelectedSliceMethod.invoke(mapScreen); // Invoke the method
                if (selectedSlice == null) {
                    throw new NullPointerException("selectedSlice is null");
                }

                Method dimensionMethod = selectedSlice.getClass().getDeclaredMethod("dimension");
                dimensionMethod.setAccessible(true); // Make the method accessible
                Object dimension = dimensionMethod.invoke(selectedSlice); // Invoke to get the dimension

                for (IWaystone waystone : activatedWaystones) {
                    BlockPos waystonePos = waystone.getPos();
                    Field zoomLevelField = MapWidget.class.getDeclaredField("targetZoomLevel");
                    zoomLevelField.setAccessible(true);
                    float zoomLevel = zoomLevelField.getFloat(widget);  // assuming it's a float
                    // Check if the hovered position falls within a 5x5 area around the waystone
                    int hoverAreaRadius = 4+Math.round(4 * zoomLevel);  // for example

                    if (waystone.getDimension() == dimension && playerPos.x() >= waystonePos.getX() - (2+zoomLevel) && playerPos.x() <= waystonePos.getX() + hoverAreaRadius &&
                            playerPos.z() >= waystonePos.getZ() - (2+zoomLevel) && playerPos.z() <= waystonePos.getZ() + hoverAreaRadius) {
                        // --- Required Setup ---
                        IWaystoneTeleportContext context = new WaystoneTeleportContext(null, null, null);
                        // Provide proper values depending on your mod's context
                        int xpLevelCost = PlayerWaystoneManager.getExperienceLevelCost(player, waystone, WARP_STONE, context);

                        String coords = String.format("Waystone: %s (%d XP)",
                                waystone.getName(),
                                xpLevelCost);

                        graphics.renderTooltip(mc.font, Component.literal(coords).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00FF00))), pMouseX, pMouseY);
                        return;  // Only render one tooltip
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();  // Handle any reflection issues (e.g., access exceptions)
        }
    }
}