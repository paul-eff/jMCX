package de.pauleff.jmcx.core;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jnbt.api.ICompoundTag;
import de.pauleff.jnbt.formats.binary.NBTReader;
import de.pauleff.jnbt.formats.binary.NBTWriter;

import java.io.*;

import static de.pauleff.jmcx.util.AnvilConstants.BLOCKS_PER_CHUNK_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;

/**
 * Implementation of {@link IChunk} representing a chunk in the Anvil file format.
 *
 * @author Paul Ferlitz
 */
public class Chunk implements IChunk
{
    private final int x;
    private final int z;
    private final int dataVersion;
    private final int index;
    private final Location location;
    private final int timestamp;
    private final ChunkPayload payload;
    private ICompoundTag cachedNBTData;
    private boolean nbtLoaded = false;

    /**
     * Constructs a Chunk object.
     *
     * @param index chunk index in region
     * @param location {@link Location} in region file
     * @param timestamp chunk timestamp
     * @param payload byte array representing chunk payload
     * @throws IOException if payload processing fails
     */
    public Chunk(int index, Location location, int timestamp, byte[] payload) throws IOException
    {
        this.index = index;
        this.location = location;
        this.timestamp = timestamp;
        this.payload = new ChunkPayload(payload);

        if (this.payload.getLength() > 0)
        {
            CoordinateData coordData = parseCoordinatesAndVersion();
            this.x = coordData.x;
            this.z = coordData.z;
            this.dataVersion = coordData.dataVersion;
        } else
        {
            this.x = 0;
            this.z = 0;
            this.dataVersion = 0;
        }
    }

