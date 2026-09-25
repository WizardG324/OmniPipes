package com.wizardg.omnipipes.compat.rei;

import com.wizardg.omnipipes.screen.PipeScreen;
import dev.architectury.fluid.FluidStack;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

// Keeps REI clear of the pipe screen's side parts, and lets items and fluids be dragged onto its filter slots.
@REIPluginClient
public class ModReiPlugin implements REIClientPlugin {
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(PipeScreen.class, (PipeScreen screen) -> screen.extraAreas().stream()
                .map(area -> new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight())).toList());
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackVisitor(new DraggableStackVisitor<PipeScreen>() {
            @Override
            public <R extends Screen> boolean isHandingScreen(R screen) {
                return screen instanceof PipeScreen;
            }

            @Override
            public DraggedAcceptorResult acceptDraggedStack(DraggingContext<PipeScreen> context, DraggableStack dragged) {
                ItemStack stack = toStack(dragged.getStack().getValue());
                if (stack.isEmpty()) return DraggedAcceptorResult.PASS;
                PipeScreen screen = context.getScreen();
                var pos = context.getCurrentPosition();
                for (Slot slot : screen.filterSlots()) {
                    if (screen.slotArea(slot).contains(pos.x, pos.y)) {
                        screen.dropIntoFilter(slot, stack);
                        return DraggedAcceptorResult.CONSUMED;
                    }
                }
                return DraggedAcceptorResult.PASS;
            }

            @Override
            public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<PipeScreen> context, DraggableStack dragged) {
                if (toStack(dragged.getStack().getValue()).isEmpty()) return Stream.empty();
                PipeScreen screen = context.getScreen();
                return screen.filterSlots().stream().map(slot -> {
                    Rect2i area = screen.slotArea(slot);
                    return BoundsProvider.ofRectangle(new Rectangle(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
                });
            }
        });
    }

    private static ItemStack toStack(Object value) {
        return switch (value) {
            case ItemStack item -> item;
            case FluidStack fluid -> PipeScreen.bucketOf(fluid.getFluid());
            default -> ItemStack.EMPTY;
        };
    }
}
