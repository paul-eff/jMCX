package me.pauleff.Anvil;

import me.pauleff.Helpers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * The Region class represents a region in the Anvil file format.
 * It handles reading chunk data from a region file.
 */
public class Region
{
    private final int HEADER_SIZE = 8192;
    private final int LOCATION_TABLE_SIZE = 4096;
    private final int LOCATION_SIZE = 4;
    private final int TIMESTAMP_TABLE_SIZE = 4096;
    private final int TIMESTAMP_SIZE = 4;

    private final RandomAccessFile raf;

    private final int x;
    private final int z;
    private final ArrayList<Chunk> chunks;

    /**
     * Constructs a Region object.
     *
     * @param x         the x-coordinate of the region
     * @param z         the z-coordinate of the region
     * @param anvilFile the RandomAccessFile for the target region file
     * @throws IOException if an I/O error occurs
     */
    public Region(int x, int z, RandomAccessFile anvilFile) throws IOException
    {
        this.x = x;
        this.z = z;
        this.raf = anvilFile;
        this.chunks = readAllChunks();
    }

    public void replaceChunk(Chunk chunk) throws IOException
    {
        int offset = 4 * ((chunk.getX() & 31) + (chunk.getZ() & 31) * 32);
        chunk.getLocation().setOffset(offset);
        this.chunks.set(chunk.getIndex(), chunk);

    }

    /**
     * Gets the x-coordinate of the region.
     *
     * @return the x-coordinate
     */
    public int getX()
    {
        return x;
    }

    /**
     * Gets the z-coordinate of the region.
     *
     * @return the z-coordinate
     */
    public int getZ()
    {
        return z;
    }

    /**
     * Gets the list of all 1024 chunks in the region.
     * If a chunk was not generated yet all its values will be 0 or null.
     *
     * @return the list of chunks
     */
    public ArrayList<Chunk> getChunks()
    {
        return chunks;
    }

    /**
     * Reads all chunks from the region file.
     *
     * @return the list of chunks
     * @throws IOException if an I/O error occurs
     */
    public ArrayList<Chunk> readAllChunks() throws IOException
    {
        int locationsCount = LOCATION_TABLE_SIZE / LOCATION_SIZE;
        int timestampsCount = TIMESTAMP_TABLE_SIZE / TIMESTAMP_SIZE;

        Location[] locations = new Location[locationsCount];
        int[] timestamps = new int[timestampsCount];

        for (int i = 0; i < locationsCount; i++)
        {
            // Read and save location
            raf.seek(i * LOCATION_SIZE);
            byte[] byteBuffer = new byte[4];
            raf.read(byteBuffer);
            locations[i] = new Location(byteBuffer);
            // Read and save the timestamp for the location
            raf.seek(LOCATION_TABLE_SIZE + i * TIMESTAMP_SIZE);
            byteBuffer = new byte[4];
            raf.read(byteBuffer);
            timestamps[i] = Helpers.readInt(byteBuffer, ByteOrder.BIG_ENDIAN);
        }

        // Iterate over all locations and read their chunks
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < locationsCount; i++)
        {
            Location currLocation = locations[i];
            raf.seek(currLocation.getOffset() * 4096L);
            byte[] chunkData = new byte[currLocation.getSectorCount() * 4096];
            raf.read(chunkData);
            chunks.add(new Chunk(i, locations[i], timestamps[i], chunkData));
        }
        return chunks;
    }

    /**
     * WIP!!! Reads a specific chunk from the region file.
     *
     * @param x the x-coordinate of the chunk
     * @param z the z-coordinate of the chunk
     * @return the chunk object
     * @throws IOException if an I/O error occurs
     */
    public Chunk readChunk(int x, int z) throws IOException
    {
        int offset = 4 * ((x & 31) + (z & 31) * 32);
        // TODO: Implement full method when needed
        //return new Chunk(offset, readLocation(offset), readTimestamp(offset), readChunkData(offset));
        return null;
    }

    public Chunk getChunk(int x, int z)
    {
        for (Chunk chunk : chunks)
        {
            if (chunk.getX() == x && chunk.getZ() == z)
            {
                return chunk;
            }
        }
        return null;
    }

    public ArrayList<Chunk> getChunksWithOwnables() throws IOException
    {
        ArrayList<Chunk> chunksWithOwnables = new ArrayList<>();
        for (Chunk chunk : chunks)
        {
            if (chunk.hasOwnableEntities())
            {
                chunksWithOwnables.add(chunk);
            }
        }
        return chunksWithOwnables;
    }

    /**
     * Checks if a chunk is within the region.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is in the region, false otherwise
     */
    public boolean chunkInRegion(Chunk chunk)
    {
        int[] chunkRegionCoordinates = chunk.chunkToRegionCoordinate();
        return chunkRegionCoordinates[0] == this.x && chunkRegionCoordinates[1] == this.z;
    }

    /**
     * Gets the starting block coordinates of the region.
     *
     * @return an array containing the x and z starting block coordinates
     */
    public int[] regionStartingBlockCoordinate()
    {
        int regionX = x * 512;
        int regionZ = z * 512;
        return new int[]{regionX, regionZ};
    }
}