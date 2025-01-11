package mmmfrieddough.craftpilot.service;

import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import mmmfrieddough.craftpilot.config.ModConfig.Rendering;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
            Rendering config,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate) {

        // Get currently targeted ghost block
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Vec3d lookVec = camera.getFocusedEntity().getRotationVec(1.0f);
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        BlockPos targetedBlock = GhostBlockService.findTargetedGhostBlock(
                ghostBlocks, camPos, lookVec, reach, client.crosshairTarget);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.lineWidth(2.0f);

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
                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    VertexRendering.drawOutline(
                            matrices,
                            normalVertices,
                            shape,
                            pos.getX() - cameraX,
                            pos.getY() - cameraY,
                            pos.getZ() - cameraZ,
                            normalColor);
                });
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
            VoxelShape shape = state.getOutlineShape(client.world, targetedBlock);
            int targetedColor = (alpha << 24) | config.targetedOutlineColor;
            shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                VertexRendering.drawOutline(
                        matrices,
                        targetedVertices,
                        shape,
                        targetedBlock.getX() - cameraX,
                        targetedBlock.getY() - cameraY,
                        targetedBlock.getZ() - cameraZ,
                        targetedColor);
            });

            immediate.draw();
        }

        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }
}