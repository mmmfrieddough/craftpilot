package mmmfrieddough.craftpilot.service;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Wraps an OrderedRenderCommandQueue to scale the alpha component of
 * tinted colors, producing translucent block entity rendering.
 */
@Environment(EnvType.CLIENT)
public class AlphaRenderCommandQueue implements OrderedRenderCommandQueue {
    private final OrderedRenderCommandQueue delegate;
    private final float alphaMultiplier;

    public AlphaRenderCommandQueue(OrderedRenderCommandQueue delegate, float alphaMultiplier) {
        this.delegate = delegate;
        this.alphaMultiplier = alphaMultiplier;
    }

    private int scaleAlpha(int color) {
        int alpha = (color >> 24) & 0xFF;
        int scaledAlpha = (int) (alpha * alphaMultiplier);
        return (scaledAlpha << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public RenderCommandQueue getBatchingQueue(int order) {
        return delegate.getBatchingQueue(order);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer renderLayer,
            int light, int overlay, int tintedColor, @Nullable Sprite sprite, int outlineColor,
            @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        delegate.submitModel(model, state, matrices,
                GhostBlockRenderService.getTranslucentLayer(renderLayer), light, overlay,
                scaleAlpha(tintedColor), sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer renderLayer,
            int light, int overlay, @Nullable Sprite sprite, boolean sheeted, boolean hasGlint,
            int tintedColor, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
            int outlineColor) {
        delegate.submitModelPart(part, matrices,
                GhostBlockRenderService.getTranslucentLayer(renderLayer), light, overlay, sprite,
                sheeted, hasGlint, scaleAlpha(tintedColor), crumblingOverlay, outlineColor);
    }

    @Override
    public void submitShadowPieces(MatrixStack matrices, float shadowRadius,
            List<EntityRenderState.ShadowPiece> shadowPieces) {
        delegate.submitShadowPieces(matrices, shadowRadius, shadowPieces);
    }

    @Override
    public void submitLabel(MatrixStack matrices, @Nullable Vec3d nameLabelPos, int y, Text label,
            boolean notSneaking, int light, double squaredDistanceToCamera, CameraRenderState cameraState) {
        delegate.submitLabel(matrices, nameLabelPos, y, label, notSneaking, light,
                squaredDistanceToCamera, cameraState);
    }

    @Override
    public void submitText(MatrixStack matrices, float x, float y, OrderedText text,
            boolean dropShadow, TextRenderer.TextLayerType layerType, int light, int color,
            int backgroundColor, int outlineColor) {
        delegate.submitText(matrices, x, y, text, dropShadow, layerType, light,
                scaleAlpha(color), backgroundColor, outlineColor);
    }

    @Override
    public void submitFire(MatrixStack matrices, EntityRenderState renderState, Quaternionf rotation) {
        delegate.submitFire(matrices, renderState, rotation);
    }

    @Override
    public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
        delegate.submitLeash(matrices, leashData);
    }

    @Override
    public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
        delegate.submitBlock(matrices, state, light, overlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
        delegate.submitMovingBlock(matrices, state);
    }

    @Override
    public void submitBlockStateModel(MatrixStack matrices, RenderLayer renderLayer, BlockStateModel model,
            float r, float g, float b, int light, int overlay, int outlineColor) {
        delegate.submitBlockStateModel(matrices, renderLayer, model, r, g, b, light, overlay, outlineColor);
    }

    @Override
    public void submitItem(MatrixStack matrices, ItemDisplayContext displayContext, int light, int overlay,
            int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderLayer renderLayer,
            ItemRenderState.Glint glintType) {
        delegate.submitItem(matrices, displayContext, light, overlay, outlineColors, tintLayers, quads,
                renderLayer, glintType);
    }

    @Override
    public void submitCustom(MatrixStack matrices, RenderLayer renderLayer,
            OrderedRenderCommandQueue.Custom customRenderer) {
        delegate.submitCustom(matrices, renderLayer, customRenderer);
    }

    @Override
    public void submitCustom(OrderedRenderCommandQueue.LayeredCustom customRenderer) {
        delegate.submitCustom(customRenderer);
    }

}
