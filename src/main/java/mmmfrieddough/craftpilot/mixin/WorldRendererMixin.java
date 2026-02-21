package mmmfrieddough.craftpilot.mixin;

import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

import mmmfrieddough.craftpilot.CraftPilotClient;
import mmmfrieddough.craftpilot.config.ModConfig;
import mmmfrieddough.craftpilot.service.CraftPilotService;
import mmmfrieddough.craftpilot.service.GhostBlockRenderService;
import mmmfrieddough.craftpilot.service.GhostBlockService;
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profilers;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow
    private MinecraftClient client;

    private ModConfig config;
    private IWorldManager worldManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        config = CraftPilotClient.getInstance().getConfig();
        worldManager = CraftPilotClient.getInstance().getWorldManager();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean renderBlockOutline, Camera camera, Matrix4f matrix1, Matrix4f matrix2, Matrix4f matrix3,
            GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bool2, CallbackInfo ci) {
        Profilers.get().push("craftpilot_update_targetted_block");

        if (client.player != null) {
            Entity cameraEntity = client.getCameraEntity();
            double blockInteractionRange = config.general.enableInfiniteReach ? 10000.0D
                    : client.player.getBlockInteractionRange();
            double enttityInteractionRange = client.player.getEntityInteractionRange();
            HitResult vanillaTarget = CraftPilotService.findCrosshairTarget(cameraEntity, blockInteractionRange,
                    enttityInteractionRange, 1.0F);
            GhostBlockService.updateCurrentTarget(client.world, worldManager, camera, blockInteractionRange,
                    vanillaTarget);
        }

        Profilers.get().pop();
    }

    @Inject(method = "renderTargetBlockOutline", at = @At("RETURN"))
    private void onRenderAfterOutline(VertexConsumerProvider.Immediate immediate, MatrixStack matrices,
            boolean renderBlockOutline, net.minecraft.client.render.state.WorldRenderState renderStates,
            CallbackInfo ci) {
        // Inject after block outline rendering (line 656) but before final
        // immediate.draw() (line 659)
        // This ensures ghost blocks render after translucent blocks for proper depth
        // sorting
        Profilers.get().push("craftpilot_ghost_blocks");

        Map<BlockPos, BlockState> ghostBlocks = worldManager.getGhostBlocks();

        // Early return if no blocks to render
        if (ghostBlocks.isEmpty()) {
            Profilers.get().pop();
            return;
        }

        int renderDistance = config.rendering.renderDistance;
        Camera camera = client.gameRenderer.getCamera();

        GhostBlockRenderService.renderGhostBlocks(client, ghostBlocks, camera, renderDistance,
                config.rendering.blockPlacementOpacity, matrices, immediate);

        GhostBlockRenderService.renderBlockOutlines(client, ghostBlocks, camera, renderDistance,
                config.rendering,
                matrices, immediate);

        Profilers.get().pop();
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"))
    private void onRenderBlockEntities(MatrixStack matrices, net.minecraft.client.render.state.WorldRenderState renderStates,
            net.minecraft.client.render.command.OrderedRenderCommandQueueImpl queue, CallbackInfo ci) {
        Map<BlockPos, BlockState> ghostBlocks = worldManager.getGhostBlocks();
        if (ghostBlocks.isEmpty()) {
            return;
        }

        int renderDistance = config.rendering.renderDistance;
        Camera camera = client.gameRenderer.getCamera();
        GhostBlockRenderService.renderGhostBlockEntities(client, ghostBlocks, camera, renderDistance,
                renderStates.blockEntityRenderStates);
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, double cameraX,
            double cameraY, double cameraZ, OutlineRenderState outlineRenderState, int i, CallbackInfo ci) {
        if (GhostBlockService.getCurrentTarget() != null) {
            ci.cancel();
        }
    }
}