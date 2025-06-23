package de.pauleff.jmcx.core;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jnbt.api.ICompoundTag;
import de.pauleff.jnbt.formats.binary.NBTReader;
import de.pauleff.jnbt.formats.binary.NBTWriter;

import java.io.*;

import static de.pauleff.jmcx.util.AnvilConstants.BLOCKS_PER_CHUNK_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;

/**
 * The Chunk class represents a chunk in the Anvil file format.
 * Uses jNBT library for type-safe NBT data handling.
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
     * @param index     the index of the chunk
     * @param location  the location of the chunk in the region file
     * @param timestamp the timestamp of the chunk
     * @param payload   the byte array representing the chunk payload
     * @throws IOException if an I/O error occurs during payload processing
     */
    public Chunk(int index, Location location, int timestamp, byte[] payload) throws IOException
    {
        this.index = index;
        this.location = location;
        this.timestamp = timestamp;
        this.payload = new ChunkPayload(payload);

        // Parse coordinates and data version from NBT data
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
     * Parses coordinates and data version from NBT without caching the full NBT data.
     */
    private CoordinateData parseCoordinatesAndVersion() throws IOException
    {
        try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(this.payload.getDecompressedData()));
             NBTReader reader = new NBTReader(inputStream))
        {
            ICompoundTag root = reader.read();

            int x, z, dataVersion;

            // Check for entity file format (Position tag)
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
            // Check for POI file format (pos tag)
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
                // Standard region file format
                if (root.hasTag("xPos") && root.hasTag("zPos"))
                {
                    x = root.getInt("xPos");
                    z = root.getInt("zPos");
                } else
                {
                    throw new IOException("Invalid chunk format: missing xPos/zPos tags");
                }
            }

            // Get data version
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
     * @param data the new chunk data as NBT bytes
     * @throws IOException if an error occurs processing the data
     */
    private void setChunkData(byte[] data) throws IOException
    {
        this.payload.compressAndSetData(data);
    }

    /**
     * Gets the NBT root compound tag for this chunk with lazy loading.
     * NBT data is only decompressed and parsed when first accessed.
     *
     * @return the root compound tag containing chunk data
     * @throws IOException if an error occurs reading the chunk data
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
     * Sets new chunk data from an NBT compound tag.
     *
     * @param nbtData the NBT compound tag containing chunk data
     * @throws IOException if an error occurs writing the NBT data
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

        // Update cached data
        cachedNBTData = nbtData;
        nbtLoaded = true;
    }

    /**
     * Loads and caches the NBT data.
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
     * Checks if NBT data has been loaded into memory.
     *
     * @return true if NBT data is currently loaded
     */
    public boolean isNBTLoaded()
    {
        return nbtLoaded;
    }

    /**
     * Gets the location of the chunk.
     *
     * @return the location of the chunk
     */
    public Location getLocation()
    {
        return location;
    }

    /**
     * Gets the timestamp of the chunk.
     *
     * @return the timestamp of the chunk
     */
    public int getTimestamp()
    {
        return timestamp;
    }

    /**
     * Gets the payload of the chunk.
     *
     * @return the payload of the chunk
     */
    public ChunkPayload getPayload()
    {
        return payload;
    }

    /**
     * Gets the x-coordinate of the chunk.
     *
     * @return the x-coordinate of the chunk
     */
    public int getX()
    {
        return x;
    }

    /**
     * Gets the z-coordinate of the chunk.
     *
     * @return the z-coordinate of the chunk
     */
    public int getZ()
    {
        return z;
    }

    /**
     * Gets the data version of the chunk.
     *
     * @return the data version of the chunk
     */
    public int getDataVersion()
    {
        return dataVersion;
    }

    /**
     * Gets the index of the chunk.
     *
     * @return the index of the chunk
     */
    public int getIndex()
    {
        return index;
    }

    /**
     * Checks if this chunk contains entities with ownership information.
     * This is useful for UUID conversion in MinecraftOfflineOnlineConverter.
     *
     * @return true if the chunk contains entities with Owner or Target tags
     * @throws IOException if an error occurs reading the chunk data
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

        // Check for Owner tag (tamed animals, etc.)
        boolean hasOwner = root.hasTag("Owner");

        // Check for Target tag (hostile mobs with targets)
        boolean hasTarget = root.hasTag("Target");

        return hasOwner || hasTarget;
    }

    /**
     * Converts the chunk coordinates to region coordinates.
     *
     * @return an array containing the region x and z coordinates
     */
    @Override
    public int[] chunkToRegionCoordinate()
    {
        return new int[]{this.x / CHUNKS_PER_REGION_SIDE, this.z / CHUNKS_PER_REGION_SIDE};
    }

    /**
     * Checks if the given block coordinates fall within this chunk.
     *
     * @param blockX the block x-coordinate
     * @param blockZ the block z-coordinate
     * @return true if the block is in this chunk
     */
    @Override
    public boolean isBlockInChunk(int blockX, int blockZ)
    {
        int chunkX = blockX / BLOCKS_PER_CHUNK_SIDE;
        int chunkZ = blockZ / BLOCKS_PER_CHUNK_SIDE;
        return (chunkX == this.x && chunkZ == this.z);
    }

    /**
     * Gets the starting block coordinates of the chunk.
     *
     * @return an array containing the x and z starting block coordinates
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
     * Returns a string representation of the Chunk object.
     *
     * @return a string representation of the Chunk object
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
     * Simple data class to hold coordinate and version data.
     */
    private static class CoordinateData
    {
        final int x;
        final int z;
        final int dataVersion;

        CoordinateData(int x, int z, int dataVersion)
        {
            this.x = x;
            this.z = z;
            this.dataVersion = dataVersion;
        }
    }
}