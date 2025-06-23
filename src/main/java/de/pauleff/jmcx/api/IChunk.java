package de.pauleff.jmcx.api;

import de.pauleff.jnbt.api.ICompoundTag;

import java.io.IOException;

/**
 * Interface representing a single chunk within a Minecraft region.
 *
 * @author Paul Ferlitz
 */
public interface IChunk
{
    /**
     * Gets chunk x-coordinate.
     *
     * @return x-coordinate
     */
    int getX();

    /**
     * Gets chunk z-coordinate.
     *
     * @return z-coordinate
     */
    int getZ();

    /**
     * Gets Minecraft data version of this chunk.
     *
     * @return data version
     */
    int getDataVersion();

    /**
     * Gets last modified timestamp (seconds since Unix epoch).
     *
     * @return timestamp
     */
    int getTimestamp();

    /**
     * Gets NBT data containing all chunk data.
     *
     * @return {@link ICompoundTag} or null if empty
     * @throws IOException if reading fails
     */
    ICompoundTag getNBTData() throws IOException;

    /**
     * Sets new NBT data for this chunk.
     *
     * @param nbtData new {@link ICompoundTag}
     * @throws IOException if writing fails
     */
    void setNBTData(ICompoundTag nbtData) throws IOException;

    /**
     * Checks if chunk contains entities with Owner or Target tags.
     *
     * @return true if ownable entities exist
     * @throws IOException if reading fails
     */
    boolean hasOwnableEntities() throws IOException;

    /**
     * Checks if block coordinates fall within this chunk.
     *
     * @param blockX block x-coordinate
     * @param blockZ block z-coordinate
     * @return true if block is in chunk
     */
    boolean isBlockInChunk(int blockX, int blockZ);

    /**
     * Gets block coordinate range for this chunk.
     *
     * @return [minX, minZ, maxX, maxZ] coordinates
     */
    int[] getBlockCoordinateRange();

    /**
     * Gets starting block coordinates of this chunk.
     *
     * @return [startX, startZ] coordinates
     */
    int[] getStartingBlockCoordinates();

    /**
     * Converts chunk coordinates to region coordinates.
     *
     * @return [regionX, regionZ] coordinates
     */
    int[] chunkToRegionCoordinate();

    /**
     * Gets chunk index within its region (0-1023).
     *
     * @return chunk index
     */
    int getIndex();

    /**
     * Checks if chunk is empty or ungenerated.
     *
     * @return true if empty
     */
    boolean isEmpty();

    /**
     * Gets compressed chunk data size in bytes.
     *
     * @return data size
     */
    int getDataSize();

    /**
     * Checks if NBT data is loaded into memory.
     *
     * @return true if NBT data loaded
     */
    boolean isNBTLoaded();
}