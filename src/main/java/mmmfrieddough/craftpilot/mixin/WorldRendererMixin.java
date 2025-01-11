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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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

        GhostBlockRenderService.renderGhostBlocks(client, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ,
                renderDistance, config.rendering.blockPlacementOpacity, matrices, immediate);

        GhostBlockRenderService.renderBlockOutlines(client, ghostBlocks, cameraPos, cameraX, cameraY, cameraZ,
                renderDistance, config.rendering, matrices, immediate);

        matrices.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        Profilers.get().pop();
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX,
            double cameraY, double cameraZ, BlockPos blockPos, BlockState blockState, int i, CallbackInfo ci) {
        // Get ghost blocks
        IWorldManager manager = CraftPilot.getInstance().getWorldManager();
        Map<BlockPos, BlockState> ghostBlocks = manager.getGhostBlocks();

        // Early return if no blocks to check
        if (ghostBlocks.isEmpty()) {
            return;
        }

        // Check if there's a ghost block in front of the target
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        Vec3d lookVec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        double reach = client.player.getAttributeValue(EntityAttributes.BLOCK_INTERACTION_RANGE);
        HitResult vanillaTarget = client.crosshairTarget;

        BlockPos ghostTarget = GhostBlockService.findTargetedGhostBlock(ghostBlocks, cameraPos, lookVec, reach,
                vanillaTarget);
        if (ghostTarget != null) {
            // If there's a ghost block being targeted, cancel the vanilla outline
            ci.cancel();
        }
    }
}