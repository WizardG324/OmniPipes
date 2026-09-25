package com.wizardg.omnipipes.data;

import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.ModBlocks;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import com.wizardg.omnipipes.block.custom.PipeBlock.Side;
import com.wizardg.omnipipes.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class ModModelProvider extends ModelProvider {
    private static final TextureSlot CABLE = TextureSlot.create("cable");
    private static final TextureSlot PORT = TextureSlot.create("port");
    private static final TextureSlot PORT_V = TextureSlot.create("port_v");
    private static final TextureSlot CONNECTOR = TextureSlot.create("connector");

    // Models are built facing north (tip at z=0), the blockstate rotates them per side.
    private static final Map<Direction, VariantMutator> ROTATIONS = Map.of(
            Direction.NORTH, NOP, Direction.EAST, Y_ROT_90, Direction.SOUTH, Y_ROT_180,
            Direction.WEST, Y_ROT_270, Direction.UP, X_ROT_270, Direction.DOWN, X_ROT_90);

    // One core face (built facing north, rotated like the arms): the box joint, or tube stripes running along u or v.
    private static final Map<String, ExtendedModelTemplate> CORE_FACES = Map.of(
            "box", coreFace(12, 12), "u", coreFace(0, 6), "v", coreFace(6, 0));
    private static final ExtendedModelTemplate ARM = template().element(arm(0, 6)).build();
    private static final ExtendedModelTemplate PORT_ARM = template().requiredTextureSlot(PORT).requiredTextureSlot(PORT_V).requiredTextureSlot(CONNECTOR)
            .element(plate(2.5f, 0, 11, 0)).element(plate(4.5f, 0.5f, 7, 2)).element(arm(1, 6)).element(arrows()).build();
    private static final ExtendedModelTemplate ITEM = template().parent(Identifier.withDefaultNamespace("block/block"))
            .element(arm(0, 6).andThen(cap(Direction.NORTH))).element(arm(6, 10)).element(arm(10, 16).andThen(cap(Direction.SOUTH))).build();

    public ModModelProvider(PackOutput output) {
        super(output, OmniPipes.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Identifier item = pipeModels(blockModels, "", "cable");
        Identifier dyedItem = pipeModels(blockModels, "_dyed", "cable_dyed");

        ModItems.TIER_UPGRADES.forEach(tier -> itemModels.generateFlatItem(tier.get(), ModelTemplates.FLAT_ITEM));
        for (var upgrade : List.of(ModItems.CREATIVE_UPGRADE, ModItems.FLUID_UPGRADE, ModItems.ENERGY_UPGRADE, ModItems.TAG_FILTER))
            itemModels.generateFlatItem(upgrade.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(ModItems.PIPE_CONFIGURATOR.get(), ModelTemplates.FLAT_HANDHELD_ITEM); // held like a tool

        pipeBlockState(blockModels, ModBlocks.PIPE.get(), "");
        itemModels.itemModelOutput.accept(ModBlocks.PIPE.get().asItem(), ItemModelUtils.plainModel(item));
        ModBlocks.COLORED_PIPES.forEach((color, pipe) -> {
            pipeBlockState(blockModels, pipe.get(), "_dyed");
            itemModels.itemModelOutput.accept(pipe.get().asItem(),
                    ItemModelUtils.tintedModel(dyedItem, ItemModelUtils.constantTint(color.getTextureDiffuseColor())));
        });
    }

    // One full set of pipe models using the given cable texture, returns the item model.
    private static Identifier pipeModels(BlockModelGenerators blockModels, String suffix, String cable) {
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, texture(cable))
                .put(CABLE, texture(cable))
                .put(CONNECTOR, texture("connector"));
        CORE_FACES.forEach((name, template) -> template.create(model("pipe_core_" + name + suffix), textures, blockModels.modelOutput));
        ARM.create(model("pipe_arm" + suffix), textures, blockModels.modelOutput);
        for (Side side : new Side[]{Side.INSERT, Side.EXTRACT, Side.BOTH})
            PORT_ARM.create(model("pipe_arm_" + side.getSerializedName() + suffix),
                    textures.copyAndUpdate(PORT, texture("pipe_" + side.getSerializedName()))
                            .copyAndUpdate(PORT_V, texture("pipe_" + side.getSerializedName() + "_v")), blockModels.modelOutput);
        return ITEM.create(model("pipe_item" + suffix), textures, blockModels.modelOutput);
    }

    private static void pipeBlockState(BlockModelGenerators blockModels, Block pipe, String suffix) {
        MultiPartGenerator gen = MultiPartGenerator.multiPart(pipe);
        for (Direction dir : Direction.values()) {
            coreFace(gen, dir, suffix);
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

    private static ExtendedModelTemplate coreFace(float u, float v) {
        return template().element(e -> e.from(6, 6, 6).to(10, 10, 10)
                .face(Direction.NORTH, f -> f.texture(CABLE).uvs(u, v, u + 4, v + 4).tintindex(0))).build();
    }

    // A core face only shows on a side without an arm. On a straight run of plain pipe (both arms along one axis
    // are PIPE, every other side NONE) it continues the tube, anything else (lone pipe, dead end, connection,
    // corner, junction) gets the box joint. Rotating the north face keeps its U on X (Z for east/west) and V on Y
    // (z for up/down), which picks the stripe direction.
    private static void coreFace(MultiPartGenerator gen, Direction face, String suffix) {
        Direction.Axis uAxis = face.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis vAxis = face.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        Map<Direction, Set<Side>> visible = Map.of(face, Set.of(Side.NONE));
        List<ConditionBuilder> box = new ArrayList<>();
        for (var a : along(uAxis, false))
            for (var b : along(vAxis, false)) {
                var terms = merge(visible, merge(a, b));
                if (terms != null) box.add(when(terms));
            }
        gen.with(or(box.toArray(ConditionBuilder[]::new)), plainVariant(model("pipe_core_box" + suffix)).with(ROTATIONS.get(face)));
        gen.with(when(merge(visible, along(uAxis, true).getFirst())), plainVariant(model("pipe_core_u" + suffix)).with(ROTATIONS.get(face)));
        gen.with(when(merge(visible, along(vAxis, true).getFirst())), plainVariant(model("pipe_core_v" + suffix)).with(ROTATIONS.get(face)));
    }

    // Straight along axis: both its sides PIPE and all four other sides NONE. With straight = false this is the
    // negation, one single-side term per way it can fail.
    private static List<Map<Direction, Set<Side>>> along(Direction.Axis axis, boolean straight) {
        Set<Side> notPipe = EnumSet.complementOf(EnumSet.of(Side.PIPE)), notNone = EnumSet.complementOf(EnumSet.of(Side.NONE));
        Map<Direction, Set<Side>> all = new EnumMap<>(Direction.class);
        List<Map<Direction, Set<Side>>> fails = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            boolean onAxis = dir.getAxis() == axis;
            all.put(dir, Set.of(onAxis ? Side.PIPE : Side.NONE));
            fails.add(Map.of(dir, onAxis ? notPipe : notNone));
        }
        return straight ? List.of(all) : fails;
    }

    // Intersects two sets of terms, null if some side can't satisfy both.
    private static Map<Direction, Set<Side>> merge(Map<Direction, Set<Side>> a, Map<Direction, Set<Side>> b) {
        if (a == null || b == null) return null;
        Map<Direction, Set<Side>> out = new EnumMap<>(Direction.class);
        out.putAll(a);
        for (var entry : b.entrySet()) {
            Set<Side> both = EnumSet.copyOf(entry.getValue());
            if (out.containsKey(entry.getKey())) both.retainAll(out.get(entry.getKey()));
            if (both.isEmpty()) return null;
            out.put(entry.getKey(), both);
        }
        return out;
    }

    private static ConditionBuilder when(Map<Direction, Set<Side>> terms) {
        ConditionBuilder builder = BlockModelGenerators.condition();
        terms.forEach((dir, sides) -> {
            Side[] values = sides.toArray(Side[]::new);
            builder.term(PipeBlock.SIDES.get(dir), values[0], Arrays.copyOfRange(values, 1, values.length));
        });
        return builder;
    }

    // 4px conduit along z (tintindex 0), the uvs pick the strip of cable.png running lengthwise.
    private static Consumer<ElementBuilder> arm(float z1, float z2) {
        float len = z2 - z1;
        return e -> e.from(6, 6, z1).to(10, 10, z2)
                .face(Direction.EAST, f -> f.texture(CABLE).uvs(0, 6, len, 10).tintindex(0))
                .face(Direction.WEST, f -> f.texture(CABLE).uvs(0, 6, len, 10).tintindex(0))
                .face(Direction.UP, f -> f.texture(CABLE).uvs(6, 0, 10, len).tintindex(0))
                .face(Direction.DOWN, f -> f.texture(CABLE).uvs(6, 0, 10, len).tintindex(0));
    }

    // Closes an open conduit end on the item model.
    private static Consumer<ElementBuilder> cap(Direction side) {
        return e -> e.face(side, f -> f.texture(CABLE).uvs(12, 12, 16, 16).tintindex(0));
    }

    // EnderIO style thin plate step on the neighboring block, size x size and 0.5px deep starting at depth z.
    // The texture's frame covers pixels 0-10, a smaller plate reads from inset so it gets plain steel.
    private static Consumer<ElementBuilder> plate(float from, float z, float size, float inset) {
        float to = from + size;
        return e -> e.from(from, from, z).to(to, to, z + 0.5f)
                .face(Direction.NORTH, f -> f.texture(CONNECTOR).uvs(inset, inset, inset + size, inset + size).cullface(Direction.NORTH))
                .face(Direction.SOUTH, f -> f.texture(CONNECTOR).uvs(inset, inset, inset + size, inset + size))
                .face(Direction.EAST, f -> f.texture(CONNECTOR).uvs(0, 0, 0.5f, size))
                .face(Direction.WEST, f -> f.texture(CONNECTOR).uvs(0, 0, 0.5f, size))
                .face(Direction.UP, f -> f.texture(CONNECTOR).uvs(0, 0, size, 0.5f))
                .face(Direction.DOWN, f -> f.texture(CONNECTOR).uvs(0, 0, size, 0.5f));
    }

    // EnderIO style mode arrows on a see-through shell just outside the cable. The textures point toward the block's
    // port for east/west (flipped on west), port_v for up/down (flipped on down).
    // Arrows could potentially use a do-over, not fully settled on them
    private static Consumer<ElementBuilder> arrows() {
        return e -> e.from(5.8f, 5.8f, 1).to(10.2f, 10.2f, 6)
                .face(Direction.EAST, f -> f.texture(PORT).uvs(0, 0, 16, 16))
                .face(Direction.WEST, f -> f.texture(PORT).uvs(16, 0, 0, 16))
                .face(Direction.UP, f -> f.texture(PORT_V).uvs(0, 0, 16, 16))
                .face(Direction.DOWN, f -> f.texture(PORT_V).uvs(0, 16, 16, 0));
    }

    private static Identifier model(String name) {
        return Identifier.fromNamespaceAndPath(OmniPipes.MODID, "block/" + name);
    }

    private static Material texture(String name) {
        return new Material(model(name));
    }
}
