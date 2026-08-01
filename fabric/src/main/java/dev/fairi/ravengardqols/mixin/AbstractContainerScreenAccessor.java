package dev.fairi.ravengardqols.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int ravengardqols$getLeftPos();

    @Accessor("topPos")
    int ravengardqols$getTopPos();

    @Accessor("imageWidth")
    int ravengardqols$getImageWidth();

    @Accessor("imageHeight")
    int ravengardqols$getImageHeight();

    @Accessor("hoveredSlot")
    Slot ravengardqols$getHoveredSlot();
}
