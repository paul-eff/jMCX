package de.pauleff.jmcx.builder;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.ChunkPayload;
import de.pauleff.jmcx.core.Location;
import de.pauleff.jnbt.api.ICompoundTag;
import de.pauleff.jnbt.formats.binary.NBTWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;

/**
 * Builder class for creating Chunk objects using the builder pattern.
 * Provides a fluent API for constructing chunks with NBT data.
 */
public class ChunkBuilder
{
    private int chunkX;
    private int chunkZ;
    private boolean xExplicitlySet = false;
    private boolean zExplicitlySet = false;
    private int dataVersion = 4325; // Default to 1.21.5 data version (https://minecraft.wiki/w/Data_version)
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
        builder.xExplicitlySet = true;
        builder.zExplicitlySet = true;
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
        } else
        {
            // If source chunk has no NBT data, we'll create a minimal chunk
            System.out.println("Warning: Source chunk has no NBT data, will create minimal chunk structure");
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
        this.xExplicitlySet = true;
        this.zExplicitlySet = true;
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
        this.xExplicitlySet = true;
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
        this.zExplicitlySet = true;
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
     * @throws IOException           if an error occurs during chunk construction
     * @throws IllegalStateException if the builder is in an invalid state
     */
    public IChunk build() throws IOException
    {
        validateBuilder();

        // Calculate index if not set - use region-local coordinates
        if (index == -1)
        {
            index = de.pauleff.jmcx.util.AnvilUtils.chunkCoordinatesToIndex(chunkX, chunkZ);
        }

        // Validate that the calculated index is consistent with coordinates
        int expectedIndex = de.pauleff.jmcx.util.AnvilUtils.chunkCoordinatesToIndex(chunkX, chunkZ);
        if (index != expectedIndex)
        {
            // Update index to match coordinates to ensure consistency
            index = expectedIndex;
        }

        // Create location if not set
        if (location == null)
        {
            location = Location.createEmptyLocation();
        }

        // Create minimal NBT data if not provided
        if (nbtData == null)
        {
            // Create chunk with empty payload
            Chunk chunk = new Chunk(index, location, timestamp, new byte[0]);
            return chunk;
        } else
        {
            // Update NBT coordinates to match builder settings
            updateNBTCoordinates();

            // Create NBT payload before constructing chunk so coordinates are read correctly
            byte[] nbtPayload = createNBTPayload();

            // Calculate sector count needed for the payload and update location
            int sectorCount = de.pauleff.jmcx.util.AnvilUtils.calculateSectorCount(nbtPayload.length);
            location = de.pauleff.jmcx.util.AnvilUtils.createLocation(0, sectorCount); // offset will be set by region when writing

            // Create chunk with NBT payload so constructor can read coordinates
            Chunk chunk = new Chunk(index, location, timestamp, nbtPayload);
            return chunk;
        }
    }

    /**
     * Creates a compressed NBT payload from the NBT data.
     * Uses the same format as Chunk.setNBTData() and ChunkPayload.
     *
     * @return compressed NBT payload as byte array
     * @throws IOException if NBT serialization fails
     */
    private byte[] createNBTPayload() throws IOException
    {
        if (nbtData == null)
        {
            return new byte[0];
        }

        // Convert NBT data to raw bytes using NBTWriter (same as Chunk.setNBTData())
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(byteOutput);
             NBTWriter writer = new NBTWriter(dos))
        {
            writer.write(nbtData);
        }
        byte[] nbtBytes = byteOutput.toByteArray();

        // Compress the NBT data using ChunkPayload's compression logic
        ChunkPayload tempPayload = new ChunkPayload(new byte[0]); // Create temporary payload for compression
        byte[] compressedData = tempPayload.compressData(nbtBytes, compressionType);

        // Create the final payload with proper format: [4-byte length][1-byte compression type][compressed data]
        // Length field should contain only the compressed data length (excluding compression type byte)
        ByteBuffer buffer = ByteBuffer.allocate(5 + compressedData.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(compressedData.length); // Only compressed data length
        buffer.put(compressionType);
        buffer.put(compressedData);

        return buffer.array();
    }


    /**
     * Updates the NBT data to ensure coordinates match builder settings.
     * Priority: builder coordinates > NBT coordinates > defaults (0,0)
     */
    private void updateNBTCoordinates()
    {
        if (nbtData == null) return;

        // Determine final coordinates based on priority for each axis independently:
        // 1. If X/Z was explicitly set via builder methods, use builder value
        // 2. Otherwise, if NBT has X/Z coordinates, use those and update builder state
        // 3. Otherwise, use builder defaults (0,0)

        int finalX = chunkX;
        int finalZ = chunkZ;

        // Handle X coordinate
        if (!xExplicitlySet && nbtData.hasTag("xPos"))
        {
            finalX = nbtData.getInt("xPos");
            this.chunkX = finalX; // Update builder state
        }

        // Handle Z coordinate
        if (!zExplicitlySet && nbtData.hasTag("zPos"))
        {
            finalZ = nbtData.getInt("zPos");
            this.chunkZ = finalZ; // Update builder state
        }

        // Always ensure NBT has the final coordinates
        nbtData.setInt("xPos", finalX);
        nbtData.setInt("zPos", finalZ);
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
            // If data version is 0 or negative, set it to a reasonable default
            // This handles empty/corrupt chunks that don't have valid NBT data
            System.out.println("Warning: Invalid data version (" + dataVersion + "), setting to default 4325");
            this.dataVersion = 4325; // Default to 1.21.5 data version
        }

        if (timestamp < 0)
        {
            throw new IllegalStateException("Timestamp cannot be negative, got: " + timestamp);
        }
    }
}