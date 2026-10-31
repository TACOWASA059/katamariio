package com.github.tacowasa059.katamariio.mixin.client;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.entities.KatamariEntityAttacher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Shadow
    public abstract <S extends EntityRenderState> EntityRenderer<?, ? super S> getRenderer(S renderState);

    @Unique
    private static final Map<EntityRenderState, AttachedContext> katamariIO$attachedContexts = new WeakHashMap<>();

    @Inject(
            method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private <E extends Entity> void katamariIO$onExtractEntity(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        Player player = null;
        if (entity.getVehicle() instanceof Player p) {
            player = p;
        } else if (net.minecraft.client.Minecraft.getInstance().level != null) {
            Player localPlayer = net.minecraft.client.Minecraft.getInstance().player;
            if (localPlayer != null && ((ICustomPlayerData) localPlayer).katamariIO$getPassengerLocalPos(entity.getId()) != null) {
                player = localPlayer;
            }
        }
        if (player != null && ((ICustomPlayerData) player).katamariIO$getFlag()) {
            EntityRenderState state = cir.getReturnValue();
            if (state != null) {
                katamariIO$attachedContexts.put(state, new AttachedContext(new WeakReference<>(entity), new WeakReference<>(player), partialTick));
            }
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <S extends EntityRenderState> void katamariIO$onSubmit(
            S renderState, CameraRenderState cameraRenderState, double x, double y, double z,
            PoseStack poseStack, SubmitNodeCollector nodeCollector, CallbackInfo ci) {
        AttachedContext ctx = katamariIO$attachedContexts.remove(renderState);
        if (ctx == null) {
            return;
        }
        Entity entity = ctx.entity().get();
        Player player = ctx.player().get();
        if (entity == null || player == null) {
            return;
        }
        Vector3f localPos = KatamariEntityAttacher.getLocalPos(player, entity);
        if (localPos == null) {
            return;
        }

        float partialTick = ctx.partialTick();
        ICustomPlayerData data = (ICustomPlayerData) player;
        float size = data.katamariIO$getCollisionSize();
        Quaternionf ballRot = data.katamariIO$getInterpolatedQuaternion(partialTick);

        double ballX = Mth.lerp(partialTick, player.xo, player.getX());
        double ballY = Mth.lerp(partialTick, player.yo, player.getY());
        double ballZ = Mth.lerp(partialTick, player.zo, player.getZ());
        Vec3 ballCenter = new Vec3(ballX, ballY + size * 0.5, ballZ);

        Vector3f rotated = new Vector3f(localPos).rotate(ballRot);
        Vec3 mobWorldPos = new Vec3(
                ballCenter.x + rotated.x,
                ballCenter.y + rotated.y,
                ballCenter.z + rotated.z
        );

        Vec3 camPos = cameraRenderState.pos != null ? cameraRenderState.pos : Vec3.ZERO;
        Vec3 camRel = mobWorldPos.subtract(camPos);

        EntityRenderer<?, ? super S> renderer = this.getRenderer(renderState);
        if (renderer == null) {
            return;
        }

        Vec3 renderOffset = renderer.getRenderOffset(renderState);
        double renderX = camRel.x + renderOffset.x();
        double renderY = camRel.y + renderOffset.y();
        double renderZ = camRel.z + renderOffset.z();

        poseStack.pushPose();
        poseStack.translate(renderX, renderY, renderZ);

        Quaternionf attachQuat = KatamariEntityAttacher.getAttachQuat(player, entity);
        if (attachQuat != null) {
            Quaternionf delta = new Quaternionf(ballRot).mul(new Quaternionf(attachQuat).invert());
            poseStack.mulPose(delta);
        } else {
            poseStack.mulPose(ballRot);
        }

        renderer.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.popPose();

        ci.cancel();
    }

    @Unique
    private record AttachedContext(WeakReference<Entity> entity, WeakReference<Player> player, float partialTick) {
    }
}
