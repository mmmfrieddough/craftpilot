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
import mmmfrieddough.craftpilot.service.GhostBlockRenderService;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profilers;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix,
            Matrix4f projectionMatrix, CallbackInfo ci) {
        Profilers.get().push("craftpilot_update_targetted_block");

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
            IWorldManager manager = CraftPilot.getInstance().getWorldManager();
            GhostBlockService.updateCurrentTarget(manager, camera, reach, client.crosshairTarget);
        }

        Profilers.get().pop();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
            Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Profilers.get().push("craftpilot_render_blocks");

        IWorldManager manager = CraftPilot.getInstance().getWorldManager();
        Map<BlockPos, BlockState> ghostBlocks = manager.getGhostBlocks();

        // Early return if no blocks to render
        if (ghostBlocks.isEmpty()) {
            Profilers.get().pop();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig config = CraftPilot.getConfig();
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

        GhostBlockRenderService.renderGhostBlocks(client, ghostBlocks, camera, renderDistance,
                config.rendering.blockPlacementOpacity, matrices, immediate);

        GhostBlockRenderService.renderBlockOutlines(client, ghostBlocks, camera, renderDistance, config.rendering,
                matrices, immediate);

        matrices.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        Profilers.get().pop();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX,
            double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState, int i, CallbackInfo ci) {
        if (GhostBlockService.getCurrentTarget() != null) {
            ci.cancel();
        }
    }
}