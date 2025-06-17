package de.pauleff.jmcx.builder;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.api.AnvilFactory;
import de.pauleff.jmcx.core.Region;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Location;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder class for creating Region objects using the builder pattern.
 * Provides a fluent API for constructing regions with chunks.
 */
public class RegionBuilder
{
    private int regionX;
    private int regionZ;
    private final Map<Integer, IChunk> chunks = new HashMap<>();
    private boolean validateChunks = true;

    private RegionBuilder()
    {
    }

    /**
     * Creates a new RegionBuilder instance.
     *
     * @return a new RegionBuilder
     */
    public static RegionBuilder create()
    {
        return new RegionBuilder();
    }

    /**
     * Creates a RegionBuilder from an existing region file.
     *
     * @param file the region file to load
     * @return a RegionBuilder populated with data from the file
     * @throws IOException if an error occurs reading the file
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
     * @param filePath the path to the region file
     * @return a RegionBuilder populated with data from the file
     * @throws IOException if an error occurs reading the file
     */
    public static RegionBuilder fromFile(String filePath) throws IOException
    {
        return fromFile(new File(filePath));
    }

    /**
     * Sets the region coordinates.
     *
     * @param x the region X coordinate
     * @param z the region Z coordinate
     * @return this builder instance
     */
    public RegionBuilder withCoordinates(int x, int z)
    {
        this.regionX = x;
        this.regionZ = z;
        return this;
    }

    /**
     * Adds a chunk to the region.
     *
     * @param chunk the chunk to add
     * @return this builder instance
     * @throws IllegalArgumentException if the chunk is invalid or conflicts with existing chunks
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
     * @param chunks the chunks to add
     * @return this builder instance
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
     * Adds an empty chunk at the specified coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return this builder instance
     * @throws IOException if an error occurs creating the empty chunk
     */
    public RegionBuilder addEmptyChunk(int chunkX, int chunkZ) throws IOException
    {
        int index = calculateChunkIndex(chunkX, chunkZ);
        Location location = Location.createEmptyLocation(); // Empty chunk has no data
        Chunk emptyChunk = new Chunk(index, location, 0, new byte[0]);
        return addChunk(emptyChunk);
    }

    /**
     * Removes a chunk from the region.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return this builder instance
     */
    public RegionBuilder removeChunk(int chunkX, int chunkZ)
    {
        int index = calculateChunkIndex(chunkX, chunkZ);
        chunks.remove(index);
        return this;
    }

    /**
     * Removes a chunk from the region by index.
     *
     * @param index the chunk index (0-1023)
     * @return this builder instance
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
     * Enables or disables chunk validation during building.
     *
     * @param validate whether to validate chunks
     * @return this builder instance
     */
    public RegionBuilder validateChunks(boolean validate)
    {
        this.validateChunks = validate;
        return this;
    }

    /**
     * Clears all chunks from the builder.
     *
     * @return this builder instance
     */
    public RegionBuilder clearChunks()
    {
        chunks.clear();
        return this;
    }

    /**
     * Gets the number of chunks currently in the builder.
     *
     * @return the chunk count
     */
    public int getChunkCount()
    {
        return chunks.size();
    }

    /**
     * Checks if the builder has any chunks.
     *
     * @return true if the builder contains chunks
     */
    public boolean hasChunks()
    {
        return !chunks.isEmpty();
    }

    /**
     * Builds the Region object from the configured chunks.
     *
     * @return a new Region instance
     * @throws IOException if an error occurs during region construction
     * @throws IllegalStateException if the builder is in an invalid state
     */
    public IRegion build() throws IOException
    {
        validateBuilder();
        
        // Create a list of all CHUNKS_PER_REGION chunks (empty chunks for missing ones)
        List<IChunk> allChunks = new ArrayList<>(CHUNKS_PER_REGION);
        
        for (int i = 0; i < CHUNKS_PER_REGION; i++)
        {
            IChunk chunk = chunks.get(i);
            if (chunk != null)
            {
                allChunks.add(chunk);
            }
            else
            {
                // Create empty chunk for missing slots
                Location emptyLocation = Location.createEmptyLocation();
                Chunk emptyChunk = new Chunk(i, emptyLocation, 0, new byte[0]);
                allChunks.add(emptyChunk);
            }
        }
        
        return new Region(regionX, regionZ, allChunks);
    }

    /**
     * Calculates the chunk index within a region based on chunk coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk index (0-1023)
     */
    private int calculateChunkIndex(int chunkX, int chunkZ)
    {
        // Convert to region-local coordinates
        int localX = chunkX % CHUNKS_PER_REGION_SIDE; // chunkX % 32
        int localZ = chunkZ % CHUNKS_PER_REGION_SIDE; // chunkZ % 32
        return localZ * CHUNKS_PER_REGION_SIDE + localX;
    }

    /**
     * Validates chunk coordinates against the region boundaries.
     *
     * @param chunk the chunk to validate
     * @throws IllegalArgumentException if the chunk coordinates are invalid
     */
    private void validateChunkCoordinates(IChunk chunk)
    {
        int expectedRegionX = chunk.getX() / CHUNKS_PER_REGION_SIDE; // chunk.getX() / 32
        int expectedRegionZ = chunk.getZ() / CHUNKS_PER_REGION_SIDE; // chunk.getZ() / 32
        
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
     * Validates the builder state before building.
     *
     * @throws IllegalStateException if the builder is in an invalid state
     */
    private void validateBuilder()
    {
        // Basic validation - could be expanded
        if (chunks.size() > CHUNKS_PER_REGION)
        {
            throw new IllegalStateException("Too many chunks: " + chunks.size() + " (maximum " + CHUNKS_PER_REGION + ")");
        }
    }
}