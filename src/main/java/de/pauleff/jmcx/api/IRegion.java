package de.pauleff.jmcx.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Interface representing a Minecraft region containing a 32x32 grid of chunks.
 * Each region corresponds to a single .mca file and contains up to 1024 chunks.
 * 
 * @author Paul Ferlitz
 * @since 0.2
 */
public interface IRegion
{
    /**
     * Gets the x-coordinate of this region.
     * 
     * @return the x-coordinate
     */
    int getX();

    /**
     * Gets the z-coordinate of this region.
     * 
     * @return the z-coordinate
     */
    int getZ();

    /**
     * Gets all 1024 chunks in this region, including empty chunks.
     * 
     * @return list of all chunks
     */
    List<IChunk> getChunks();

    /**
     * Gets a specific chunk by coordinates.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return the chunk if it exists, empty otherwise
     */
    Optional<IChunk> getChunk(int chunkX, int chunkZ);

    /**
     * Gets chunks containing entities with Owner or Target tags.
     * 
     * @return list of chunks with ownable entities
     * @throws IOException if reading chunk data fails
     */
    List<IChunk> getChunksWithOwnables() throws IOException;

    /**
     * Replaces a chunk in this region.
     * 
     * @param chunk the new chunk
     * @throws IOException if replacement fails
     */
    void replaceChunk(IChunk chunk) throws IOException;

    /**
     * Checks if this region contains a chunk at the given coordinates.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return true if chunk exists at coordinates
     */
    boolean containsChunk(int chunkX, int chunkZ);

    /**
     * Gets the block coordinate range [minX, minZ, maxX, maxZ] for this region.
     * 
     * @return array of [minX, minZ, maxX, maxZ] coordinates
     */
    int[] getBlockCoordinateRange();

    /**
     * Gets the starting block coordinates [startX, startZ] of this region.
     * 
     * @return array of [startX, startZ] coordinates
     */
    int[] getStartingBlockCoordinates();

    /**
     * Checks if the given {@link IChunk} belongs to this region.
     * 
     * @param chunk the chunk to check
     * @return true if chunk belongs to this region
     */
    boolean chunkInRegion(IChunk chunk);
}