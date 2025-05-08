package com.tofu26.a2w.mixin;


import net.minecraft.server.level.ColumnPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.client.screen.MapWidget;

@Mixin(MapWidget.class)
public class ClickMixin {


    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void printCoordinatesOnClick(double mouseX, double mouseY, int pButton, CallbackInfoReturnable<Boolean> cir) {
        // Using reflection to access the private getHoveredPos method from MapWidget
        try {
            MapWidget widget = (MapWidget) (Object) this;

            // Access the getHoveredPos method via reflection
            java.lang.reflect.Method getHoveredPosMethod = MapWidget.class.getDeclaredMethod("getHoveredPos", double.class, double.class);
            getHoveredPosMethod.setAccessible(true);
            ColumnPos pos = (ColumnPos) getHoveredPosMethod.invoke(widget, mouseX, mouseY);

            // Print the coordinates of the clicked position to the console
            System.out.println("clicked (" + pos.x() + ", " + pos.z() + ")");
        } catch (Exception e) {
            e.printStackTrace();  // If reflection fails, print the error stack trace
        }
    }
}