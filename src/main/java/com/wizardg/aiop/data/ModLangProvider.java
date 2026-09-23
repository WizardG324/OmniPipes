package com.wizardg.aiop.data;

import com.wizardg.aiop.AIOPAllinOnePipe;
import com.wizardg.aiop.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLangProvider extends LanguageProvider {

    public ModLangProvider(PackOutput output) {
        super(output, AIOPAllinOnePipe.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        //Creative Tab
        add("itemGroup.aiop", "All in One Pipe");

        //Blocks
        add(ModBlocks.PIPE.get(), "Pipe");
        ModBlocks.COLORED_PIPES.forEach((color, pipe) -> add(pipe.get(), titleCase(color.getName()) + " Pipe"));
    }

    // light_blue -> Light Blue
    private static String titleCase(String name) {
        StringBuilder out = new StringBuilder();
        for (String word : name.split("_")) out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        return out.toString();
    }
}
