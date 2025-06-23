package de.pauleff.jmcx.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Interface representing a Minecraft region containing a 32x32 grid of chunks.
 *
 * @author Paul Ferlitz
 */
public interface IRegion
{
    /**
     * Gets region x-coordinate.
     *
     * @return x-coordinate
     */
    int getX();

    /**
     * Gets region z-coordinate.
     *
     * @return z-coordinate
     */
    int getZ();

    /**
     * Gets all chunks in region including empty chunks.
     *
     * @return list of all {@link IChunk} objects
     */
    List<IChunk> getChunks();

    /**
     * Gets chunk by coordinates.
     *
     * @param chunkX chunk x-coordinate
     * @param chunkZ chunk z-coordinate
     * @return {@link IChunk} if exists, empty otherwise
     */
    Optional<IChunk> getChunk(int chunkX, int chunkZ);

    /**
     * Gets chunks containing entities with Owner or Target tags.
     *
     * @return list of {@link IChunk} objects with ownable entities
     * @throws IOException if reading fails
     */
    List<IChunk> getChunksWithOwnables() throws IOException;

    /**
     * Replaces a chunk in this region.
     *
     * @param chunk new {@link IChunk}
     * @throws IOException if replacement fails
     */
    void replaceChunk(IChunk chunk) throws IOException;

    /**
     * Checks if region contains chunk at coordinates.
     *
     * @param chunkX chunk x-coordinate
     * @param chunkZ chunk z-coordinate
     * @return true if chunk exists
     */
    boolean containsChunk(int chunkX, int chunkZ);

    /**
     * Gets block coordinate range for this region.
     *
     * @return [minX, minZ, maxX, maxZ] coordinates
     */
    int[] getBlockCoordinateRange();

    /**
     * Gets starting block coordinates of this region.
     *
     * @return [startX, startZ] coordinates
     */
    int[] getStartingBlockCoordinates();

    /**
     * Checks if chunk belongs to this region.
     *
     * @param chunk {@link IChunk} to check
     * @return true if chunk belongs to region
     */
    boolean chunkInRegion(IChunk chunk);
}