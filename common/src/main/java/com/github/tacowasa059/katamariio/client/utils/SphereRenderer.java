package com.github.tacowasa059.katamariio.client.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class SphereRenderer {
    public static <T extends MultiBufferSource> void drawTexturedSphere(PoseStack poseStack, T buffer,
                                                                        ResourceLocation texture, float radius, int segments, float x, float z, int packedLight, boolean lightmap2, int overlay) {
        Matrix4f positionMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0)));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        addBottomSphere(radius, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, 0.1875F, false, packedLight, lightmap2, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, 0.6875F, false, packedLight, lightmap2, overlay);
        addBottomSphere(radius, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, 0.3125F, true, packedLight, lightmap2, overlay);
        addBottomSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, 0.8125F, true, packedLight, lightmap2, overlay);
        addSideSphere(radius, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, false, packedLight, lightmap2, overlay);
        addSideSphere(radius * 1.01F, segments, x, z, vertexConsumer, positionMatrix, normalMatrix, true, packedLight, lightmap2, overlay);

        poseStack.mulPose(new Quaternionf(new AxisAngle4f(-(float) Math.PI / 4, 0, 1, 0)));
    }

    private static void addBottomSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                        Matrix4f positionMatrix, Matrix3f normalMatrix, float u0, boolean isLower, int packedLight,
                                        boolean lightmap2, int overlay) {
        float cubeSize = 0.0625F;
        for (int j = 0; j < Math.round(segments / 4f); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4f)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4f)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI * 2 * (i + 1) / segments;
                if (isLower) {
                    angle = -angle;
                    nextAngle = -nextAngle;
                }

                float dx = (float) (radius * sinTheta * Math.cos(angle));
                float dz = (float) (radius * sinTheta * Math.sin(angle));
                float nextIdx = (float) (radius * sinTheta * Math.cos(nextAngle));
                float nextIdz = (float) (radius * sinTheta * Math.sin(nextAngle));
                float nextJdx = (float) (radius * nextSinTheta * Math.cos(angle));
                float nextJdz = (float) (radius * nextSinTheta * Math.sin(angle));
                float nextIJdx = (float) (radius * nextSinTheta * Math.cos(nextAngle));
                float nextIJdz = (float) (radius * nextSinTheta * Math.sin(nextAngle));

                float[] posList = isLower
                        ? new float[]{dx, -radius * cosTheta, dz, nextIdx, -radius * cosTheta, nextIdz, nextJdx, -radius * nextCosTheta, nextJdz, nextIJdx, -radius * nextCosTheta, nextIJdz}
                        : new float[]{dx, radius * cosTheta, dz, nextIdx, radius * cosTheta, nextIdz, nextJdx, radius * nextCosTheta, nextJdz, nextIJdx, radius * nextCosTheta, nextIJdz};

                float x1 = (float) Math.cos(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float z1 = (float) Math.sin(angle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIx1 = (float) Math.cos(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextIz1 = (float) Math.sin(nextAngle - Math.PI / 4) * sinTheta * (float) Math.sqrt(2.0F);
                float nextJx1 = (float) Math.cos(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextJz1 = (float) Math.sin(angle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJx1 = (float) Math.cos(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);
                float nextIJz1 = (float) Math.sin(nextAngle - Math.PI / 4) * nextSinTheta * (float) Math.sqrt(2.0F);

                float u = u0 + getSquareX(x1, z1, cubeSize);
                float v = 0.0625F + getSquareZ(x1, z1, cubeSize);
                float nextIU = u0 + getSquareX(nextIx1, nextIz1, cubeSize);
                float nextIV = 0.0625F + getSquareZ(nextIx1, nextIz1, cubeSize);
                float nextJU = u0 + getSquareX(nextJx1, nextJz1, cubeSize);
                float nextJV = 0.0625F + getSquareZ(nextJx1, nextJz1, cubeSize);
                float nextIJU = u0 + getSquareX(nextIJx1, nextIJz1, cubeSize);
                float nextIJV = 0.0625F + getSquareZ(nextIJx1, nextIJz1, cubeSize);

                float[] uvList = new float[]{u, v, nextIU, nextIV, nextJU, nextJV, nextIJU, nextIJV};
                addSideSphereQuads(vertexBuilder, positionMatrix, normalMatrix, x, z, posList, uvList, packedLight, lightmap2, overlay);
            }
        }
    }

    private static void addSideSphere(float radius, int segments, float x, float z, VertexConsumer vertexBuilder,
                                      Matrix4f positionMatrix, Matrix3f normalMatrix, boolean isInner, int packedLight,
                                      boolean lightmap2, int overlay) {
        for (int j = Math.round(segments / 4f); j < Math.round(3 * segments / 4f); j++) {
            float theta = (float) (Math.PI * j / (4 * Math.round(segments / 4f)));
            float sinTheta = (float) Math.sin(theta);
            float cosTheta = (float) Math.cos(theta);
            float nextTheta = (float) (Math.PI * (j + 1) / (4 * Math.round(segments / 4f)));
            float nextSinTheta = (float) Math.sin(nextTheta);
            float nextCosTheta = (float) Math.cos(nextTheta);

            for (int i = 0; i < segments; i++) {
                double angle = -Math.PI / 2 - Math.PI * 2 * i / segments;
                double nextAngle = -Math.PI / 2 - Math.PI * 2 * (i + 1) / segments;

                float dx = (float) (radius * Math.cos(angle) * sinTheta);
                float dz = (float) (radius * Math.sin(angle) * sinTheta);
                float nextIdx = (float) (radius * Math.cos(nextAngle) * sinTheta);
                float nextIdz = (float) (radius * Math.sin(nextAngle) * sinTheta);
                float nextJdx = (float) (radius * Math.cos(angle) * nextSinTheta);
                float nextJdz = (float) (radius * Math.sin(angle) * nextSinTheta);
                float nextIJdx = (float) (radius * Math.cos(nextAngle) * nextSinTheta);
                float nextIJdz = (float) (radius * Math.sin(nextAngle) * nextSinTheta);

                float[] posList = new float[]{
                        dx, radius * cosTheta, dz,
                        nextIdx, radius * cosTheta, nextIdz,
                        nextJdx, radius * nextCosTheta, nextJdz,
                        nextIJdx, radius * nextCosTheta, nextIJdz
                };

                float u = 0.5F * i / segments;
                float v = 0.125F + 0.125F * (j - Math.round(segments / 4f)) / Math.round(segments / 2f);
                float nextIU = 0.5F * (i + 1) / segments;
                float nextJU = 0.5F * i / segments;
                float nextJV = 0.125F + 0.125F * (j + 1 - Math.round(segments / 4f)) / Math.round(segments / 2f);
                float nextIJU = 0.5F * (i + 1) / segments;

                if (isInner) {
                    u += 0.5F;
                    nextIU += 0.5F;
                    nextJU += 0.5F;
                    nextIJU += 0.5F;
                }

                float[] uvList = new float[]{u, v, nextIU, v, nextJU, nextJV, nextIJU, nextJV};
                addSideSphereQuads(vertexBuilder, positionMatrix, normalMatrix, x, z, posList, uvList, packedLight, lightmap2, overlay);
            }
        }
    }

    private static void addSideSphereQuads(VertexConsumer vertexConsumer, Matrix4f positionMatrix, Matrix3f normalMatrix,
                                           float x, float z, float[] posList, float[] uvList, int packedLight, boolean lightmap2, int overlay) {
        if (lightmap2) {
            vertexConsumer.vertex(positionMatrix, posList[0] + x, posList[1], posList[2] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[0], uvList[1]).overlayCoords(overlay).uv2(packedLight).normal(normalMatrix, posList[0], posList[1], posList[2]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[3] + x, posList[4], posList[5] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[2], uvList[3]).overlayCoords(overlay).uv2(packedLight).normal(normalMatrix, posList[3], posList[4], posList[5]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[9] + x, posList[10], posList[11] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[6], uvList[7]).overlayCoords(overlay).uv2(packedLight).normal(normalMatrix, posList[9], posList[10], posList[11]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[6] + x, posList[7], posList[8] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[4], uvList[5]).overlayCoords(overlay).uv2(packedLight).normal(normalMatrix, posList[6], posList[7], posList[8]).endVertex();
        } else {
            vertexConsumer.vertex(positionMatrix, posList[0] + x, posList[1], posList[2] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[0], uvList[1]).overlayCoords(overlay).uv2(packedLight).normal(posList[0], posList[1], posList[2]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[3] + x, posList[4], posList[5] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[2], uvList[3]).overlayCoords(overlay).uv2(packedLight).normal(posList[3], posList[4], posList[5]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[9] + x, posList[10], posList[11] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[6], uvList[7]).overlayCoords(overlay).uv2(packedLight).normal(posList[9], posList[10], posList[11]).endVertex();
            vertexConsumer.vertex(positionMatrix, posList[6] + x, posList[7], posList[8] + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                    .uv(uvList[4], uvList[5]).overlayCoords(overlay).uv2(packedLight).normal(posList[6], posList[7], posList[8]).endVertex();
        }
    }

    private static float getSquareZ(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 + 2 * Math.sqrt(2.0) * z1)
                - 0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 - 2 * Math.sqrt(2.0) * z1)));
    }

    private static float getSquareX(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 + 2 * Math.sqrt(2.0) * x1)
                - 0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 - 2 * Math.sqrt(2.0) * x1)));
    }
}
