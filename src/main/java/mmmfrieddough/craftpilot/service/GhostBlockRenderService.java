package mmmfrieddough.craftpilot.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import mmmfrieddough.craftpilot.config.ModConfig.Rendering;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public final class GhostBlockRenderService {
    private static Set<BlockPos> activeGhostPositions = Collections.emptySet();
    private static float activeOpacity = 1.0f;
    private static final Map<RenderLayer, RenderLayer> TRANSLUCENT_LAYER_MAP = new ConcurrentHashMap<>();

    private GhostBlockRenderService() {
    }

    public static void setActiveGhostContext(Set<BlockPos> positions, float opacity) {
        activeGhostPositions = positions;
        activeOpacity = opacity;
    }

    public static void clearActiveGhostContext() {
        activeGhostPositions = Collections.emptySet();
        activeOpacity = 1.0f;
    }

    public static boolean isGhostBlockEntity(BlockPos pos) {
        return activeGhostPositions.contains(pos);
    }

    public static float getActiveOpacity() {
        return activeOpacity;
    }

    public static void registerTranslucentLayer(RenderLayer opaqueLayer, RenderLayer translucentLayer) {
        TRANSLUCENT_LAYER_MAP.putIfAbsent(opaqueLayer, translucentLayer);
    }

    public static RenderLayer getTranslucentLayer(RenderLayer layer) {
        return TRANSLUCENT_LAYER_MAP.getOrDefault(layer, layer);
    }

    /**
     * Wraps a VertexConsumer to multiply alpha values
     */
    private record AlphaVertexConsumer(VertexConsumer delegate, float alphaMultiplier) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return delegate.color(red, green, blue, (int) (alpha * alphaMultiplier));
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return delegate.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return delegate.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return delegate.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return delegate.normal(x, y, z);
        }
    }

    /**
     * Renders ghost blocks with translucent effect
     */
    public static void renderGhostBlocks(MinecraftClient client, Map<BlockPos, BlockState> ghostBlocks, Camera camera,
            int renderDistance, float opacity, MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        BlockPos cameraPos = camera.getBlockPos();
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        BlockRenderManager blockRenderManager = client.getBlockRenderManager();
        RenderLayer renderLayer = RenderLayer.getTranslucentMovingBlock();
        // Render ghost blocks
        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            // Only render blocks within view distance
            if (!pos.isWithinDistance(cameraPos, renderDistance)) {
                continue;
            }

            BlockState state = entry.getValue();

            matrices.push();
            matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

            if (state.getRenderType() == BlockRenderType.MODEL) {
                BlockStateModel blockStateModel = blockRenderManager.getModel(state);
                VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
                // Wrap the vertex consumer to apply our custom opacity
                VertexConsumer alphaVertexConsumer = new AlphaVertexConsumer(vertexConsumer, opacity);
                blockRenderManager.renderBlock(state, pos, client.world, matrices, alphaVertexConsumer, false,
                        blockStateModel.getParts(client.world.random));
            }

            matrices.pop();
        }

        immediate.draw();
    }

    /**
     * Adds ghost block entities to the world render state list and tracks their
     * positions for alpha wrapping by BlockEntityRenderManagerMixin.
     */
    public static void renderGhostBlockEntities(MinecraftClient client, Map<BlockPos, BlockState> ghostBlocks,
            Camera camera, int renderDistance, float opacity,
            List<BlockEntityRenderState> blockEntityRenderStates) {
        BlockPos cameraPos = camera.getBlockPos();
        BlockEntityRenderManager blockEntityRenderManager = client.getBlockEntityRenderDispatcher();
        Set<BlockPos> ghostPositions = new HashSet<>();

        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!pos.isWithinDistance(cameraPos, renderDistance)) {
                continue;
            }

            BlockState state = entry.getValue();
            if (!(state.getBlock() instanceof BlockEntityProvider provider)) {
                continue;
            }

            BlockEntity blockEntity = provider.createBlockEntity(pos, state);
            if (blockEntity == null) {
                continue;
            }
            blockEntity.setWorld(client.world);

            BlockEntityRenderState renderState = blockEntityRenderManager.getRenderState(blockEntity, 0.0f, null);
            if (renderState != null) {
                blockEntityRenderStates.add(renderState);
                ghostPositions.add(pos);
            }
        }

        setActiveGhostContext(ghostPositions, opacity);
    }

    /**
     * Renders outlines for ghost blocks
     */
    public static void renderBlockOutlines(MinecraftClient client, Map<BlockPos, BlockState> ghostBlocks, Camera camera,
            int renderDistance, Rendering config, MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        BlockPos cameraPos = camera.getBlockPos();
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        BlockPos targetedBlock = GhostBlockService.getCurrentTargetPos();

        VertexConsumer normalVertices = immediate.getBuffer(RenderLayer.getLines());

        int alpha = (int) (config.blockOutlineOpacity * 255.0f);
        int normalColor = (alpha << 24) | config.normalOutlineColor;

        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!pos.equals(targetedBlock) && pos.isWithinDistance(cameraPos, renderDistance)) {
                VoxelShape shape = entry.getValue().getOutlineShape(client.world, pos);
                VertexRendering.drawOutline(matrices, normalVertices, shape, pos.getX() - cameraX, pos.getY() - cameraY,
                        pos.getZ() - cameraZ, normalColor);
            }
        }

        immediate.draw();

        // Second pass: render targeted block
        if (targetedBlock != null && targetedBlock.isWithinDistance(cameraPos, renderDistance)) {
            VertexConsumer targetedVertices = immediate.getBuffer(RenderLayer.getLines());

            BlockState state = ghostBlocks.get(targetedBlock);
            int targetedColor = (alpha << 24) | config.targetedOutlineColor;
            VoxelShape shape = state.getOutlineShape(client.world, targetedBlock);
            VertexRendering.drawOutline(matrices, targetedVertices, shape, targetedBlock.getX() - cameraX,
                    targetedBlock.getY() - cameraY, targetedBlock.getZ() - cameraZ, targetedColor);

            immediate.draw();
        }
    }
}