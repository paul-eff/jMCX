package de.pauleff.jmcx.core;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.util.AnvilUtils;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.BLOCKS_PER_CHUNK_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.SECTOR_SIZE_BYTES;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Region class represents a region in the Anvil file format.
 * It handles reading chunk data from a region file.
 */
public class Region implements IRegion
{
    private static final int LOCATION_SIZE = 4;
    private static final int TIMESTAMP_SIZE = 4;

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

    /**
     * Constructs a Region object from a list of chunks.
     * Used by RegionBuilder for creating regions programmatically.
     *
     * @param x      the x-coordinate of the region
     * @param z      the z-coordinate of the region
     * @param chunks the list of chunks (must contain exactly CHUNKS_PER_REGION chunks)
     * @throws IllegalArgumentException if chunks list size is not CHUNKS_PER_REGION
     */
    public Region(int x, int z, List<IChunk> chunks)
    {
        if (chunks.size() != CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException("Chunks list must contain exactly " + CHUNKS_PER_REGION + " chunks, got: " + chunks.size());
        }
        
        this.x = x;
        this.z = z;
        this.raf = null; // No file backing this region
        this.chunks = new ArrayList<>(CHUNKS_PER_REGION);
        
        // Convert IChunk to Chunk instances
        for (IChunk chunk : chunks)
        {
            if (!(chunk instanceof Chunk))
            {
                throw new IllegalArgumentException("All chunks must be instances of de.pauleff.jmcx.core.Chunk");
            }
            this.chunks.add((Chunk) chunk);
        }
    }

    @Override
    public void replaceChunk(IChunk chunk) throws IOException
    {
        if (!(chunk instanceof Chunk concreteChunk))
        {
            throw new IllegalArgumentException("Chunk must be an instance of de.pauleff.jmcx.core.Chunk");
        }
        int offset = 4 * ((concreteChunk.getX() % CHUNKS_PER_REGION_SIDE) + (concreteChunk.getZ() % CHUNKS_PER_REGION_SIDE) * CHUNKS_PER_REGION_SIDE);
        concreteChunk.getLocation().setOffset(offset);
        this.chunks.set(concreteChunk.getIndex(), concreteChunk);
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
     * Gets the list of all CHUNKS_PER_REGION chunks in the region.
     * If a chunk was not generated yet all its values will be 0 or null.
     *
     * @return the list of chunks
     */
    @Override
    public List<IChunk> getChunks()
    {
        return new ArrayList<>(chunks);
    }

    /**
     * Reads all chunks from the region file.
     *
     * @return the list of chunks
     * @throws IOException if an I/O error occurs
     */
    private ArrayList<Chunk> readAllChunks() throws IOException
    {
        int locationsCount = SECTOR_SIZE_BYTES / LOCATION_SIZE;
        int timestampsCount = SECTOR_SIZE_BYTES / TIMESTAMP_SIZE;

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
            raf.seek(SECTOR_SIZE_BYTES + i * TIMESTAMP_SIZE);
            byteBuffer = new byte[4];
            raf.read(byteBuffer);
            timestamps[i] = AnvilUtils.readInt(byteBuffer, ByteOrder.BIG_ENDIAN);
        }

        // Iterate over all locations and read their chunks
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < locationsCount; i++)
        {
            Location currLocation = locations[i];
            raf.seek(currLocation.getOffset() * (long) SECTOR_SIZE_BYTES);
            byte[] chunkData = new byte[currLocation.getSectorCount() * SECTOR_SIZE_BYTES];
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
        int offset = 4 * ((x % CHUNKS_PER_REGION_SIDE) + (z % CHUNKS_PER_REGION_SIDE) * CHUNKS_PER_REGION_SIDE);
        // TODO: Implement full method when needed
        //return new Chunk(offset, readLocation(offset), readTimestamp(offset), readChunkData(offset));
        return null;
    }

    @Override
    public Optional<IChunk> getChunk(int chunkX, int chunkZ)
    {
        for (Chunk chunk : chunks)
        {
            if (chunk.getX() == chunkX && chunk.getZ() == chunkZ)
            {
                return Optional.of(chunk);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<IChunk> getChunksWithOwnables() throws IOException
    {
        List<IChunk> chunksWithOwnables = new ArrayList<>();
        for (Chunk chunk : chunks)
        {
            if (chunk.hasOwnableEntities())
            {
                chunksWithOwnables.add(chunk);
            }
        }
        return chunksWithOwnables;
    }

    @Override
    public boolean chunkInRegion(IChunk chunk)
    {
        if (!(chunk instanceof Chunk concreteChunk))
        {
            return false;
        }
        int[] chunkRegionCoordinates = concreteChunk.chunkToRegionCoordinate();
        return chunkRegionCoordinates[0] == this.x && chunkRegionCoordinates[1] == this.z;
    }

    @Override
    public boolean containsChunk(int chunkX, int chunkZ)
    {
        // Check if chunk coordinates fall within this region's bounds
        int regionStartX = this.x * CHUNKS_PER_REGION_SIDE;
        int regionEndX = regionStartX + CHUNKS_PER_REGION_SIDE - 1;
        int regionStartZ = this.z * CHUNKS_PER_REGION_SIDE;
        int regionEndZ = regionStartZ + CHUNKS_PER_REGION_SIDE - 1;

        return chunkX >= regionStartX && chunkX <= regionEndX &&
                chunkZ >= regionStartZ && chunkZ <= regionEndZ;
    }

    @Override
    public int[] getStartingBlockCoordinates()
    {
        int regionX = x * (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        int regionZ = z * (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        return new int[]{regionX, regionZ};
    }

    @Override
    public int[] getBlockCoordinateRange()
    {
        int startX = x * (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        int endX = startX + (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE - 1);
        int startZ = z * (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE);
        int endZ = startZ + (CHUNKS_PER_REGION_SIDE * BLOCKS_PER_CHUNK_SIDE - 1);
        return new int[]{startX, startZ, endX, endZ};
    }
}