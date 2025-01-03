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

        SchematicManager manager = CraftPilot.getInstance().getSchematicManager();
        ModConfig config = CraftPilot.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos cameraPos = camera.getBlockPos();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, config.rendering.blockPlacementOpacity);

        // Create a vertex consumer for our ghost blocks
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vertices = immediate.getBuffer(RenderLayer.getTranslucent());

        // Render ghost blocks
        for (Map.Entry<BlockPos, BlockState> entry : manager.getGhostBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            // Only render blocks within view distance
            if (pos.isWithinDistance(cameraPos, config.rendering.renderDistance)) {
                matrices.push();
                matrices.translate(
                        pos.getX() - camera.getPos().x,
                        pos.getY() - camera.getPos().y,
                        pos.getZ() - camera.getPos().z);

                client.getBlockRenderManager().renderBlock(
                        entry.getValue(),
                        pos,
                        client.world,
                        matrices,
                        vertices,
                        false,
                        client.world.random);

                matrices.pop();
            }
        }

        immediate.draw();

        // Second pass: Render block outlines
        RenderSystem.setShaderColor(0.0f, 1.0f, 1.0f, config.rendering.blockOutlineOpacity);
        RenderSystem.lineWidth(2.0f);

        for (Map.Entry<BlockPos, BlockState> entry : manager.getGhostBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.isWithinDistance(cameraPos, config.rendering.renderDistance)) {
                VoxelShape shape = entry.getValue().getOutlineShape(client.world, pos);
                shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    WorldRenderer.drawBox(
                            matrices,
                            immediate.getBuffer(RenderLayer.getLines()),
                            minX + pos.getX() - camera.getPos().x,
                            minY + pos.getY() - camera.getPos().y,
                            minZ + pos.getZ() - camera.getPos().z,
                            maxX + pos.getX() - camera.getPos().x,
                            maxY + pos.getY() - camera.getPos().y,
                            maxZ + pos.getZ() - camera.getPos().z,
                            0.4f, 0.4f, 1.0f, 1.0f);
                });
            }
        }

        immediate.draw();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}