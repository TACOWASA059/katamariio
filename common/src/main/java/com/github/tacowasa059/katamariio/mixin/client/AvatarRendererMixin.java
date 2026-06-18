package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntityRenderer.class)
public abstract class AvatarRendererMixin {
    @Shadow
    protected abstract void setupRotations(LivingEntityRenderState renderState, PoseStack poseStack, float bodyRot, float scale);

    @Unique
    private static final Map<LivingEntityRenderState, RenderContext> katamariio$renderContexts = new WeakHashMap<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void katamariio$capturePlayer(LivingEntity entity, LivingEntityRenderState renderState, float partialTick, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player && renderState instanceof AvatarRenderState && ((ICustomPlayerData)player).katamariIO$getFlag()) {
            katamariio$renderContexts.put(renderState, new RenderContext(new WeakReference<>(player), partialTick));
        }
    }

    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V"
            )
    )
    private void katamariio$skipSetupRotations(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState renderState, PoseStack poseStack,
                                               float bodyRot, float scale) {
        if (!(renderState instanceof AvatarRenderState avatarRenderState) || !katamariio$renderContexts.containsKey(avatarRenderState)) {
            this.setupRotations(renderState, poseStack, bodyRot, scale);
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",
                    ordinal = 1
            ),
            cancellable = true
    )
    private void katamariio$renderKatamari(LivingEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                           CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!(renderState instanceof AvatarRenderState avatarRenderState)) {
            return;
        }

        RenderContext renderContext = katamariio$renderContexts.get(renderState);
        if (renderContext == null) {
            return;
        }

        AbstractClientPlayer player = renderContext.player().get();
        if (player == null) {
            katamariio$renderContexts.remove(renderState);
            return;
        }

        if (!KatamariPlayerRenderer.render(player, renderContext.partialTick(), poseStack, nodeCollector, renderState.lightCoords, renderState.outlineColor)) {
            katamariio$renderContexts.remove(renderState);
            return;
        }

        poseStack.popPose();
        katamariio$submitNameTags(avatarRenderState, poseStack, nodeCollector, cameraRenderState);
        katamariio$renderContexts.remove(renderState);
        ci.cancel();
    }

    @Unique
    private static void katamariio$submitNameTags(AvatarRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector,
                                                  CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        int verticalOffset = renderState.showExtraEars ? -10 : 0;
        if (renderState.scoreText != null) {
            nodeCollector.submitNameTag(
                    poseStack,
                    renderState.nameTagAttachment,
                    verticalOffset,
                    renderState.scoreText,
                    !renderState.isDiscrete,
                    renderState.lightCoords,
                    cameraRenderState
            );
            poseStack.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
        }

        if (renderState.nameTag != null) {
            nodeCollector.submitNameTag(
                    poseStack,
                    renderState.nameTagAttachment,
                    verticalOffset,
                    renderState.nameTag,
                    !renderState.isDiscrete,
                    renderState.lightCoords,
                    cameraRenderState
            );
        }

        poseStack.popPose();
    }

    @Unique
    private record RenderContext(WeakReference<AbstractClientPlayer> player, float partialTick) {
    }
}
