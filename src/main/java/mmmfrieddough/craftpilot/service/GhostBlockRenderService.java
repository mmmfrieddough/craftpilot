package mmmfrieddough.craftpilot.service;

import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public final class GhostBlockRenderService {
    private GhostBlockRenderService() {
    }

    /**
     * Renders ghost blocks with translucent effect
     */
    public static void renderGhostBlocks(
            MinecraftClient client,
            Map<BlockPos, BlockState> ghostBlocks,
            BlockPos cameraPos,
            double cameraX,
            double cameraY,
            double cameraZ,
            int renderDistance,
            float opacity,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate) {

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
        VertexConsumer translucentVertices = immediate.getBuffer(RenderLayer.getTranslucent());

        // Render ghost blocks
        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            // Only render blocks within view distance
            if (pos.isWithinDistance(cameraPos, renderDistance)) {
                matrices.push();
                matrices.translate(
                        pos.getX() - cameraX,
                        pos.getY() - cameraY,
                        pos.getZ() - cameraZ);

                client.getBlockRenderManager().renderBlock(
                        entry.getValue(),
                        pos,
                        client.world,
                        matrices,
                        translucentVertices,
                        false,
                        client.world.random);

                matrices.pop();
            }
        }

        immediate.draw();
    }

    /**
     * Renders outlines for ghost blocks
     */
    public static void renderBlockOutlines(
            MinecraftClient client,
            Map<BlockPos, BlockState> ghostBlocks,
            BlockPos cameraPos,
            double cameraX,
            double cameraY,
            double cameraZ,
            int renderDistance,
            float opacity,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate) {

        RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, opacity);
        RenderSystem.lineWidth(2.0f);

        VertexConsumer lineVertices = immediate.getBuffer(RenderLayer.getLines());

        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.isWithinDistance(cameraPos, renderDistance)) {
                VoxelShape shape = entry.getValue().getOutlineShape(client.world, pos);

                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    VertexRendering.drawOutline(
                            matrices,
                            lineVertices,
                            shape,
                            pos.getX() - cameraX,
                            pos.getY() - cameraY,
                            pos.getZ() - cameraZ,
                            0xFF66FFFF); // Cyan color in ARGB format
                });
            }
        }

        immediate.draw();
    }
}