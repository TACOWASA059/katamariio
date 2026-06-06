package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.client.KatamariPlayerRenderer;
import com.github.tacowasa059.katamariio.common.accessors.IPlayerRendererAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<Player, PlayerModel<Player>> implements IPlayerRendererAccessor {
    public PlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<Player> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Shadow
    protected abstract void renderNameTag(AbstractClientPlayer player, Component name, PoseStack matrixStack,
                                          MultiBufferSource buffer, int packedLight);

    @Override
    public void katamariIO$callRenderName(AbstractClientPlayer player, Component name, PoseStack matrixStack,
                                          MultiBufferSource buffer, int packedLight) {
        this.renderNameTag(player, name, matrixStack, buffer, packedLight);
    }

    @Override
    public boolean katamariIO$shouldShowName(Player player) {
        return this.shouldShowName(player);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void katamariIO$renderKatamari(AbstractClientPlayer player, float entityYaw, float partialTick, PoseStack poseStack,
                                           MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (KatamariPlayerRenderer.render((PlayerRenderer) (Object) this, player, partialTick, poseStack, buffer, packedLight)) {
            ci.cancel();
        }
    }
}
