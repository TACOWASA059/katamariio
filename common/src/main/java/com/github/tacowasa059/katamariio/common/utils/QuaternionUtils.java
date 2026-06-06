package com.github.tacowasa059.katamariio.common.utils;

import com.github.tacowasa059.katamariio.common.accessors.ICustomPlayerData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Math;
import org.joml.Quaternionf;

public class QuaternionUtils {
    public static Quaternionf getUpdatedQuaternion(Vec3 currentPosition, Vec3 prevPosition, ICustomPlayerData playerData) {
        if (prevPosition == null) {
            return null;
        }

        Vec3 movement = currentPosition.subtract(prevPosition);
        double distance = movement.length();
        movement = movement.subtract(0, movement.y(), 0);

        float radius = playerData.katamariIO$getSize() / 2.0f;
        Quaternionf quaternion = playerData.katamariIO$getQuaternion();
        if (quaternion.equals(new Quaternionf(0, 0, 0, 0))) {
            quaternion = new Quaternionf(0, 0, 0, 1);
        }

        double angle = distance / radius;
        Vec3 axisD = movement.yRot((float) (org.joml.Math.PI / 2.0)).normalize();
        if (axisD.equals(Vec3.ZERO)) {
            return null;
        }

        Vec3 axis = new Vec3((float) axisD.x, 0, (float) axisD.z);
        Quaternionf rotation = new Quaternionf(new AxisAngle4f((float) angle, (float) axis.x, (float) axis.y, (float) axis.z));
        rotation = normalize(rotation);
        rotation.mul(quaternion);
        return normalize(rotation);
    }

    private static Quaternionf normalize(Quaternionf quaternion) {
        float lengthSq = quaternion.x * quaternion.x + quaternion.y * quaternion.y
                + quaternion.z * quaternion.z + quaternion.w() * quaternion.w;
        if (lengthSq > 1.0E-6F) {
            float invSqrt = fastInvSqrt(lengthSq);
            return new Quaternionf(quaternion.x() * invSqrt, quaternion.y() * invSqrt,
                    quaternion.z() * invSqrt, quaternion.w() * invSqrt);
        }
        return new Quaternionf(0.0F, 0.0F, 0.0F, 0.0F);
    }

    private static float fastInvSqrt(float value) {
        float half = 0.5F * value;
        int bits = Float.floatToIntBits(value);
        bits = 1597463007 - (bits >> 1);
        value = Float.intBitsToFloat(bits);
        value *= 1.5F - half * value * value;
        return value;
    }

    public static Quaternionf getQuaternionFromEntity(Entity entity) {
        float pitch = entity.getXRot();
        float yaw = entity.getYRot();
        Quaternionf pitchQuat = new Quaternionf(new AxisAngle4f((float) (pitch * org.joml.Math.PI / 180), 1, 0, 0));
        Quaternionf yawQuat = new Quaternionf(new AxisAngle4f((float) (-yaw * org.joml.Math.PI / 180), 0, 1, 0));
        yawQuat.mul(pitchQuat);
        return yawQuat;
    }

    public static Quaternionf slerp(Quaternionf start, Quaternionf end, float t) {
        float dot = start.x() * end.x() + start.y() * end.y() + start.z() * end.z() + start.w() * end.w();
        if (dot < 0.0f) {
            end = new Quaternionf(-end.x(), -end.y(), -end.z(), -end.w());
            dot = -dot;
        }

        float theta0 = org.joml.Math.acos(dot);
        float theta = theta0 * t;
        float sinTheta = org.joml.Math.sin(theta);
        float sinTheta0 = org.joml.Math.sin(theta0);
        float s0 = Math.cos(theta) - dot * sinTheta / sinTheta0;
        float s1 = sinTheta / sinTheta0;

        return new Quaternionf(
                (s0 * start.x()) + (s1 * end.x),
                (s0 * start.y()) + (s1 * end.y),
                (s0 * start.z()) + (s1 * end.z),
                (s0 * start.w()) + (s1 * end.w)
        );
    }
}
