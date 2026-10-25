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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SphereRenderer {
    // The sphere geometry (unit positions + UVs) only depends on the segment count, not on the
    // radius. Build it once per segment count and cache it, then just scale by radius each frame
    // instead of recomputing every sin/cos/sqrt. The two shell layers (radius and radius*1.01)
    // are baked into the cached unit positions, so a single multiply by the frame radius restores
    // the exact same vertices the per-frame math used to produce. Layout: 5 floats per vertex
    // (ux, uy, uz, u, v).
    private static final Map<Integer, float[]> MESH_CACHE = new HashMap<>();

    public static <T extends MultiBufferSource> void drawTexturedSphere(PoseStack poseStack, T buffer,
                                                                        ResourceLocation texture, float radius, int segments, float x, float z, int packedLight, boolean lightmap2, int overlay) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI / 4, 0, 1, 0)));
        Matrix4f positionMatrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        float[] mesh = MESH_CACHE.computeIfAbsent(segments, SphereRenderer::buildMesh);
        for (int k = 0; k < mesh.length; k += 5) {
            float nx = mesh[k] * radius;
            float ny = mesh[k + 1] * radius;
            float nz = mesh[k + 2] * radius;

            if (lightmap2) {
                vertexConsumer.vertex(positionMatrix, nx + x, ny, nz + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                        .uv(mesh[k + 3], mesh[k + 4]).overlayCoords(overlay).uv2(packedLight)
                        .normal(normalMatrix, nx, ny, nz).endVertex();
            } else {
                vertexConsumer.vertex(positionMatrix, nx + x, ny, nz + z).color(1.0f, 1.0f, 1.0f, 1.0f)
                        .uv(mesh[k + 3], mesh[k + 4]).overlayCoords(overlay).uv2(packedLight)
                        .normal(nx, ny, nz).endVertex();
            }
        }

        poseStack.mulPose(new Quaternionf(new AxisAngle4f(-(float) Math.PI / 4, 0, 1, 0)));
    }

    private static float[] buildMesh(int segments) {
        MeshBuilder builder = new MeshBuilder();
        addBottomSphere(builder, 1.0F, segments, 0.1875F, false);
        addBottomSphere(builder, 1.01F, segments, 0.6875F, false);
        addBottomSphere(builder, 1.0F, segments, 0.3125F, true);
        addBottomSphere(builder, 1.01F, segments, 0.8125F, true);
        addSideSphere(builder, 1.0F, segments, false);
        addSideSphere(builder, 1.01F, segments, true);
        return builder.toArray();
    }

    private static void addBottomSphere(MeshBuilder builder, float radius, int segments, float u0, boolean isLower) {
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
                addQuad(builder, posList, uvList);
            }
        }
    }

    private static void addSideSphere(MeshBuilder builder, float radius, int segments, boolean isInner) {
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
                addQuad(builder, posList, uvList);
            }
        }
    }

    private static void addQuad(MeshBuilder builder, float[] posList, float[] uvList) {
        builder.add(posList[0], posList[1], posList[2], uvList[0], uvList[1]);
        builder.add(posList[3], posList[4], posList[5], uvList[2], uvList[3]);
        builder.add(posList[9], posList[10], posList[11], uvList[6], uvList[7]);
        builder.add(posList[6], posList[7], posList[8], uvList[4], uvList[5]);
    }

    private static float getSquareZ(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 + 2 * Math.sqrt(2.0) * z1)
                - 0.5 * Math.sqrt(2 - x1 * x1 + z1 * z1 - 2 * Math.sqrt(2.0) * z1)));
    }

    private static float getSquareX(float x1, float z1, float cubeSize) {
        return (float) (cubeSize * (0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 + 2 * Math.sqrt(2.0) * x1)
                - 0.5 * Math.sqrt(2 + x1 * x1 - z1 * z1 - 2 * Math.sqrt(2.0) * x1)));
    }

    private static final class MeshBuilder {
        private float[] data = new float[1024];
        private int size = 0;

        void add(float x, float y, float z, float u, float v) {
            if (size + 5 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = x;
            data[size++] = y;
            data[size++] = z;
            data[size++] = u;
            data[size++] = v;
        }

        float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
