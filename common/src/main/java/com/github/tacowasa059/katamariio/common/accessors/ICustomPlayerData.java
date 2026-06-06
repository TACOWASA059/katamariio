package com.github.tacowasa059.katamariio.common.accessors;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

public interface ICustomPlayerData {
    void katamariIO$setSize(float size);
    float katamariIO$getSize();
    void katamariIO$setCollisionSize(float size);
    float katamariIO$getCollisionSize();
    void katamariIO$setRenderSize(float size);
    float katamariIO$getRenderSize();

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
    int katamariIO$getAttachedBlockCount();
    void katamariIO$clearAttachedBlocks();

    List<Vec3> katamariIO$getSphericalPlayerPositions();
    List<Quaternionf> katamariIO$getSphericalPlayerQuaternions();
    List<Block> katamariIO$getSphericalPlayerBlocks();
}
