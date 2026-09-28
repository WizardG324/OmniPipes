package com.wizardg.omnipipes.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.wizardg.omnipipes.OmniPipes;
import com.wizardg.omnipipes.block.custom.PipeBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// One model for every pipe state, stitched from the part models (core faces, arms, connections)
// picked by PipeBlock.modelPart and cached per state.
public final class PipeModel {
    public static final ResourceLocation LOADER = ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "pipe");
    private static final String[] PARTS = {"core_box", "core_u", "core_v", "arm", "arm_insert", "arm_extract", "arm_both"};
    // Parts are built facing north, rotated per side: {x, y} degrees.
    private static final Map<Direction, int[]> ROTATIONS = Map.of(
            Direction.NORTH, new int[]{0, 0}, Direction.EAST, new int[]{0, 90}, Direction.SOUTH, new int[]{0, 180},
            Direction.WEST, new int[]{0, 270}, Direction.UP, new int[]{270, 0}, Direction.DOWN, new int[]{90, 0});

    public static final IGeometryLoader<Geometry> LOADER_INSTANCE =
            (JsonObject json, JsonDeserializationContext context) -> new Geometry(GsonHelper.getAsString(json, "suffix", ""));

    private PipeModel() {}

    // suffix picks the part set: "" for the base pipe, "_dyed" for the tinted colors.
    public record Geometry(String suffix) implements IUnbakedGeometry<Geometry> {
        private ResourceLocation part(String name) {
            return ResourceLocation.fromNamespaceAndPath(OmniPipes.MODID, "block/pipe_" + name + suffix);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
            for (String name : PARTS) modelGetter.apply(part(name)).resolveParents(modelGetter);
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                               ModelState modelState, ItemOverrides overrides) {
            Map<String, Map<Direction, BakedModel>> parts = new HashMap<>();
            for (String name : PARTS) {
                Map<Direction, BakedModel> bySide = new EnumMap<>(Direction.class);
                for (Direction dir : Direction.values()) {
                    int[] rot = ROTATIONS.get(dir);
                    bySide.put(dir, baker.bake(part(name), BlockModelRotation.by(rot[0], rot[1])));
                }
                parts.put(name, bySide);
            }
            return new Baked(parts);
        }
    }

    private static final class Baked implements IDynamicBakedModel {
        private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout()); // the arrows need it
        private final Map<String, Map<Direction, BakedModel>> parts;
        private final Map<BlockState, List<BakedModel>> byState = new ConcurrentHashMap<>(); // only states that show up

        Baked(Map<String, Map<Direction, BakedModel>> parts) {
            this.parts = parts;
        }

        private List<BakedModel> partsFor(BlockState state) {
            return byState.computeIfAbsent(state, s -> {
                List<BakedModel> list = new ArrayList<>(6);
                for (Direction dir : Direction.values()) list.add(parts.get(PipeBlock.modelPart(s, dir)).get(dir));
                return list;
            });
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            if (state == null || !(state.getBlock() instanceof PipeBlock)) return List.of();
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel part : partsFor(state)) quads.addAll(part.getQuads(state, side, rand, data, renderType));
            return quads;
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return CUTOUT;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return parts.get("core_box").get(Direction.NORTH).getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
