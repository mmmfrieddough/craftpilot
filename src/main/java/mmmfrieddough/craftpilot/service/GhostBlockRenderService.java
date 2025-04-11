package mmmfrieddough.craftpilot.service;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import mmmfrieddough.craftpilot.config.ModConfig.Rendering;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public final class GhostBlockRenderService {
    // Prevent instantiation
    private GhostBlockRenderService() {
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

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        RenderLayer renderLayer = RenderLayer.getTranslucent();

        // Render ghost blocks
        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            // Only render blocks within view distance
            if (!pos.isWithinDistance(cameraPos, renderDistance)) {
                continue;
            }

            BlockState state = entry.getValue();
            Block block = state.getBlock();

            matrices.push();
            matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

            if (block instanceof BlockEntityProvider provider) {
                BlockEntity blockEntity = provider.createBlockEntity(pos, state);
                blockEntity.setWorld(client.world);
                client.getBlockEntityRenderDispatcher().render(blockEntity, 0f, matrices, immediate);
            } else {
                VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
                client.getBlockRenderManager().renderBlock(state, pos, client.world, matrices, vertexConsumer, false,
                        client.world.random);
            }

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

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);

        // First pass: render non-targeted blocks
        RenderSystem.polygonOffset(-2.0f, -8.0f);
        RenderSystem.enablePolygonOffset();

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
            // Get fresh buffer for second pass
            VertexConsumer targetedVertices = immediate.getBuffer(RenderLayer.getLines());

            // Use less offset to render on top
            RenderSystem.polygonOffset(-3.0f, -10.0f);

            BlockState state = ghostBlocks.get(targetedBlock);
            int targetedColor = (alpha << 24) | config.targetedOutlineColor;
            VoxelShape shape = state.getOutlineShape(client.world, targetedBlock);
            VertexRendering.drawOutline(matrices, targetedVertices, shape, targetedBlock.getX() - cameraX,
                    targetedBlock.getY() - cameraY, targetedBlock.getZ() - cameraZ, targetedColor);

            immediate.draw();
        }

        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }
}