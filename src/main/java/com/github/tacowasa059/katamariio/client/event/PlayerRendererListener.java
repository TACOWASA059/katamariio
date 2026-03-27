package com.github.tacowasa059.katamariio.client.event;

import com.github.tacowasa059.katamariio.KatamariIO;
import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import com.github.tacowasa059.katamariio.common.accessors.IPlayerRendererAccessor;
import com.github.tacowasa059.katamariio.client.utils.SphereRenderer;
import com.github.tacowasa059.katamariio.common.utils.QuaternionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@Mod.EventBusSubscriber(modid = KatamariIO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PlayerRendererListener {

    public static double MaxSize = 0;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void render(RenderLivingEvent.Pre<Player, PlayerModel<Player>> event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player1) {
            AbstractClientPlayer player = (AbstractClientPlayer) entity;
            ICustomPlayerData playerData =(ICustomPlayerData)player1;

            boolean flag = playerData.katamariIO$getFlag();
            if(!flag) return;
            if (entity.isInvisible()){
                event.setCanceled(true);
                return;
            }
            float renderSize = playerData.katamariIO$getRenderSize();
            float collisionSize = playerData.katamariIO$getSize();

            float partialTicks = event.getPartialTick();

            Quaternionf quaternion = playerData.katamariIO$getInterpolatedQuaternion(partialTicks);

            ResourceLocation texture = player.getSkinTextureLocation();
            PoseStack poseStack  = event.getPoseStack();

            MultiBufferSource buffer = event.getMultiBufferSource();
            int packedLight = event.getPackedLight();
            poseStack.pushPose();
            // Rotation/orbit pivot should match physics center, not visual-only size.
            poseStack.translate(0, collisionSize / 2.0, 0);
            if(player.getVehicle()==null) poseStack.mulPose(quaternion);
            else poseStack.mulPose(QuaternionUtils.getQuaternionFromEntity(entity));

            int overlay = OverlayTexture.NO_OVERLAY;
            if(player1.hurtTime > 0 || player1.deathTime > 0)overlay = OverlayTexture.pack(OverlayTexture.u(event.getPartialTick()),
                    OverlayTexture.v(player1.hurtTime > 0 || player1.deathTime > 0));

            poseStack.pushPose();
            SphereRenderer.drawTexturedSphere(poseStack, buffer, texture, renderSize / 2.0f, 12, 0, 0, packedLight,true, overlay);
            poseStack.popPose();

            List<Vec3> vec3List = playerData.katamariIO$getSphericalPlayerPositions();
            List<Block> blockList = playerData.katamariIO$getSphericalPlayerBlocks();
            List<Quaternionf> quaternionfList = playerData.katamariIO$getSphericalPlayerQuaternions();
            int lodStep = katamariIO$getLodStepForPlayer(player);
            Entity camera = Minecraft.getInstance().getCameraEntity();
            Entity cameraEntity = camera == null ? player : camera;
            CameraType cameraType = Minecraft.getInstance().options.getCameraType();
            Vec3 viewDir = cameraEntity.getViewVector(partialTicks);
            Vector3f viewDirLocal = new Vector3f((float) viewDir.x, (float) viewDir.y, (float) viewDir.z);
            Quaternionf inverseBallRotation = new Quaternionf(quaternion).invert();
            viewDirLocal.rotate(inverseBallRotation);

            double tmp_Size = playerData.katamariIO$getRenderSize();

            for(int i=0; i<vec3List.size(); i++){
                Vec3 vec = vec3List.get(i);
                if (!katamariIO$shouldRenderByHash(vec, i, player.getId(), lodStep)) {
                    continue;
                }
                Vector3f localNormal = new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
                float normalLenSq = localNormal.lengthSquared();
                if (normalLenSq > 1.0E-6f) {
                    localNormal.normalize();
                    float dot = localNormal.dot(viewDirLocal);
                    if (cameraType == CameraType.THIRD_PERSON_BACK ? dot >= 0.0f : dot <= 0.0f) {
                        continue;
                    }
                }
                Block block = blockList.get(i);
                Quaternionf quaternionf = quaternionfList.get(i);

                poseStack.pushPose();
                poseStack.translate(vec.x, vec.y, vec.z);
                poseStack.mulPose(quaternionf);

                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),poseStack, buffer, packedLight, overlay, ModelData.EMPTY, null);

                poseStack.popPose();

                if(player.equals(Minecraft.getInstance().player)){
                    tmp_Size = Math.max(tmp_Size, vec.length()*2);
                }
            }

            if(player.equals(Minecraft.getInstance().player)){
                MaxSize = tmp_Size;
            }

            poseStack.popPose();

            EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);

            if (player instanceof RemotePlayer && renderer instanceof IPlayerRendererAccessor playerRendererAccessor) {
                if(playerRendererAccessor.katamariIO$shouldShowName(player))
                    playerRendererAccessor.katamariIO$callRenderName(player, player.getDisplayName(), poseStack, buffer, packedLight);
            }
            event.setCanceled(true);
        }
    }

    private static int katamariIO$getLodStepForPlayer(Player player) {
        double distanceSq = katamariIO$getCameraDistanceSq(player);
        if (distanceSq >= 256.0 * 256.0) return 8;
        if (distanceSq >= 128.0 * 128.0) return 4;
        if (distanceSq >= 64.0 * 64.0) return 2;
        return 1;
    }

    private static double katamariIO$getCameraDistanceSq(Player player) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return 0.0;
        }
        return camera.position().distanceToSqr(player.position());
    }

    private static boolean katamariIO$shouldRenderByHash(Vec3 vec, int index, int entityId, int step) {
        if (step <= 1) return true;
        long x = Math.round(vec.x * 2.0);
        long y = Math.round(vec.y * 2.0);
        long z = Math.round(vec.z * 2.0);
        int hash = (int) (x * 73428767L ^ y * 912931L ^ z * 389129L ^ (long) index * 12289L ^ (long) entityId * 19997L);
        return Math.floorMod(hash, step) == 0;
    }

}
