package com.github.tacowasa059.katamariio.common.accessors;

import com.github.tacowasa059.katamariio.common.data.KatamariBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public interface ICustomPlayerData {
    void katamariIO$setSize(float size);
    float katamariIO$getSize();
    void katamariIO$setCollisionSize(float size);
    float katamariIO$getCollisionSize();
    void katamariIO$setRenderSize(float size);
    float katamariIO$getRenderSize();
    float katamariIO$getOuterRadius();
    float katamariIO$getOuterSize();

    void katamariIO$setRestitutionCoefficient(float value);
    float katamariIO$getRestitutionCoefficient();
    void katamariIO$setFlag(boolean flag);
    void katamariIO$setFlagAndSizeAndRestitution(boolean flag, float collisionSize, float renderSize, float value);
    boolean katamariIO$getFlag();
    void katamariIO$setQuaternion(Quaternionf quaternion);

    Quaternionf katamariIO$getQuaternion();

    Quaternionf katamariIO$getInterpolatedQuaternion(float partialTicks);

    void katamariIO$setCurrentPosition(Vec3 pos);
    Vec3 katamariIO$getCurrentPosition();

    void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3);
    default void katamariIO$addBlock(Block block, Quaternionf quaternionf, Vec3 vec3, float extent) {
        katamariIO$addBlock(block, quaternionf, vec3);
    }
    int katamariIO$getAttachedBlockCount();
    default int katamariIO$getAttachedEntityCount() {
        return 0;
    }
    default int katamariIO$getTotalAttachedCount() {
        return katamariIO$getAttachedBlockCount() + katamariIO$getAttachedEntityCount();
    }
    void katamariIO$clearAttachedBlocks();
    void katamariIO$clearAttachedEntities();
    void katamariIO$resetAll();

    KatamariBlocks katamariIO$getBlocks();

    float katamariIO$getRadiusInDirection(float nx, float ny, float nz);
    void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat, float extent);
    default void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat, float extent, float volume) {
        katamariIO$setPassengerLocalPos(entityId, localPos, attachQuat, extent);
    }
    void katamariIO$setPassengerLocalPos(int entityId, org.joml.Vector3f localPos, org.joml.Quaternionf attachQuat);
    org.joml.Vector3f katamariIO$getPassengerLocalPos(int entityId);
    org.joml.Quaternionf katamariIO$getPassengerAttachQuat(int entityId);
    void katamariIO$removePassengerLocalPos(int entityId);
    void katamariIO$clearPassengerLocalPos();
    void katamariIO$invalidateBinRadii();
}
