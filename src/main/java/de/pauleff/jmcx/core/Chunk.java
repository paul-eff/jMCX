package de.pauleff.jmcx.core;

import de.pauleff.jnbt.api.ICompoundTag;
import de.pauleff.jnbt.formats.binary.NBTReader;
import de.pauleff.jnbt.formats.binary.NBTWriter;
import de.pauleff.jnbt.formats.binary.Compression_Types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The Chunk class represents a chunk in the Anvil file format.
 * Uses jNBT library for type-safe NBT data handling.
 */
public class Chunk
{
    private final int x;
    private final int z;
    private final int dataVersion;
    private final int index;
    private final Location location;
    private final int timestamp;
    private ChunkPayload payload;

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
        // If there is an actual payload to process
        if (this.payload.getLength() > 0)
        {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(this.payload.getDecompressedData());
                 NBTReader reader = new NBTReader(inputStream))
            {
                ICompoundTag root = reader.read();
                
                // Check for entity file format (Position tag)
                if (root.hasTag("Position"))
                {
                    // If Position is found, we are dealing with an entity file
                    int[] coords = root.getIntArray("Position");
                    if (coords.length >= 2)
                    {
                        this.x = coords[0];
                        this.z = coords[1];
                    }
                    else
                    {
                        throw new IOException("Invalid entity file format: Position array too short");
                    }
                }
                // Check for POI file format (pos tag)
                else if (root.hasTag("pos"))
                {
                    // If pos is found, we are dealing with a poi file
                    int[] coords = root.getIntArray("pos");
                    if (coords.length >= 3)
                    {
                        this.x = coords[0];
                        this.z = coords[2];
                    }
                    else
                    {
                        throw new IOException("Invalid POI file format: pos array too short");
                    }
                }
                else
                {
                    // Standard region file format
                    if (root.hasTag("xPos") && root.hasTag("zPos"))
                    {
                        this.x = root.getInt("xPos");
                        this.z = root.getInt("zPos");
                    }
                    else
                    {
                        throw new IOException("Invalid chunk format: missing xPos/zPos tags");
                    }
                }
                
                // Get data version
                if (root.hasTag("DataVersion"))
                {
                    this.dataVersion = root.getInt("DataVersion");
                }
                else
                {
                    throw new IOException("Invalid chunk format: missing DataVersion tag");
                }
            }
        }
        else
        {
            this.x = 0;
            this.z = 0;
            this.dataVersion = 0;
        }
    }

    /**
     * Sets new chunk data from raw NBT bytes.
     *
     * @param data the new chunk data as NBT bytes
     * @throws IOException if an error occurs processing the data
     */
    public void setChunkData(byte[] data) throws IOException
    {
        ChunkPayload payload = this.getPayload();
        payload.compressAndSetData(data);
        this.payload = payload;
    }

    /**
     * Gets the NBT root compound tag for this chunk.
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
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(this.payload.getDecompressedData());
             NBTReader reader = new NBTReader(inputStream))
        {
            return reader.read();
        }
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
        try (NBTWriter writer = new NBTWriter(byteOutput, Compression_Types.NONE))
        {
            writer.write(nbtData);
        }
        byte[] nbtBytes = byteOutput.toByteArray();
        setChunkData(nbtBytes);
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
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(this.getPayload().getDecompressedData());
             NBTReader reader = new NBTReader(inputStream))
        {
            ICompoundTag root = reader.read();
            
            // Check for Owner tag (tamed animals, etc.)
            boolean hasOwner = root.hasTag("Owner");
            
            // Check for Target tag (hostile mobs with targets)
            boolean hasTarget = root.hasTag("Target");
            
            return hasOwner || hasTarget;
        }
    }

    /**
     * Converts the chunk coordinates to region coordinates.
     *
     * @return an array containing the region x and z coordinates
     */
    public int[] chunkToRegionCoordinate()
    {
        int regionX = (int) Math.floor(this.x / 32.0f);
        int regionZ = (int) Math.floor(this.z / 32.0f);
        return new int[]{regionX, regionZ};
    }

    public boolean isBlockInChunk(int blockX, int blockZ)
    {
        int chunkX = (int) Math.floor(blockX / 16.0);
        int chunkZ = (int) Math.floor(blockZ / 16.0);

        return (chunkX == this.x && chunkZ == this.z);
    }

    /**
     * Gets the starting block coordinates of the chunk.
     *
     * @return an array containing the x and z starting block coordinates
     */
    public int[] chunkStartingBlockCoordinate()
    {
        int[] regionCoordinate = chunkToRegionCoordinate();
        int chunkX = regionCoordinate[0] + (this.index % 32 * 16);
        int chunkZ = regionCoordinate[1] + (this.index % 32 * 16);
        return new int[]{chunkX, chunkZ};
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
}