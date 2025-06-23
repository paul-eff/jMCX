package de.pauleff.jmcx.util;

import de.pauleff.jmcx.api.IChunk;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for filtering {@link IChunk} collections based on common criteria.
 *
 * @author Paul Ferlitz
 */
public class ChunkFilter
{
    /**
     * Filters chunks by data version range.
     *
     * @param chunks list of {@link IChunk} to filter
     * @param minVersion minimum data version (inclusive)
     * @param maxVersion maximum data version (inclusive)
     * @return chunks matching data version criteria
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
     * @param chunks list of {@link IChunk} to filter
     * @param minChunkX minimum chunk X coordinate (inclusive)
     * @param minChunkZ minimum chunk Z coordinate (inclusive)
     * @param maxChunkX maximum chunk X coordinate (inclusive)
     * @param maxChunkZ maximum chunk Z coordinate (inclusive)
     * @return chunks within coordinate range
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
     *
     * @param chunks list of {@link IChunk} to filter
     * @param minBlockX minimum block X coordinate (inclusive)
     * @param minBlockZ minimum block Z coordinate (inclusive)
     * @param maxBlockX maximum block X coordinate (inclusive)
     * @param maxBlockZ maximum block Z coordinate (inclusive)
     * @return chunks containing blocks within specified range
     */
    public static List<IChunk> filterByBlockCoordinates(List<IChunk> chunks, int minBlockX, int minBlockZ,
                                                        int maxBlockX, int maxBlockZ)
    {
        int[] minChunk = AnvilUtils.blockToChunk(minBlockX, minBlockZ);
        int[] maxChunk = AnvilUtils.blockToChunk(maxBlockX, maxBlockZ);

        return filterByCoordinateRange(chunks, minChunk[0], minChunk[1], maxChunk[0], maxChunk[1]);
    }

    /**
     * Filters chunks containing ownable entities (Owner or Target tags).
     *
     * @param chunks list of {@link IChunk} to filter
     * @return chunks containing ownable entities
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
                        System.err.println("Warning: Failed to check ownable entities for chunk at (" +
                                chunk.getX() + ", " + chunk.getZ() + "): " + e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Filters non-empty chunks.
     *
     * @param chunks list of {@link IChunk} to filter
     * @return non-empty chunks only
     */
    public static List<IChunk> filterNonEmpty(List<IChunk> chunks)
    {
        return chunks.stream()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .collect(Collectors.toList());
    }

}