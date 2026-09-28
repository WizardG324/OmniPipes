package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.custom.PipeBlock.Side;
import com.wizardg.omnipipes.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;

public class ModModelProvider extends BlockStateProvider {
    public ModModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, OmniPipes.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile item = pipeModels("", "cable");
        ModelFile dyedItem = pipeModels("_dyed", "cable_dyed");

        ModItems.TIER_UPGRADES.forEach(tier -> itemModels().basicItem(tier.get()));
        for (var upgrade : List.of(ModItems.CREATIVE_UPGRADE, ModItems.FLUID_UPGRADE, ModItems.ENERGY_UPGRADE, ModItems.TAG_FILTER))
            itemModels().basicItem(upgrade.get());
        itemModels().withExistingParent(ModItems.PIPE_CONFIGURATOR.getId().getPath(), mcLoc("item/handheld")) // held like a tool
                .texture("layer0", modLoc("item/pipe_configurator"));

        ModelFile pipe = combinedModel("", "cable");
        ModelFile dyedPipe = combinedModel("_dyed", "cable_dyed");
        getVariantBuilder(ModBlocks.PIPE.get()).partialState().setModels(new ConfiguredModel(pipe));
        itemModels().getBuilder(ModBlocks.PIPE.getId().getPath()).parent(item);
        ModBlocks.COLORED_PIPES.values().forEach(colored -> {
            getVariantBuilder(colored.get()).partialState().setModels(new ConfiguredModel(dyedPipe));
            itemModels().getBuilder(colored.getId().getPath()).parent(dyedItem); // tinted by the item color handler
        });
    }

    // Every pipe state uses this one model, client/PipeModel stitches the part models together per state.
    private ModelFile combinedModel(String suffix, String cable) {
        return models().getBuilder("pipe" + suffix).texture("particle", modLoc("block/" + cable))
                .customLoader((builder, existingFiles) -> new PipeLoaderBuilder(builder, existingFiles, suffix)).end();
    }

    private static final class PipeLoaderBuilder extends CustomLoaderBuilder<BlockModelBuilder> {
        private final String suffix;

        PipeLoaderBuilder(BlockModelBuilder parent, ExistingFileHelper existingFiles, String suffix) {
            super(ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "pipe"), parent, existingFiles, false);
            this.suffix = suffix;
        }

        @Override
        public JsonObject toJson(JsonObject json) {
            json = super.toJson(json);
            json.addProperty("suffix", suffix);
            return json;
        }
    }

    // One full set of pipe models using the given cable texture, returns the item model.
    private ModelFile pipeModels(String suffix, String cable) {
        coreFace("box" + suffix, cable, 12, 12);
        coreFace("u" + suffix, cable, 0, 6);
        coreFace("v" + suffix, cable, 6, 0);
        arm(model("pipe_arm" + suffix, cable), 0, 6);
        for (Side side : new Side[]{Side.INSERT, Side.EXTRACT, Side.BOTH}) {
            String mode = side.getSerializedName();
            BlockModelBuilder port = model("pipe_arm_" + mode + suffix, cable).renderType("cutout")
                    .texture("port", modLoc("block/pipe_" + mode)).texture("port_v", modLoc("block/pipe_" + mode + "_v"));
            plate(port, 2.5f, 0, 11, 0);
            plate(port, 4.5f, 0.5f, 7, 2);
            arm(port, 1, 6);
            arrows(port);
        }
        BlockModelBuilder item = model("pipe_item" + suffix, cable).parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")));
        cap(arm(item, 0, 6), Direction.NORTH);
        arm(item, 6, 10);
        cap(arm(item, 10, 16), Direction.SOUTH);
        return item;
    }

    private BlockModelBuilder model(String name, String cable) {
        return models().getBuilder(name)
                .texture("particle", modLoc("block/" + cable))
                .texture("cable", modLoc("block/" + cable))
                .texture("connector", modLoc("block/connector"));
    }

    // One core face (built facing north, rotated like the arms): the box joint, or tube stripes running along u or v.
    private void coreFace(String name, String cable, float u, float v) {
        model("pipe_core_" + name, cable).element().from(6, 6, 6).to(10, 10, 10)
                .face(Direction.NORTH).uvs(u, v, u + 4, v + 4).texture("#cable").tintindex(0).end().end();
    }

    // 4px conduit along z (tintindex 0), the uvs pick the strip of cable.png running lengthwise.
    private static ModelBuilder<BlockModelBuilder>.ElementBuilder arm(BlockModelBuilder model, float z1, float z2) {
        float len = z2 - z1;
        var element = model.element().from(6, 6, z1).to(10, 10, z2);
        element.face(Direction.EAST).uvs(0, 6, len, 10).texture("#cable").tintindex(0).end();
        element.face(Direction.WEST).uvs(0, 6, len, 10).texture("#cable").tintindex(0).end();
        element.face(Direction.UP).uvs(6, 0, 10, len).texture("#cable").tintindex(0).end();
        element.face(Direction.DOWN).uvs(6, 0, 10, len).texture("#cable").tintindex(0).end();
        return element;
    }

    // Closes an open conduit end on the item model.
    private static void cap(ModelBuilder<BlockModelBuilder>.ElementBuilder element, Direction side) {
        element.face(side).uvs(12, 12, 16, 16).texture("#cable").tintindex(0).end();
    }

    // EnderIO style thin plate step on the neighboring block, size x size and 0.5px deep starting at depth z.
    // The texture's frame covers pixels 0-10, a smaller plate reads from inset so it gets plain steel.
    private static void plate(BlockModelBuilder model, float from, float z, float size, float inset) {
        float to = from + size;
        var element = model.element().from(from, from, z).to(to, to, z + 0.5f);
        element.face(Direction.NORTH).uvs(inset, inset, inset + size, inset + size).texture("#connector").cullface(Direction.NORTH).end();
        element.face(Direction.SOUTH).uvs(inset, inset, inset + size, inset + size).texture("#connector").end();
        element.face(Direction.EAST).uvs(0, 0, 0.5f, size).texture("#connector").end();
        element.face(Direction.WEST).uvs(0, 0, 0.5f, size).texture("#connector").end();
        element.face(Direction.UP).uvs(0, 0, size, 0.5f).texture("#connector").end();
        element.face(Direction.DOWN).uvs(0, 0, size, 0.5f).texture("#connector").end();
    }

    // EnderIO style mode arrows on a see-through shell just outside the cable. The textures point toward the block:
    // port for east/west (flipped on west), port_v for up/down (flipped on down). The model renders cutout.
    private static void arrows(BlockModelBuilder model) {
        var element = model.element().from(5.8f, 5.8f, 1).to(10.2f, 10.2f, 6);
        element.face(Direction.EAST).uvs(0, 0, 16, 16).texture("#port").end();
        element.face(Direction.WEST).uvs(16, 0, 0, 16).texture("#port").end();
        element.face(Direction.UP).uvs(0, 0, 16, 16).texture("#port_v").end();
        element.face(Direction.DOWN).uvs(0, 16, 16, 0).texture("#port_v").end();
    }
}
