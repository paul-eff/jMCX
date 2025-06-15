package de.pauleff.jmcx.api;

import de.pauleff.jnbt.api.ICompoundTag;

import java.io.IOException;

/**
 * Interface representing a single chunk within a Minecraft region.
 * Each chunk covers a 16x16 block area and contains block data, entities, and metadata.
 * 
 * @author Paul Ferlitz
 * @since 0.2
 */
public interface IChunk
{
    /**
     * Gets the x-coordinate of this chunk.
     * 
     * @return the x-coordinate
     */
    int getX();

    /**
     * Gets the z-coordinate of this chunk.
     * 
     * @return the z-coordinate
     */
    int getZ();

    /**
     * Gets the Minecraft data version of this chunk.
     * 
     * @return the data version
     */
    int getDataVersion();

    /**
     * Gets the timestamp when this chunk was last modified (seconds since Unix epoch).
     * 
     * @return the timestamp
     */
    int getTimestamp();

    /**
     * Gets the {@link ICompoundTag} containing all chunk data, or null if empty.
     * 
     * @return the NBT data or null if empty
     * @throws IOException if reading fails
     */
    ICompoundTag getNBTData() throws IOException;

    /**
     * Sets new NBT data for this chunk using the given {@link ICompoundTag}.
     * 
     * @param nbtData the new NBT data
     * @throws IOException if writing fails
     */
    void setNBTData(ICompoundTag nbtData) throws IOException;

    /**
     * Checks if this chunk contains entities with Owner or Target tags.
     * 
     * @return true if ownable entities exist
     * @throws IOException if reading fails
     */
    boolean hasOwnableEntities() throws IOException;

    /**
     * Checks if the given block coordinates fall within this chunk.
     * 
     * @param blockX the block x-coordinate
     * @param blockZ the block z-coordinate
     * @return true if block is in this chunk
     */
    boolean isBlockInChunk(int blockX, int blockZ);

    /**
     * Gets the block coordinate range [minX, minZ, maxX, maxZ] for this chunk.
     * 
     * @return array of [minX, minZ, maxX, maxZ] coordinates
     */
    int[] getBlockCoordinateRange();

    /**
     * Gets the starting block coordinates [startX, startZ] of this chunk.
     * 
     * @return array of [startX, startZ] coordinates
     */
    int[] getStartingBlockCoordinates();

    /**
     * Converts this chunk's coordinates to region coordinates [regionX, regionZ].
     * 
     * @return array of [regionX, regionZ] coordinates
     */
    int[] chunkToRegionCoordinate();

    /**
     * Gets the index of this chunk within its region (0-1023).
     * 
     * @return the chunk index
     */
    int getIndex();

    /**
     * Checks if this chunk is empty or ungenerated.
     * 
     * @return true if chunk is empty
     */
    boolean isEmpty();

    /**
     * Gets the size of the compressed chunk data in bytes.
     * 
     * @return the data size in bytes
     */
    int getDataSize();
}