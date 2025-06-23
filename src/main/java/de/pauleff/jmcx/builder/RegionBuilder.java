package de.pauleff.jmcx.builder;

import de.pauleff.jmcx.api.AnvilFactory;
import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Location;
import de.pauleff.jmcx.core.Region;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;

/**
 * Builder class for creating {@link IRegion} objects using the builder pattern.
 *
 * @author Paul Ferlitz
 */
public class RegionBuilder
{
    private final Map<Integer, IChunk> chunks = new HashMap<>();
    private int regionX;
    private int regionZ;
    private boolean validateChunks = true;

    private RegionBuilder()
    {
    }

    /**
     * Creates a new RegionBuilder instance.
     *
     * @return new RegionBuilder
     */
    public static RegionBuilder create()
    {
        return new RegionBuilder();
    }

    /**
     * Creates a RegionBuilder from an existing region file.
     *
     * @param file region file to load
     * @return RegionBuilder populated with file data
     * @throws IOException if reading file fails
     */
    public static RegionBuilder fromFile(File file) throws IOException
    {
        RegionBuilder builder = new RegionBuilder();

        try (var reader = AnvilFactory.createReader(file))
        {
            IRegion region = reader.readRegion();
            builder.regionX = region.getX();
            builder.regionZ = region.getZ();

            for (IChunk chunk : region.getChunks())
            {
                if (!chunk.isEmpty())
                {
                    builder.chunks.put(chunk.getIndex(), chunk);
                }
            }
        }

        return builder;
    }

    /**
     * Creates a RegionBuilder from an existing region file path.
     *
     * @param filePath path to region file
     * @return RegionBuilder populated with file data
     * @throws IOException if reading file fails
     */
    public static RegionBuilder fromFile(String filePath) throws IOException
    {
        return fromFile(new File(filePath));
    }

    /**
     * Sets region coordinates.
     *
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @return this builder
     */
    public RegionBuilder withCoordinates(int regionX, int regionZ)
    {
        this.regionX = regionX;
        this.regionZ = regionZ;
        return this;
    }

    /**
     * Adds a chunk to the region.
     *
     * @param chunk {@link IChunk} to add
     * @return this builder
     * @throws IllegalArgumentException if chunk invalid or conflicts
     */
    public RegionBuilder addChunk(IChunk chunk)
    {
        if (chunk == null)
        {
            throw new IllegalArgumentException("Chunk cannot be null");
        }

        if (validateChunks)
        {
            validateChunkCoordinates(chunk);
        }

        chunks.put(chunk.getIndex(), chunk);
        return this;
    }

    /**
     * Adds multiple chunks to the region.
     *
     * @param chunks {@link IChunk} list to add
     * @return this builder
     */
    public RegionBuilder addChunks(List<IChunk> chunks)
    {
        for (IChunk chunk : chunks)
        {
            addChunk(chunk);
        }
        return this;
    }

    /**
     * Adds empty chunk at specified coordinates.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return this builder
     * @throws IOException if creating empty chunk fails
     */
    public RegionBuilder addEmptyChunk(int chunkX, int chunkZ) throws IOException
    {
        int index = calculateChunkIndex(chunkX, chunkZ);
        Location location = Location.createEmptyLocation();
        Chunk emptyChunk = new Chunk(index, location, 0, new byte[0]);
        return addChunk(emptyChunk);
    }

    /**
     * Removes chunk from the region.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return this builder
     */
    public RegionBuilder removeChunk(int chunkX, int chunkZ)
    {
        int index = calculateChunkIndex(chunkX, chunkZ);
        chunks.remove(index);
        return this;
    }

    /**
     * Removes chunk from the region by index.
     *
     * @param index chunk index (0-1023)
     * @return this builder
     */
    public RegionBuilder removeChunk(int index)
    {
        if (index < 0 || index >= CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException("Chunk index must be between 0 and " + (CHUNKS_PER_REGION - 1) + ", got: " + index);
        }
        chunks.remove(index);
        return this;
    }

    /**
     * Enables or disables chunk validation.
     *
     * @param validate whether to validate chunks
     * @return this builder
     */
    public RegionBuilder validateChunks(boolean validate)
    {
        this.validateChunks = validate;
        return this;
    }

    /**
     * Clears all chunks from the builder.
     *
     * @return this builder
     */
    public RegionBuilder clearChunks()
    {
        chunks.clear();
        return this;
    }

    /**
     * Gets number of chunks in the builder.
     *
     * @return chunk count
     */
    public int getChunkCount()
    {
        return chunks.size();
    }

    /**
     * Checks if builder has any chunks.
     *
     * @return true if builder contains chunks
     */
    public boolean hasChunks()
    {
        return !chunks.isEmpty();
    }

    /**
     * Builds the {@link IRegion} from configured chunks.
     *
     * @return new {@link IRegion} instance
     * @throws IOException if region construction fails
     * @throws IllegalStateException if builder state invalid
     */
    public IRegion build() throws IOException
    {
        validateBuilder();

        List<IChunk> allChunks = new ArrayList<>(CHUNKS_PER_REGION);

        for (int i = 0; i < CHUNKS_PER_REGION; i++)
        {
            IChunk chunk = chunks.get(i);
            if (chunk != null)
            {
                allChunks.add(chunk);
            } else
            {
                Location emptyLocation = Location.createEmptyLocation();
                Chunk emptyChunk = new Chunk(i, emptyLocation, 0, new byte[0]);
                allChunks.add(emptyChunk);
            }
        }

        return new Region(regionX, regionZ, allChunks);
    }

    /**
     * Calculates chunk index within region from coordinates.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return chunk index (0-1023)
     */
    private int calculateChunkIndex(int chunkX, int chunkZ)
    {
        int localX = chunkX % CHUNKS_PER_REGION_SIDE;
        int localZ = chunkZ % CHUNKS_PER_REGION_SIDE;
        return localZ * CHUNKS_PER_REGION_SIDE + localX;
    }

    /**
     * Validates chunk coordinates against region boundaries.
     *
     * @param chunk {@link IChunk} to validate
     * @throws IllegalArgumentException if chunk coordinates invalid
     */
    private void validateChunkCoordinates(IChunk chunk)
    {
        int expectedRegionX = chunk.getX() / CHUNKS_PER_REGION_SIDE;
        int expectedRegionZ = chunk.getZ() / CHUNKS_PER_REGION_SIDE;

        if (expectedRegionX != regionX || expectedRegionZ != regionZ)
        {
            throw new IllegalArgumentException(
                    String.format("Chunk at (%d, %d) does not belong to region (%d, %d). " +
                                    "Expected region: (%d, %d)",
                            chunk.getX(), chunk.getZ(), regionX, regionZ,
                            expectedRegionX, expectedRegionZ)
            );
        }
    }

    /**
     * Validates builder state before building.
     *
     * @throws IllegalStateException if builder state invalid
     */
    private void validateBuilder()
    {
        if (chunks.size() > CHUNKS_PER_REGION)
        {
            throw new IllegalStateException("Too many chunks: " + chunks.size() + " (maximum " + CHUNKS_PER_REGION + ")");
        }
    }
}