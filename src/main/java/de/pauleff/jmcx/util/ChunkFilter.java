package de.pauleff.jmcx.util;

import de.pauleff.jmcx.api.IChunk;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for filtering chunks based on common criteria.
 * Provides essential filtering methods for the most frequent use cases.
 */
public class ChunkFilter
{
    /**
     * Filters chunks by data version range.
     *
     * @param chunks     the list of chunks to filter
     * @param minVersion the minimum data version (inclusive)
     * @param maxVersion the maximum data version (inclusive)
     * @return a list of chunks matching the data version criteria
     */
    public static List<IChunk> filterByDataVersion(List<IChunk> chunks, int minVersion, int maxVersion)
    {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .filter(chunk -> {
                    int dataVersion = chunk.getDataVersion();
                    return dataVersion >= minVersion && dataVersion <= maxVersion;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters chunks by coordinate range.
     *
     * @param chunks     the list of chunks to filter
     * @param minChunkX  the minimum chunk X coordinate (inclusive)
     * @param minChunkZ  the minimum chunk Z coordinate (inclusive)
     * @param maxChunkX  the maximum chunk X coordinate (inclusive)
     * @param maxChunkZ  the maximum chunk Z coordinate (inclusive)
     * @return a list of chunks within the coordinate range
     */
    public static List<IChunk> filterByCoordinateRange(List<IChunk> chunks, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ)
    {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .filter(chunk -> {
                    int x = chunk.getX();
                    int z = chunk.getZ();
                    return x >= minChunkX && x <= maxChunkX && z >= minChunkZ && z <= maxChunkZ;
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters chunks by block coordinate range.
     * Converts block coordinates to chunk coordinates and filters accordingly.
     *
     * @param chunks    the list of chunks to filter
     * @param minBlockX the minimum block X coordinate (inclusive)
     * @param minBlockZ the minimum block Z coordinate (inclusive)
     * @param maxBlockX the maximum block X coordinate (inclusive)
     * @param maxBlockZ the maximum block Z coordinate (inclusive)
     * @return a list of chunks that contain blocks within the specified range
     */
    public static List<IChunk> filterByBlockCoordinates(List<IChunk> chunks, int minBlockX, int minBlockZ,
                                                        int maxBlockX, int maxBlockZ)
    {
        // Convert block coordinates to chunk coordinates
        int[] minChunk = AnvilUtils.blockToChunk(minBlockX, minBlockZ);
        int[] maxChunk = AnvilUtils.blockToChunk(maxBlockX, maxBlockZ);

        return filterByCoordinateRange(chunks, minChunk[0], minChunk[1], maxChunk[0], maxChunk[1]);
    }

    /**
     * Filters chunks that contain ownable entities (entities with Owner or Target tags).
     * This is useful for finding chunks that need UUID conversion.
     *
     * @param chunks the list of chunks to filter
     * @return a list of chunks containing ownable entities
     */
    public static List<IChunk> filterWithOwnables(List<IChunk> chunks)
    {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .filter(chunk -> {
                    try
                    {
                        return chunk.hasOwnableEntities();
                    } catch (IOException e)
                    {
                        // Log warning but continue processing
                        System.err.println("Warning: Failed to check ownable entities for chunk at (" +
                                chunk.getX() + ", " + chunk.getZ() + "): " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters non-empty chunks from a list.
     *
     * @param chunks the list of chunks to filter
     * @return a list containing only non-empty chunks
     */
    public static List<IChunk> filterNonEmpty(List<IChunk> chunks)
    {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .collect(Collectors.toList());
    }

}