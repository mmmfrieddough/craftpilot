package mmmfrieddough.craftpilot.service;

import java.util.Map;

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
     * Renders ghost blocks with translucent effect
     */
    public static void renderGhostBlocks(MinecraftClient client, Map<BlockPos, BlockState> ghostBlocks, Camera camera,
            int renderDistance, float opacity, MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        BlockPos cameraPos = camera.getBlockPos();
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        BlockRenderManager blockRenderManager = client.getBlockRenderManager();

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
                BlockStateModel blockStateModel = blockRenderManager.getModel(state);
                VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
                client.getBlockRenderManager().renderBlock(state, pos, client.world, matrices, vertexConsumer, false,
                        blockStateModel.getParts(client.world.random));
            }

            matrices.pop();
        }

        immediate.draw();

        // Reset shader color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
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