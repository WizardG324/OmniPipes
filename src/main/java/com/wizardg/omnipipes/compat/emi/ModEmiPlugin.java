package com.wizardg.omnipipes.compat.emi;

import com.wizardg.omnipipes.screen.PipeScreen;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

// Keeps EMI clear of the pipe screen's side parts, and lets items and fluids be dragged onto its filter slots.
@EmiEntrypoint
public class ModEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(PipeScreen.class, (screen, out) -> screen.extraAreas().forEach(area -> out.accept(bounds(area))));
        registry.addDragDropHandler(PipeScreen.class, new EmiDragDropHandler.BoundsBased<PipeScreen>((screen, out) -> {
            for (Slot slot : screen.filterSlots())
                out.accept(bounds(screen.slotArea(slot)), dragged -> screen.dropIntoFilter(slot, toStack(dragged)));
        }));
    }

    private static Bounds bounds(Rect2i area) {
        return new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    // Only single stacks, a tag ingredient has no one item to filter.
    private static ItemStack toStack(EmiIngredient dragged) {
        if (dragged.getEmiStacks().size() != 1) return ItemStack.EMPTY;
        EmiStack stack = dragged.getEmiStacks().getFirst();
        return stack.getKey() instanceof Fluid fluid ? PipeScreen.bucketOf(fluid) : stack.getItemStack();
    }
}