    /**
     * Parses coordinates and data version from NBT without caching.
     */
    private CoordinateData parseCoordinatesAndVersion() throws IOException
    {
        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.payload.getDecompressedData()));
             NBTReader reader = new NBTReader(inputStream))
        {
            ICompoundTag root = reader.read();

            int x, z, dataVersion;

            if (root.hasTag("Position"))
            {
                int[] coords = root.getIntArray("Position");
                if (coords.length >= 2)
                {
                    x = coords[0];
                    z = coords[1];
                } else
                {
                    throw new IOException("Invalid entity file format: Position array too short");
                }
            }
            else if (root.hasTag("pos"))
            {
                int[] coords = root.getIntArray("pos");
                if (coords.length >= 3)
                {
                    x = coords[0];
                    z = coords[2];
                } else
                {
                    throw new IOException("Invalid POI file format: pos array too short");
                }
            } else
            {
                if (root.hasTag("xPos") && root.hasTag("zPos"))
                {
                    x = root.getInt("xPos");
                    z = root.getInt("zPos");
                } else
                {
                    throw new IOException("Invalid chunk format: missing xPos/zPos tags");
                }
            }

            if (root.hasTag("DataVersion"))
            {
                dataVersion = root.getInt("DataVersion");
            } else
            {
                throw new IOException("Invalid chunk format: missing DataVersion tag");
            }

            return new CoordinateData(x, z, dataVersion);
        }
    }

    /**
     * Sets new chunk data from raw NBT bytes.
     *
     * @param payload new chunk data as NBT bytes
     * @throws IOException if processing payload fails
     */
    private void setChunkData(byte[] payload) throws IOException
    {
        this.payload.compressAndSetData(payload);
    }

    /**
     * Gets NBT root compound tag with lazy loading.
     *
     * @return {@link ICompoundTag} containing chunk data or null if empty
     * @throws IOException if reading chunk data fails
     */
    public ICompoundTag getNBTData() throws IOException
    {
        if (this.payload.getLength() == 0)
        {
            return null;
        }

        if (!nbtLoaded)
        {
            loadNBTData();
        }

        return cachedNBTData;
    }

    /**
     * Sets new chunk data from NBT compound tag.
     *
     * @param nbtData {@link ICompoundTag} containing chunk data
     * @throws IOException if writing NBT data fails
     */
    public void setNBTData(ICompoundTag nbtData) throws IOException
    {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(byteOutput);
             NBTWriter writer = new NBTWriter(dos))
        {
            writer.write(nbtData);
        }
        byte[] nbtBytes = byteOutput.toByteArray();
        setChunkData(nbtBytes);

        cachedNBTData = nbtData;
        nbtLoaded = true;
    }

    /**
     * Loads and caches NBT data.
     */
    private void loadNBTData() throws IOException
    {
        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.payload.getDecompressedData()));
             NBTReader reader = new NBTReader(inputStream))
        {
            cachedNBTData = reader.read();
            nbtLoaded = true;
        }
    }

    /**
     * Checks if NBT data is loaded into memory.
     *
     * @return true if NBT data loaded
     */
    public boolean isNBTLoaded()
    {
        return nbtLoaded;
    }

    /**
     * Gets chunk location.
     *
     * @return {@link Location} of chunk
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * Gets chunk timestamp.
     *
     * @return timestamp
     */
    public int getTimestamp()
    {
        return timestamp;
    }

    /**
     * Gets chunk payload.
     *
     * @return {@link ChunkPayload}
     */
    public ChunkPayload getPayload()
    {
        return payload;
    }

    /**
     * Gets chunk x-coordinate.
     *
     * @return x-coordinate
     */
    public int getX()
    {
        return x;
    }

    /**
     * Gets chunk z-coordinate.
     *
     * @return z-coordinate
     */
    public int getZ()
    {
        return z;
    }

    /**
     * Gets data version of the chunk.
     *
     * @return data version
     */
    public int getDataVersion()
    {
        return dataVersion;
    }

    /**
     * Gets chunk index.
     *
     * @return chunk index
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Checks if chunk contains entities with Owner or Target tags.
     *
     * @return true if chunk contains ownable entities
     * @throws IOException if reading chunk data fails
     */
    public boolean hasOwnableEntities() throws IOException
    {
        if (this.payload.getLength() == 0)
        {
            return false;
        }

        ICompoundTag root = getNBTData();
        if (root == null)
        {
            return false;
        }

        boolean hasOwner = root.hasTag("Owner");
        boolean hasTarget = root.hasTag("Target");

        return hasOwner || hasTarget;
    }

    /**
     * Converts chunk coordinates to region coordinates.
     *
     * @return [regionX, regionZ] coordinates
     */
    @Override
    public int[] chunkToRegionCoordinate()
    {
        return new int[]{this.x / CHUNKS_PER_REGION_SIDE, this.z / CHUNKS_PER_REGION_SIDE};
    }

    /**
     * Checks if block coordinates fall within this chunk.
     *
     * @param blockX block x-coordinate
     * @param blockZ block z-coordinate
     * @return true if block is in chunk
     */
    @Override
    public boolean isBlockInChunk(int blockX, int blockZ)
    {
        int chunkX = blockX / BLOCKS_PER_CHUNK_SIDE;
        int chunkZ = blockZ / BLOCKS_PER_CHUNK_SIDE;
        return (chunkX == this.x && chunkZ == this.z);
    }

    /**
     * Gets starting block coordinates of chunk.
     *
     * @return [startX, startZ] block coordinates
     */
    @Override
    public int[] getStartingBlockCoordinates()
    {
        int[] regionCoordinate = chunkToRegionCoordinate();
        int chunkX = regionCoordinate[0] + (this.index % CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        int chunkZ = regionCoordinate[1] + (this.index % CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        return new int[]{chunkX, chunkZ};
    }

    @Override
    public int[] getBlockCoordinateRange()
    {
        int startX = this.x * BLOCKS_PER_CHUNK_SIDE;
        int endX = startX + (BLOCKS_PER_CHUNK_SIDE - 1);
        int startZ = this.z * BLOCKS_PER_CHUNK_SIDE;
        int endZ = startZ + (BLOCKS_PER_CHUNK_SIDE - 1);
        return new int[]{startX, startZ, endX, endZ};
    }

    @Override
    public boolean isEmpty()
    {
        return this.payload.getLength() == 0;
    }

    @Override
    public int getDataSize()
    {
        return this.payload.getLength();
    }

    /**
     * Returns string representation of Chunk.
     *
     * @return string representation
     */
    @Override
    public String toString()
    {
        return "Chunk{" +
                "location=" + location +
                ", timestamp=" + timestamp +
                ", chunkData (Bytes)=" + payload.getLength() +
                '}';
    }

    /**
     * Data class holding coordinate and version data.
     */
    private record CoordinateData(int x, int z, int dataVersion)
    {
    }
}