package mmmfrieddough.craftpilot.mixin;

import java.util.Map;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import mmmfrieddough.craftpilot.CraftPilot;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.shape.VoxelShape;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix,
            Matrix4f projectionMatrix, CallbackInfo ci) {
        Profilers.get().push("craftpilot_render");

        MinecraftClient client = MinecraftClient.getInstance();
        IWorldManager manager = CraftPilot.getInstance().getWorldManager();
        Map<BlockPos, BlockState> ghostBlocks = manager.getGhostBlocks();

        // Early return if no blocks to render
        if (ghostBlocks.isEmpty()) {
            return;
        }

        ModConfig config = CraftPilot.getConfig();
        BlockPos cameraPos = camera.getBlockPos();
        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;
        int renderDistance = config.rendering.renderDistance;

        // Create vertex consumer once
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Create a new MatrixStack for our transformations
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        // Apply the position matrix to align with world coordinates
        matrices.multiplyPositionMatrix(positionMatrix);

        renderGhostBlocks(client, config, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ, renderDistance, matrices,
                immediate);
        renderBlockOutlines(client, config, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ, renderDistance, matrices,
                immediate);

        matrices.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        Profilers.get().pop();
    }

    private void renderGhostBlocks(MinecraftClient client, ModConfig config, Map<BlockPos, BlockState> ghostBlocks,
            BlockPos cameraPos, double cameraX, double cameraY, double cameraZ, int renderDistance,
            MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, config.rendering.blockPlacementOpacity);

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

    private void renderBlockOutlines(MinecraftClient client, ModConfig config, Map<BlockPos, BlockState> ghostBlocks,
            BlockPos cameraPos, double cameraX, double cameraY, double cameraZ, int renderDistance,
            MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, config.rendering.blockOutlineOpacity);
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