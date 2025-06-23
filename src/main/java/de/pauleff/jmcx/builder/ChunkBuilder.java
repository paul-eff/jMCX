package de.pauleff.jmcx.builder;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.ChunkPayload;
import de.pauleff.jmcx.core.Location;
import de.pauleff.jmcx.util.AnvilUtils;
import de.pauleff.jnbt.api.ICompoundTag;
import de.pauleff.jnbt.formats.binary.NBTWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;

/**
 * Builder class for creating {@link IChunk} objects using the builder pattern.
 *
 * @author Paul Ferlitz
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
        this.timestamp = (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Creates a new ChunkBuilder instance.
     *
     * @return new {@link ChunkBuilder}
     */
    public static ChunkBuilder create()
    {
        return new ChunkBuilder();
    }

    /**
     * Creates a ChunkBuilder from an existing chunk.
     *
     * @param chunk {@link IChunk} to copy data from
     * @return ChunkBuilder populated with chunk data
     * @throws IOException if reading chunk data fails
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

        ICompoundTag existingNbt = chunk.getNBTData();
        if (existingNbt != null)
        {
            builder.nbtData = existingNbt;
        } else
        {
            System.out.println("Warning: Source chunk has no NBT data, will create minimal chunk structure");
        }

        return builder;
    }

    /**
     * Sets the chunk coordinates.
     *
     * @param x chunk X coordinate
     * @param z chunk Z coordinate
     * @return this builder
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
     * Sets chunk X coordinate.
     *
     * @param x chunk X coordinate
     * @return this builder
     */
    public ChunkBuilder withX(int x)
    {
        this.chunkX = x;
        this.xExplicitlySet = true;
        return this;
    }

    /**
     * Sets chunk Z coordinate.
     *
     * @param z chunk Z coordinate
     * @return this builder
     */
    public ChunkBuilder withZ(int z)
    {
        this.chunkZ = z;
        this.zExplicitlySet = true;
        return this;
    }

    /**
     * Sets data version of the chunk.
     *
     * @param dataVersion data version (e.g., 4325 for Minecraft 1.21.5)
     * @return this builder
     */
    public ChunkBuilder withDataVersion(int dataVersion)
    {
        this.dataVersion = dataVersion;
        return this;
    }

    /**
     * Sets timestamp of the chunk.
     *
     * @param timestamp Unix timestamp
     * @return this builder
     */
    public ChunkBuilder withTimestamp(int timestamp)
    {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Sets current timestamp.
     *
     * @return this builder
     */
    public ChunkBuilder withCurrentTimestamp()
    {
        this.timestamp = (int) (System.currentTimeMillis() / 1000);
        return this;
    }

    /**
     * Sets NBT data for the chunk.
     *
     * @param nbtData {@link ICompoundTag} containing chunk data
     * @return this builder
     */
    public ChunkBuilder withNBTData(ICompoundTag nbtData)
    {
        this.nbtData = nbtData;
        return this;
    }

    /**
     * Sets compression type to Zlib.
     *
     * @return this builder
     */
    public ChunkBuilder withZlibCompression()
    {
        this.compressionType = 2;
        return this;
    }

    /**
     * Sets compression type to GZip.
     *
     * @return this builder
     */
    public ChunkBuilder withGZipCompression()
    {
        this.compressionType = 1;
        return this;
    }

    /**
     * Sets compression type to uncompressed.
     *
     * @return this builder
     */
    public ChunkBuilder withUncompressed()
    {
        this.compressionType = 3;
        return this;
    }

    /**
     * Sets chunk index within the region.
     *
     * @param index chunk index (0-1023)
     * @return this builder
     * @throws IllegalArgumentException if index out of range
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
     * Sets location information for the chunk.
     *
     * @param location chunk {@link Location} in region file
     * @return this builder
     */
    public ChunkBuilder withLocation(Location location)
    {
        this.location = location;
        return this;
    }

    /**
     * Creates empty chunk location.
     *
     * @return this builder
     */
    public ChunkBuilder withEmptyLocation()
    {
        this.location = Location.createEmptyLocation();
        return this;
    }

    /**
     * Enables or disables coordinate validation.
     *
     * @param validate whether to validate coordinates
     * @return this builder
     */
    public ChunkBuilder validateCoordinates(boolean validate)
    {
        this.validateCoordinates = validate;
        return this;
    }

    /**
     * Creates empty chunk with minimal NBT structure.
     *
     * @return this builder
     */
    public ChunkBuilder asEmptyChunk()
    {
        this.nbtData = null;
        return this;
    }

    /**
     * Builds the {@link IChunk} from configured parameters.
     *
     * @return new {@link IChunk} instance
     * @throws IOException if chunk construction fails
     * @throws IllegalStateException if builder state invalid
     */
    public IChunk build() throws IOException
    {
        validateBuilder();

        if (this.index == -1)
        {
            this.index = AnvilUtils.chunkCoordinatesToIndex(this.chunkX, this.chunkZ);
        }

        int expectedIndex = AnvilUtils.chunkCoordinatesToIndex(this.chunkX, this.chunkZ);
        if (this.index != expectedIndex)
        {
            this.index = expectedIndex;
        }

        if (this.location == null)
        {
            this.location = Location.createEmptyLocation();
        }

        if (this.nbtData == null)
        {
            return new Chunk(this.index, this.location, this.timestamp, new byte[0]);
        } else
        {
            updateNBTCoordinates();
            byte[] nbtPayload = createNBTPayload();
            int sectorCount = AnvilUtils.calculateSectorCount(nbtPayload.length);
            this.location = AnvilUtils.createLocation(0, sectorCount);
            return new Chunk(this.index, this.location, this.timestamp, nbtPayload);
        }
    }

    /**
     * Creates compressed NBT payload from NBT data.
     *
     * @return compressed NBT payload
     * @throws IOException if NBT serialization fails
     */
    private byte[] createNBTPayload() throws IOException
    {
        if (nbtData == null)
        {
            return new byte[0];
        }

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(byteOutput);
             NBTWriter writer = new NBTWriter(dos))
        {
            writer.write(nbtData);
        }
        byte[] nbtBytes = byteOutput.toByteArray();

        ChunkPayload tempPayload = new ChunkPayload(new byte[0]);
        byte[] compressedData = tempPayload.compressData(nbtBytes, compressionType);

        ByteBuffer buffer = ByteBuffer.allocate(5 + compressedData.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(compressedData.length);
        buffer.put(compressionType);
        buffer.put(compressedData);

        return buffer.array();
    }


    /**
     * Updates NBT coordinates to match builder settings.
     */
    private void updateNBTCoordinates()
    {
        if (nbtData == null) return;

        int finalX = chunkX;
        int finalZ = chunkZ;

        if (!xExplicitlySet && nbtData.hasTag("xPos"))
        {
            finalX = nbtData.getInt("xPos");
            this.chunkX = finalX;
        }

        if (!zExplicitlySet && nbtData.hasTag("zPos"))
        {
            finalZ = nbtData.getInt("zPos");
            this.chunkZ = finalZ;
        }

        nbtData.setInt("xPos", finalX);
        nbtData.setInt("zPos", finalZ);
    }

    /**
     * Validates builder state before building.
     *
     * @throws IllegalStateException if builder state invalid
     */
    private void validateBuilder()
    {
        if (validateCoordinates)
        {
            if (Math.abs(chunkX) > 1875000 || Math.abs(chunkZ) > 1875000)
            {
                throw new IllegalStateException("Chunk coordinates are too large: (" + chunkX + ", " + chunkZ + ")");
            }
        }

        if (dataVersion <= 0)
        {
            System.out.println("Warning: Invalid data version (" + dataVersion + "), setting to default 4325");
            this.dataVersion = 4325;
        }

        if (timestamp < 0)
        {
            throw new IllegalStateException("Timestamp cannot be negative, got: " + timestamp);
        }
    }
}