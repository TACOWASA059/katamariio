package com.github.tacowasa059.katamariio.common.accessors;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface IPlayerRendererAccessor {
    void katamariIO$callRenderName(AbstractClientPlayer player, Component name,
                                   PoseStack matrixStack, MultiBufferSource buffer, int packedLight);
    boolean katamariIO$shouldShowName(Player player);
}

