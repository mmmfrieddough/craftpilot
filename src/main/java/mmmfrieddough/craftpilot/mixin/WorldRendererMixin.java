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
import mmmfrieddough.craftpilot.world.IWorldManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profilers;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator objectAllocator, RenderTickCounter renderTickCounter,
            boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
            Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
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

        GhostBlockRenderService.renderGhostBlocks(
                client, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ,
                renderDistance, config.rendering.blockPlacementOpacity,
                matrices, immediate);

        GhostBlockRenderService.renderBlockOutlines(
                client, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ,
                renderDistance, config.rendering.blockOutlineOpacity,
                matrices, immediate);

        matrices.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        Profilers.get().pop();
    }
}