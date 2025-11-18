package mmmfrieddough.craftpilot.service;

import java.util.Map;

import mmmfrieddough.craftpilot.config.ModConfig.Rendering;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public final class GhostBlockRenderService {
    // Prevent instantiation
    private GhostBlockRenderService() {
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

            // TODO: Fix block entity rendering with new API
            // The BlockEntityRenderManager API changed significantly in 1.21:
            // - Now requires creating BlockEntityRenderState first via getRenderState()
            // - Then calling render() with OrderedRenderCommandQueue and CameraRenderState
            // - Need to investigate proper way to render detached block entities for ghost
            // blocks
            // Old code (doesn't compile):
            // if (block instanceof BlockEntityProvider provider) {
            // BlockEntity blockEntity = provider.createBlockEntity(pos, state);
            // blockEntity.setWorld(client.world);
            // client.getBlockEntityRenderDispatcher().render(blockEntity, matrices,
            // immediate);
            // }

            BlockStateModel blockStateModel = blockRenderManager.getModel(state);
            VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
            // Wrap the vertex consumer to apply our custom opacity
            VertexConsumer alphaVertexConsumer = new AlphaVertexConsumer(vertexConsumer, opacity);
            client.getBlockRenderManager().renderBlock(state, pos, client.world, matrices, alphaVertexConsumer,
                    false,
                    blockStateModel.getParts(client.world.random));

            matrices.pop();
        }

        immediate.draw();
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