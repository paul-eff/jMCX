package de.pauleff.jmcx.core;

import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.util.AnvilUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.pauleff.jmcx.util.AnvilConstants.*;

/**
 * Implementation of {@link IRegion} representing a region in the Anvil file format.
 *
 * @author Paul Ferlitz
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
     * @param x region x-coordinate
     * @param z region z-coordinate
     * @param anvilFile RandomAccessFile for target region file
     * @throws IOException if I/O error occurs
     */
    public Region(int x, int z, RandomAccessFile anvilFile) throws IOException
    {
        this.x = x;
        this.z = z;
        this.raf = anvilFile;
        this.chunks = readAllChunks();
    }

    /**
     * Constructs a Region object from chunk list.
     *
     * @param x region x-coordinate
     * @param z region z-coordinate
     * @param chunks list of {@link IChunk} (must contain exactly CHUNKS_PER_REGION chunks)
     * @throws IllegalArgumentException if chunks list size not CHUNKS_PER_REGION
     */
    public Region(int x, int z, List<IChunk> chunks)
    {
        if (chunks.size() != CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException("Chunks list must contain exactly " + CHUNKS_PER_REGION + " chunks, got: " + chunks.size());
        }

        this.x = x;
        this.z = z;
        this.raf = null;
        this.chunks = new ArrayList<>(CHUNKS_PER_REGION);

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

        int targetIndex = AnvilUtils.chunkCoordinatesToIndex(concreteChunk.getX(), concreteChunk.getZ());

        if (!containsChunk(concreteChunk.getX(), concreteChunk.getZ()))
        {
            throw new IllegalArgumentException(
                    String.format("Chunk at (%d, %d) does not belong to region (%d, %d)",
                            concreteChunk.getX(), concreteChunk.getZ(), this.x, this.z));
        }
        this.chunks.set(targetIndex, concreteChunk);
    }

    /**
     * Gets region x-coordinate.
     *
     * @return x-coordinate
     */
    public int getX()
    {
        return x;
    }

    /**
     * Gets region z-coordinate.
     *
     * @return z-coordinate
     */
    public int getZ()
    {
        return z;
    }

    /**
     * Gets list of all chunks in region.
     *
     * @return list of {@link IChunk} objects
     */
    @Override
    public List<IChunk> getChunks()
    {
        return new ArrayList<>(chunks);
    }

    /**
     * Reads all chunks from region file.
     *
     * @return list of chunks
     * @throws IOException if I/O error occurs
     */
    private ArrayList<Chunk> readAllChunks() throws IOException
    {
        int locationCount = SECTOR_SIZE_BYTES / LOCATION_SIZE;
        int timestampCount = SECTOR_SIZE_BYTES / TIMESTAMP_SIZE;

        Location[] locations = new Location[locationCount];
        int[] timestamps = new int[timestampCount];

        for (int i = 0; i < locationCount; i++)
        {
            raf.seek(i * LOCATION_SIZE);
            byte[] byteBuffer = new byte[4];
            raf.read(byteBuffer);
            locations[i] = new Location(byteBuffer);
            raf.seek(SECTOR_SIZE_BYTES + i * TIMESTAMP_SIZE);
            byteBuffer = new byte[4];
            raf.read(byteBuffer);
            timestamps[i] = AnvilUtils.readInt(byteBuffer, ByteOrder.BIG_ENDIAN);
        }

        ArrayList<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < locationCount; i++)
        {
            Location currLocation = locations[i];
            if (currLocation.getOffset() == 0 && currLocation.getSectorCount() == 0)
            {
                chunks.add(new Chunk(i, locations[i], timestamps[i], new byte[0]));
            } else
            {
                raf.seek(currLocation.getOffset() * (long) SECTOR_SIZE_BYTES);
                byte[] chunkData = new byte[currLocation.getSectorCount() * SECTOR_SIZE_BYTES];
                raf.read(chunkData);
                chunks.add(new Chunk(i, locations[i], timestamps[i], chunkData));
            }
        }
        return chunks;
    }

    @Override
    public Optional<IChunk> getChunk(int chunkX, int chunkZ)
    {
        if (!containsChunk(chunkX, chunkZ))
        {
            return Optional.empty();
        }

        int index = AnvilUtils.chunkCoordinatesToIndex(chunkX, chunkZ);
        if (index >= 0 && index < chunks.size())
        {
            Chunk chunk = chunks.get(index);
            if (chunk != null && !chunk.isEmpty())
            {
                if (chunk.getX() == chunkX && chunk.getZ() == chunkZ)
                {
                    return Optional.of(chunk);
                } else
                {
                    System.err.printf("WARNING: Retrieved chunk position [%d, %d] does not match requested [%d, %d] at index %d%n",
                            chunk.getX(), chunk.getZ(), chunkX, chunkZ, index);
                    return Optional.empty();
                }
            } else if (chunk != null)
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