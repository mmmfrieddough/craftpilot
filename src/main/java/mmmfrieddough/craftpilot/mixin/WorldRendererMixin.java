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
import mmmfrieddough.craftpilot.schematic.SchematicManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
            Matrix4f matrix4f, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getProfiler().push("craftpilot_render");

        SchematicManager manager = CraftPilot.getInstance().getSchematicManager();
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

        // First pass: Ghost blocks
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
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

        // Second pass: Block outlines
        RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, config.rendering.blockOutlineOpacity);
        RenderSystem.lineWidth(2.0f);

        VertexConsumer lineVertices = immediate.getBuffer(RenderLayer.getLines());

        for (Map.Entry<BlockPos, BlockState> entry : ghostBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.isWithinDistance(cameraPos, renderDistance)) {
                VoxelShape shape = entry.getValue().getOutlineShape(client.world, pos);
                double offsetX = pos.getX() - cameraX;
                double offsetY = pos.getY() - cameraY;
                double offsetZ = pos.getZ() - cameraZ;

                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    WorldRenderer.drawBox(
                            matrices,
                            lineVertices,
                            minX + offsetX,
                            minY + offsetY,
                            minZ + offsetZ,
                            maxX + offsetX,
                            maxY + offsetY,
                            maxZ + offsetZ,
                            0.4f, 0.4f, 1.0f, 1.0f);
                });
            }
        }

        immediate.draw();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        client.getProfiler().pop();
    }
}