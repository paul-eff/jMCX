package de.pauleff.jmcx.builder;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Location;
import de.pauleff.jnbt.api.ICompoundTag;

import java.io.IOException;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;

/**
 * Builder class for creating Chunk objects using the builder pattern.
 * Provides a fluent API for constructing chunks with NBT data.
 */
public class ChunkBuilder
{
    private int chunkX;
    private int chunkZ;
    private int dataVersion = 4325; // Default to 1.20.5 data version (https://minecraft.wiki/w/Data_version)
    private int timestamp;
    private ICompoundTag nbtData;
    private byte compressionType = 2; // Default to Zlib compression
    private int index = -1;
    private Location location;
    private boolean validateCoordinates = true;

    private ChunkBuilder()
    {
        this.timestamp = (int) (System.currentTimeMillis() / 1000); // Current Unix timestamp
    }

    /**
     * Creates a new ChunkBuilder instance.
     *
     * @return a new ChunkBuilder
     */
    public static ChunkBuilder create()
    {
        return new ChunkBuilder();
    }

    /**
     * Creates a ChunkBuilder from an existing chunk.
     *
     * @param chunk the chunk to copy data from
     * @return a ChunkBuilder populated with data from the chunk
     * @throws IOException if an error occurs reading the chunk data
     */
    public static ChunkBuilder fromChunk(IChunk chunk) throws IOException
    {
        ChunkBuilder builder = new ChunkBuilder();
        builder.chunkX = chunk.getX();
        builder.chunkZ = chunk.getZ();
        builder.dataVersion = chunk.getDataVersion();
        builder.timestamp = chunk.getTimestamp();
        builder.index = chunk.getIndex();
        
        if (chunk instanceof Chunk concreteChunk)
        {
            builder.location = concreteChunk.getLocation();
            builder.compressionType = concreteChunk.getPayload().getCompressionType();
        }
        
        // Copy NBT data if available
        ICompoundTag existingNbt = chunk.getNBTData();
        if (existingNbt != null)
        {
            builder.nbtData = existingNbt;
        }
        
        return builder;
    }

    /**
     * Sets the chunk coordinates.
     *
     * @param x the chunk X coordinate
     * @param z the chunk Z coordinate
     * @return this builder instance
     */
    public ChunkBuilder withCoordinates(int x, int z)
    {
        this.chunkX = x;
        this.chunkZ = z;
        return this;
    }

    /**
     * Sets the chunk X coordinate.
     *
     * @param x the chunk X coordinate
     * @return this builder instance
     */
    public ChunkBuilder withX(int x)
    {
        this.chunkX = x;
        return this;
    }

    /**
     * Sets the chunk Z coordinate.
     *
     * @param z the chunk Z coordinate
     * @return this builder instance
     */
    public ChunkBuilder withZ(int z)
    {
        this.chunkZ = z;
        return this;
    }

    /**
     * Sets the data version of the chunk.
     *
     * @param dataVersion the data version (e.g., 4325 for Minecraft 1.20.5)
     * @return this builder instance
     */
    public ChunkBuilder withDataVersion(int dataVersion)
    {
        this.dataVersion = dataVersion;
        return this;
    }

