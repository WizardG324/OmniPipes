package com.wizardg.omnipipes.compat.jei;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.screen.PipeScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

// Lets items and fluids be dragged from JEI onto the pipe's filter slots.
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(OmniPipes.MODID, "jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(PipeScreen.class, new IGhostIngredientHandler<>() {
            @Override
            public <I> List<Target<I>> getTargetsTyped(PipeScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
                ItemStack stack = switch (ingredient.getIngredient()) {
                    case ItemStack item -> item;
                    case FluidStack fluid -> PipeScreen.bucketOf(fluid.getFluid());
                    default -> ItemStack.EMPTY;
                };
                if (stack.isEmpty()) return List.of();
                return screen.filterSlots().stream().<Target<I>>map(slot -> new Target<>() {
                    @Override
                    public Rect2i getArea() {
                        return screen.slotArea(slot);
                    }

                    @Override
                    public void accept(I ignored) {
                        screen.dropIntoFilter(slot, stack);
                    }
                }).toList();
            }

            @Override
            public void onComplete() {}
        });
    }
}
