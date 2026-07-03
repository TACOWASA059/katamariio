package com.github.tacowasa059.katamariio.common.data;

import net.minecraft.world.level.block.Block;
import org.joml.Quaternionf;

import java.util.Arrays;

/**
 * Struct-of-arrays store for the blocks attached to a katamari.
 * <p>
 * Replaces the previous three parallel {@code List<Vec3>} / {@code List<Quaternionf>} /
 * {@code List<Block>}. Holding the data in primitive arrays removes the per-block object
 * headers (a Vec3 and a Quaternionf per block plus three list slots, ~110 bytes) and drops
 * to ~36 bytes per block, without changing the stored values or the rendered result.
 */
public final class KatamariBlocks {
    private static final int DEFAULT_CAPACITY = 16;

    private float[] pos;    // x,y,z per block at [i*3 .. i*3+2]
    private float[] quat;   // x,y,z,w per block at [i*4 .. i*4+3]
    private Block[] blocks; // shared singleton references, no allocation
    private int size;

    public KatamariBlocks() {
        this(DEFAULT_CAPACITY);
    }

    public KatamariBlocks(int capacity) {
        int cap = Math.max(1, capacity);
        this.pos = new float[cap * 3];
        this.quat = new float[cap * 4];
        this.blocks = new Block[cap];
        this.size = 0;
    }

    public KatamariBlocks(KatamariBlocks other) {
        this.pos = Arrays.copyOf(other.pos, other.size * 3);
        this.quat = Arrays.copyOf(other.quat, other.size * 4);
        this.blocks = Arrays.copyOf(other.blocks, other.size);
        this.size = other.size;
    }

    public int size() {
        return size;
    }

    public Block block(int i) {
        return blocks[i];
    }

    public float posX(int i) {
        return pos[i * 3];
    }

    public float posY(int i) {
        return pos[i * 3 + 1];
    }

    public float posZ(int i) {
        return pos[i * 3 + 2];
    }

    public float quatX(int i) {
        return quat[i * 4];
    }

    public float quatY(int i) {
        return quat[i * 4 + 1];
    }

    public float quatZ(int i) {
        return quat[i * 4 + 2];
    }

    public float quatW(int i) {
        return quat[i * 4 + 3];
    }

    public double lengthSqr(int i) {
        int p = i * 3;
        double x = pos[p];
        double y = pos[p + 1];
        double z = pos[p + 2];
        return x * x + y * y + z * z;
    }

    public double length(int i) {
        return Math.sqrt(lengthSqr(i));
    }

    /** Copies block i's rotation into {@code dest} without allocating. */
    public void quatInto(int i, Quaternionf dest) {
        int r = i * 4;
        dest.set(quat[r], quat[r + 1], quat[r + 2], quat[r + 3]);
    }

    public void add(Block block, Quaternionf rotation, double x, double y, double z) {
        addRaw(block, rotation.x(), rotation.y(), rotation.z(), rotation.w(), (float) x, (float) y, (float) z);
    }

    public void addRaw(Block block, float qx, float qy, float qz, float qw, float x, float y, float z) {
        ensureCapacity(size + 1);
        int p = size * 3;
        int r = size * 4;
        pos[p] = x;
        pos[p + 1] = y;
        pos[p + 2] = z;
        quat[r] = qx;
        quat[r + 1] = qy;
        quat[r + 2] = qz;
        quat[r + 3] = qw;
        blocks[size] = block;
        size++;
    }

    public void clear() {
        Arrays.fill(blocks, 0, size, null);
        size = 0;
    }

    /**
     * Once the ball is at least {@code minRadius} big, keeps only the outer blocks per angular bin,
     * compacting in place. The bin grid is scaled to the radius so each bin is about one block wide
     * (a block's footprint covers its bin), which bounds the retained count to the visible surface
     * (~R^2) no matter how many blocks were ever collected, while keeping every block the renderer
     * could draw. Small balls (below {@code minRadius}) keep every block. Returns true if anything
     * was removed.
     */
    public boolean pruneToSurface(float minRadius, int minLatBins, int maxLatBins, float thickness) {
        if (size == 0) {
            return false;
        }

        // Ball radius = the farthest block. Small balls are left fully detailed.
        float maxLen = 0.0f;
        for (int i = 0; i < size; i++) {
            float len = (float) length(i);
            if (len > maxLen) {
                maxLen = len;
            }
        }
        if (maxLen < minRadius) {
            return false;
        }

        int latBins = Math.round((float) Math.PI * maxLen);
        if (latBins < minLatBins) {
            latBins = minLatBins;
        } else if (latBins > maxLatBins) {
            latBins = maxLatBins;
        }
        int lonBins = latBins * 2;

        float[] binMax = new float[latBins * lonBins];
        for (int i = 0; i < size; i++) {
            double len = length(i);
            if (len < 1.0E-4) {
                continue;
            }
            int bin = binIndex(i, len, latBins, lonBins);
            if ((float) len > binMax[bin]) {
                binMax[bin] = (float) len;
            }
        }

        int w = 0;
        for (int i = 0; i < size; i++) {
            double len = length(i);
            boolean keep = len < 1.0E-4 || len >= binMax[binIndex(i, len, latBins, lonBins)] - thickness;
            if (!keep) {
                continue;
            }
            if (w != i) {
                System.arraycopy(pos, i * 3, pos, w * 3, 3);
                System.arraycopy(quat, i * 4, quat, w * 4, 4);
                blocks[w] = blocks[i];
            }
            w++;
        }
        if (w == size) {
            return false;
        }
        Arrays.fill(blocks, w, size, null);
        size = w;
        return true;
    }

    private int binIndex(int i, double len, int latBins, int lonBins) {
        int p = i * 3;
        double ny = pos[p + 1] / len;
        int lat = (int) ((Math.asin(Math.max(-1.0, Math.min(1.0, ny))) + Math.PI / 2.0) / Math.PI * latBins);
        if (lat < 0) {
            lat = 0;
        } else if (lat >= latBins) {
            lat = latBins - 1;
        }
        int lon = (int) ((Math.atan2(pos[p + 2], pos[p]) + Math.PI) / (2.0 * Math.PI) * lonBins);
        if (lon < 0) {
            lon = 0;
        } else if (lon >= lonBins) {
            lon = lonBins - 1;
        }
        return lat * lonBins + lon;
    }

    private void ensureCapacity(int required) {
        if (required <= blocks.length) {
            return;
        }
        int newCap = Math.max(required, blocks.length + (blocks.length >> 1));
        pos = Arrays.copyOf(pos, newCap * 3);
        quat = Arrays.copyOf(quat, newCap * 4);
        blocks = Arrays.copyOf(blocks, newCap);
    }
}
