package com.tofu26.a2w.mixin;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.IWaystoneTeleportContext;
import net.blay09.mods.waystones.api.WaystonesAPI;
import net.blay09.mods.waystones.core.PlayerWaystoneManager;
import net.blay09.mods.waystones.core.WaystoneTeleportContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.client.screen.MapWidget;

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

        MapWidget widget = (MapWidget) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Collection<IWaystone> activatedWaystones = WaystonesAPI.getActivatedWaystones(player);

        try {
            // Reflect to access 'mapScreen' from MapWidget
            Field mapScreenField = MapWidget.class.getDeclaredField("mapScreen");
            mapScreenField.setAccessible(true);
            Object mapScreen = mapScreenField.get(widget);
            if (mapScreen == null) return;

            // Check hover status
            Field isHoveredField = MapWidget.class.getDeclaredField("isHovered");
            isHoveredField.setAccessible(true);
            boolean isHovered = (boolean) isHoveredField.get(widget);
            if (!isHovered) return;

            // Get hovered column position
            Method getHoveredPosMethod = MapWidget.class.getDeclaredMethod("getHoveredPos", double.class, double.class);
            getHoveredPosMethod.setAccessible(true);
            ColumnPos playerPos = (ColumnPos) getHoveredPosMethod.invoke(widget, pMouseX, pMouseY);

            // Reflect to get selected slice and dimension
            Method getSelectedSliceMethod = mapScreen.getClass().getDeclaredMethod("getSelectedSlice");
            getSelectedSliceMethod.setAccessible(true);
            Object selectedSlice = getSelectedSliceMethod.invoke(mapScreen);
            if (selectedSlice == null) return;

            Method dimensionMethod = selectedSlice.getClass().getDeclaredMethod("dimension");
            dimensionMethod.setAccessible(true);
            Object dimension = dimensionMethod.invoke(selectedSlice);

            Field zoomLevelField = MapWidget.class.getDeclaredField("targetZoomLevel");
            zoomLevelField.setAccessible(true);
            float zoomLevel = zoomLevelField.getFloat(widget);
            int hoverAreaRadius = 4 + Math.round(4 * zoomLevel);

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

                    // Create styled components
                    MutableComponent warpText = Component.literal("Warp Here")
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))); // White

                    Component levelCost = null;
                    if (xpLevelCost > 0) {
                        assert player != null;
                        if (player.experienceLevel < xpLevelCost) {
                            levelCost = Component.literal(String.format(" (%d Levels)", xpLevelCost))
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555))); // Red
                        } else {
                            levelCost = Component.literal(String.format(" (%d Levels)", xpLevelCost))
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))); // Green
                        }
                    }

                    Component tooltip = levelCost != null ? warpText.append(levelCost) : warpText;

                    graphics.renderTooltip(mc.font, tooltip, pMouseX, pMouseY);
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}