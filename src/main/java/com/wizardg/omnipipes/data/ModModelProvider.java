package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.custom.PipeBlock.Side;
import com.wizardg.omnipipes.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.ElementBuilder;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplate;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class ModModelProvider extends ModelProvider {
    private static final TextureSlot CABLE = TextureSlot.create("cable");
    private static final TextureSlot CONNECTOR = TextureSlot.create("connector");
    private static final TextureSlot PORT = TextureSlot.create("port");

    // Models are built facing north (tip at z=0), the blockstate rotates them per side.
    private static final Map<Direction, VariantMutator> ROTATIONS = Map.of(
            Direction.NORTH, NOP, Direction.EAST, Y_ROT_90, Direction.SOUTH, Y_ROT_180,
            Direction.WEST, Y_ROT_270, Direction.UP, X_ROT_270, Direction.DOWN, X_ROT_90);

    private static final ExtendedModelTemplate CORE = template().element(core()).build();
    private static final ExtendedModelTemplate ARM = template().element(arm(0, 6)).build();
    private static final ExtendedModelTemplate PORT_ARM = template().requiredTextureSlot(CONNECTOR).requiredTextureSlot(PORT)
            .element(plate()).element(ring()).element(arm(3, 6)).build();
    private static final ExtendedModelTemplate ITEM = template().parent(Identifier.withDefaultNamespace("block/block"))
            .element(arm(0, 6).andThen(cap(Direction.NORTH))).element(core()).element(arm(10, 16).andThen(cap(Direction.SOUTH))).build();

    public ModModelProvider(PackOutput output) {
        super(output, OmniPipes.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Identifier item = pipeModels(blockModels, "", "cable");
        Identifier dyedItem = pipeModels(blockModels, "_dyed", "cable_dyed");

        ModItems.TIER_UPGRADES.forEach(tier -> itemModels.generateFlatItem(tier.get(), ModelTemplates.FLAT_ITEM));
        for (var upgrade : List.of(ModItems.CREATIVE_UPGRADE, ModItems.FLUID_UPGRADE, ModItems.ENERGY_UPGRADE))
            itemModels.generateFlatItem(upgrade.get(), ModelTemplates.FLAT_ITEM);

        pipeBlockState(blockModels, ModBlocks.PIPE.get(), "");
        itemModels.itemModelOutput.accept(ModBlocks.PIPE.get().asItem(), ItemModelUtils.plainModel(item));
        ModBlocks.COLORED_PIPES.forEach((color, pipe) -> {
            pipeBlockState(blockModels, pipe.get(), "_dyed");
            itemModels.itemModelOutput.accept(pipe.get().asItem(),
                    ItemModelUtils.tintedModel(dyedItem, ItemModelUtils.constantTint(color.getTextureDiffuseColor()),
                            ItemModelUtils.constantTint(PipeBlock.coreTint(color))));
        });
    }

    // One full set of pipe models using the given cable texture, returns the item model.
    private static Identifier pipeModels(BlockModelGenerators blockModels, String suffix, String cable) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, texture(cable))
                .put(CABLE, texture(cable))
                .put(CONNECTOR, texture("connector"));
        CORE.create(model("pipe_core" + suffix), textures, blockModels.modelOutput);
        ARM.create(model("pipe_arm" + suffix), textures, blockModels.modelOutput);
        for (Side side : new Side[]{Side.INSERT, Side.EXTRACT, Side.BOTH})
            PORT_ARM.create(model("pipe_arm_" + side.getSerializedName() + suffix),
                    textures.copyAndUpdate(PORT, texture("pipe_" + side.getSerializedName())), blockModels.modelOutput);
        return ITEM.create(model("pipe_item" + suffix), textures, blockModels.modelOutput);
    }

    private static void pipeBlockState(BlockModelGenerators blockModels, Block pipe, String suffix) {
        MultiPartGenerator gen = MultiPartGenerator.multiPart(pipe).with(plainVariant(model("pipe_core" + suffix)));
        for (Direction dir : Direction.values()) {
            var prop = PipeBlock.SIDES.get(dir);
            gen.with(condition().term(prop, Side.PIPE), plainVariant(model("pipe_arm" + suffix)).with(ROTATIONS.get(dir)));
            for (Side side : new Side[]{Side.INSERT, Side.EXTRACT, Side.BOTH})
                gen.with(condition().term(prop, side), plainVariant(model("pipe_arm_" + side.getSerializedName() + suffix)).with(ROTATIONS.get(dir)));
        }
        blockModels.blockStateOutput.accept(gen);
    }

    private static ExtendedModelTemplateBuilder template() {
        return ExtendedModelTemplateBuilder.builder().requiredTextureSlot(TextureSlot.PARTICLE).requiredTextureSlot(CABLE);
    }

    // Cable faces use tintindex 0. The core cube uses its own brighter square of the texture and tintindex 1 (lighter tint).
    private static Consumer<ElementBuilder> core() {
        return e -> e.from(6, 6, 6).to(10, 10, 10).allFaces((dir, f) -> f.texture(CABLE).uvs(12, 12, 16, 16).tintindex(1));
    }

    // 4px cable along z, the uvs pick the strip of cable.png with the core line running lengthwise.
    private static Consumer<ElementBuilder> arm(float z1, float z2) {
        float len = z2 - z1;
        return e -> e.from(6, 6, z1).to(10, 10, z2)
                .face(Direction.EAST, f -> f.texture(CABLE).uvs(0, 6, len, 10).tintindex(0))
                .face(Direction.WEST, f -> f.texture(CABLE).uvs(0, 6, len, 10).tintindex(0))
                .face(Direction.UP, f -> f.texture(CABLE).uvs(6, 0, 10, len).tintindex(0))
                .face(Direction.DOWN, f -> f.texture(CABLE).uvs(6, 0, 10, len).tintindex(0));
    }

    // Closes an open cable end with the same square as the core cube.
    private static Consumer<ElementBuilder> cap(Direction side) {
        return e -> e.face(side, f -> f.texture(CABLE).uvs(12, 12, 16, 16).tintindex(1));
    }

    // EnderIO style connector plate against the neighboring block.
    private static Consumer<ElementBuilder> plate() {
        return e -> e.from(3, 3, 0).to(13, 13, 2)
                .face(Direction.NORTH, f -> f.texture(CONNECTOR).uvs(3, 3, 13, 13).cullface(Direction.NORTH))
                .face(Direction.SOUTH, f -> f.texture(CONNECTOR).uvs(3, 3, 13, 13))
                .face(Direction.EAST, f -> f.texture(CONNECTOR).uvs(0, 0, 2, 10))
                .face(Direction.WEST, f -> f.texture(CONNECTOR).uvs(0, 0, 2, 10))
                .face(Direction.UP, f -> f.texture(CONNECTOR).uvs(0, 0, 10, 2))
                .face(Direction.DOWN, f -> f.texture(CONNECTOR).uvs(0, 0, 10, 2));
    }

    // 3px collar showing the side mode. Its texture has arrows pointing toward the block in the east/west
    // and up/down strips, the uvs are flipped per face so every arrow points along the arm the same way.
    private static Consumer<ElementBuilder> ring() {
        return e -> e.from(5, 5, 2).to(11, 11, 5)
                .face(Direction.SOUTH, f -> f.texture(PORT).uvs(5, 5, 11, 11))
                .face(Direction.EAST, f -> f.texture(PORT).uvs(0, 0, 3, 6))
                .face(Direction.WEST, f -> f.texture(PORT).uvs(3, 0, 0, 6))
                .face(Direction.UP, f -> f.texture(PORT).uvs(4, 3, 10, 0))
                .face(Direction.DOWN, f -> f.texture(PORT).uvs(4, 0, 10, 3));
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath(OmniPipes.MODID, "block/" + name);
    }

    private static Material texture(String name) {
        return new Material(model(name));
    }
}