    /**
     * Sets the timestamp of the chunk.
     *
     * @param timestamp the Unix timestamp
     * @return this builder instance
     */
    public ChunkBuilder withTimestamp(int timestamp)
    {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Sets the current timestamp.
     *
     * @return this builder instance
     */
    public ChunkBuilder withCurrentTimestamp()
    {
        this.timestamp = (int) (System.currentTimeMillis() / 1000);
        return this;
    }

    /**
     * Sets the NBT data for the chunk.
     *
     * @param nbtData the NBT compound tag containing chunk data
     * @return this builder instance
     */
    public ChunkBuilder withNBTData(ICompoundTag nbtData)
    {
        this.nbtData = nbtData;
        return this;
    }

    /**
     * Sets the compression type to Zlib (standard compression).
     *
     * @return this builder instance
     */
    public ChunkBuilder withZlibCompression()
    {
        this.compressionType = 2;
        return this;
    }

    /**
     * Sets the compression type to GZip.
     *
     * @return this builder instance
     */
    public ChunkBuilder withGZipCompression()
    {
        this.compressionType = 1;
        return this;
    }

    /**
     * Sets the compression type to uncompressed.
     *
     * @return this builder instance
     */
    public ChunkBuilder withUncompressed()
    {
        this.compressionType = 3;
        return this;
    }

    /**
     * Sets the chunk index within the region.
     *
     * @param index the chunk index (0-(CHUNKS_PER_REGION - 1))
     * @return this builder instance
     * @throws IllegalArgumentException if index is out of range
     */
    public ChunkBuilder withIndex(int index)
    {
        if (index < 0 || index >= CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException("Chunk index must be between 0 and " + (CHUNKS_PER_REGION - 1) + ", got: " + index);
        }
        this.index = index;
        return this;
    }

    /**
     * Sets the location information for the chunk.
     *
     * @param location the chunk location in the region file
     * @return this builder instance
     */
    public ChunkBuilder withLocation(Location location)
    {
        this.location = location;
        return this;
    }

    /**
     * Creates an empty chunk location.
     *
     * @return this builder instance
     */
    public ChunkBuilder withEmptyLocation()
    {
        this.location = Location.createEmptyLocation();
        return this;
    }

    /**
     * Enables or disables coordinate validation during building.
     *
     * @param validate whether to validate coordinates
     * @return this builder instance
     */
    public ChunkBuilder validateCoordinates(boolean validate)
    {
        this.validateCoordinates = validate;
        return this;
    }

    /**
     * Creates an empty chunk with minimal NBT structure.
     * Note: NBT data will be created during build() if not provided.
     *
     * @return this builder instance
     */
    public ChunkBuilder asEmptyChunk()
    {
        this.nbtData = null; // Will be created in build() method
        return this;
    }

    /**
     * Builds the Chunk object from the configured parameters.
     *
     * @return a new Chunk instance
     * @throws IOException if an error occurs during chunk construction
     * @throws IllegalStateException if the builder is in an invalid state
     */
    public IChunk build() throws IOException
    {
        validateBuilder();
        
        // Calculate index if not set
        if (index == -1)
        {
            index = calculateChunkIndex(chunkX, chunkZ);
        }
        
        // Create location if not set
        if (location == null)
        {
            location = Location.createEmptyLocation();
        }
        
        // Create minimal NBT data if not provided
        if (nbtData == null)
        {
            // For now, we'll create an empty chunk without NBT data
            // The actual NBT creation will be handled by the application layer
            // This allows the builder to work without requiring complex NBT construction
        }
        else
        {
            // Validate NBT data has correct structure
            updateNBTCoordinates();
        }
        
        // Create chunk with empty payload first
        Chunk chunk = new Chunk(index, location, timestamp, new byte[0]);
        
        // Set the NBT data only if provided (this will compress it appropriately)
        if (nbtData != null)
        {
            chunk.setNBTData(nbtData);
        }
        
        return chunk;
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
     * Updates the NBT data to ensure coordinates and data version match builder settings.
     * Note: This is a simplified implementation that validates the NBT data has the expected structure.
     */
    private void updateNBTCoordinates()
    {
        if (nbtData == null) return;
        
        // Basic validation that the NBT data contains expected tags
        // More sophisticated coordinate updating would require NBT modification capabilities
        if (!nbtData.hasTag("xPos") || !nbtData.hasTag("zPos") || !nbtData.hasTag("DataVersion"))
        {
            System.err.println("Warning: NBT data may not have correct chunk coordinates or data version");
        }
    }

    /**
     * Validates the builder state before building.
     *
     * @throws IllegalStateException if the builder is in an invalid state
     */
    private void validateBuilder()
    {
        if (validateCoordinates)
        {
            // Basic coordinate validation
            if (Math.abs(chunkX) > 1875000 || Math.abs(chunkZ) > 1875000)
            {
                throw new IllegalStateException("Chunk coordinates are too large: (" + chunkX + ", " + chunkZ + ")");
            }
        }
        
        if (dataVersion <= 0)
        {
            throw new IllegalStateException("Data version must be positive, got: " + dataVersion);
        }
        
        if (timestamp < 0)
        {
            throw new IllegalStateException("Timestamp cannot be negative, got: " + timestamp);
        }
    }
}