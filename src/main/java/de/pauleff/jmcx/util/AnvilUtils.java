package de.pauleff.jmcx.util;

import de.pauleff.jmcx.core.Location;
import de.pauleff.jmcx.formats.FileFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.pauleff.jmcx.util.AnvilConstants.*;

/**
 * Utility methods for MCA file operations and coordinate conversions.
 *
 * @author Paul Ferlitz
 */
public class AnvilUtils
{
    /**
     * The standard sector size for MCA files (4KiB).
     */
    public static final int SECTOR_SIZE = 4096;

    /**
     * Reads an integer from a byte array with the specified byte order.
     *
     * @param data the byte array to read from
     * @param order the byte order to use (BIG_ENDIAN or LITTLE_ENDIAN)
     * @return the integer value read from the byte array
     */
    public static int readInt(byte[] data, ByteOrder order)
    {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(order);
        buffer.position(4 - data.length);
        buffer.put(data);
        buffer.rewind();
        return buffer.getInt();
    }

    /**
     * Pads data to specified sector size boundary.
     *
     * @param data the data to pad
     * @param sectorSize the sector size
     * @return padded data
     * @throws IllegalArgumentException if sectorSize not positive
     */
    public static byte[] padToSectorSize(byte[] data, int sectorSize)
    {
        if (sectorSize <= 0)
        {
            throw new IllegalArgumentException("Sector size must be positive, got: " + sectorSize);
        }

        if (data == null)
        {
            throw new IllegalArgumentException("Data cannot be null");
        }

        int remainder = data.length % sectorSize;
        if (remainder == 0)
        {
            return data;
        }

        int neededPadding = sectorSize - remainder;
        byte[] paddedData = new byte[data.length + neededPadding];

        System.arraycopy(data, 0, paddedData, 0, data.length);

        return paddedData;
    }

    /**
     * Pads data to standard MCA sector size.
     *
     * @param data the data to pad
     * @return padded data
     */
    public static byte[] padToSectorSize(byte[] data)
    {
        return padToSectorSize(data, SECTOR_SIZE);
    }

    /**
     * Calculates sectors needed for data size.
     *
     * @param dataSize data size in bytes
     * @return number of 4KiB sectors needed
     */
    public static int calculateSectorCount(int dataSize)
    {
        if (dataSize <= 0)
        {
            return 0;
        }
        return (int) Math.ceil((double) dataSize / SECTOR_SIZE);
    }

    /**
     * Validates offset is sector-aligned.
     *
     * @param offset the offset to validate
     * @return true if sector-aligned
     */
    public static boolean isSectorAligned(long offset)
    {
        return offset % SECTOR_SIZE == 0;
    }

    /**
     * Converts block coordinates to chunk coordinates.
     *
     * @param blockX block X coordinate
     * @param blockZ block Z coordinate
     * @return [chunkX, chunkZ] coordinates
     */
    public static int[] blockToChunk(int blockX, int blockZ)
    {
        int chunkX = blockX / BLOCKS_PER_CHUNK_SIDE;
        int chunkZ = blockZ / BLOCKS_PER_CHUNK_SIDE;
        return new int[]{chunkX, chunkZ};
    }

    /**
     * Converts chunk coordinates to region coordinates.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return [regionX, regionZ] coordinates
     */
    public static int[] chunkToRegion(int chunkX, int chunkZ)
    {
        int regionX = chunkX / CHUNKS_PER_REGION_SIDE;
        int regionZ = chunkZ / CHUNKS_PER_REGION_SIDE;
        return new int[]{regionX, regionZ};
    }

    /**
     * Converts block coordinates directly to region coordinates.
     *
     * @param blockX block X coordinate
     * @param blockZ block Z coordinate
     * @return [regionX, regionZ] coordinates
     */
    public static int[] blockToRegion(int blockX, int blockZ)
    {
        int[] chunkCoords = blockToChunk(blockX, blockZ);
        return chunkToRegion(chunkCoords[0], chunkCoords[1]);
    }

    /**
     * Generates region filename from coordinates.
     *
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @return region filename (e.g., "r.0.0.mca")
     */
    public static String generateRegionFilename(int regionX, int regionZ)
    {
        return "r." + regionX + "." + regionZ + "." + FileFormat.ANVIL.getExtension();
    }

    /**
     * Parses region coordinates from filename.
     *
     * @param filename region filename (e.g., "r.0.0.mca")
     * @return [regionX, regionZ] coordinates
     * @throws IllegalArgumentException if filename format invalid
     */
    public static int[] parseRegionFilename(String filename)
    {
        if (filename == null || filename.trim().isEmpty())
        {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        String[] parts = filename.split("\\.");
        if (parts.length != 4 || !"r".equals(parts[0]) || !FileFormat.ANVIL.getExtension().equals(parts[3]))
        {
            throw new IllegalArgumentException(
                    "Invalid region filename format. Expected: r.x.z.mca, got: " + filename
            );
        }

        try
        {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new int[]{x, z};
        } catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                    "Invalid coordinates in filename: " + filename + ". Coordinates must be integers.", e
            );
        }
    }

    /**
     * Validates file is a valid region file.
     *
     * @param file file to validate
     * @return true if valid region file
     */
    public static boolean isValidRegionFile(java.io.File file)
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            return false;
        }

        try
        {
            parseRegionFilename(file.getName());

            return file.length() >= 8192;
        } catch (IllegalArgumentException e)
        {
            return false;
        }
    }


    /**
     * Converts chunk coordinates to region-local index.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return chunk index (0-1023) within region
     */
    public static int chunkCoordinatesToIndex(int chunkX, int chunkZ)
    {
        return (chunkZ % CHUNKS_PER_REGION_SIDE) * CHUNKS_PER_REGION_SIDE + (chunkX % CHUNKS_PER_REGION_SIDE);
    }

    /**
     * Calculates chunk coordinates from region-local index.
     *
     * @param regionX region X coordinate
     * @param regionZ region Z coordinate
     * @param chunkIndex chunk index within region (0-1023)
     * @return [chunkX, chunkZ] global coordinates
     * @throws IllegalArgumentException if index out of range
     */
    public static int[] calculateChunkCoordinates(int regionX, int regionZ, int chunkIndex)
    {
        if (chunkIndex < 0 || chunkIndex >= CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException("Chunk index must be between 0 and " + (CHUNKS_PER_REGION - 1) + ", got: " + chunkIndex);
        }

        int localX = chunkIndex % CHUNKS_PER_REGION_SIDE;
        int localZ = chunkIndex / CHUNKS_PER_REGION_SIDE;

        int globalChunkX = regionX * CHUNKS_PER_REGION_SIDE + localX;
        int globalChunkZ = regionZ * CHUNKS_PER_REGION_SIDE + localZ;

        return new int[]{globalChunkX, globalChunkZ};
    }

    /**
     * Creates {@link Location} object from offset and sector count.
     *
     * @param offset chunk offset in sectors (max 16777215)
     * @param sectorCount number of sectors (max 255)
     * @return new {@link Location} object
     * @throws IllegalArgumentException if values out of range
     */
    public static Location createLocation(int offset, int sectorCount)
    {
        if (offset < 0 || offset > 16777215) // 3 bytes max value
        {
            throw new IllegalArgumentException("Offset must be between 0 and 16777215, got: " + offset);
        }

        if (sectorCount < 0 || sectorCount > 255) // 1 byte max value
        {
            throw new IllegalArgumentException("Sector count must be between 0 and 255, got: " + sectorCount);
        }

        byte[] locationBytes = new byte[4];

        locationBytes[0] = (byte) ((offset >> 16) & 0xFF);
        locationBytes[1] = (byte) ((offset >> 8) & 0xFF);
        locationBytes[2] = (byte) (offset & 0xFF);

        locationBytes[3] = (byte) sectorCount;

        return new Location(locationBytes);
    }

    /**
     * Validates chunk placement for MCA format.
     *
     * @param offset chunk offset in sectors
     * @param sectorCount number of sectors
     * @param fileSize total file size in bytes
     * @return true if placement valid
     */
    public static boolean isValidChunkPlacement(int offset, int sectorCount, long fileSize)
    {
        if (offset < 2) // Must be after header (2 sectors)
        {
            return false;
        }

        if (sectorCount <= 0 || sectorCount > 255)
        {
            return false;
        }

        long chunkStart = (long) offset * SECTOR_SIZE;
        long chunkEnd = chunkStart + (long) sectorCount * SECTOR_SIZE;

        return chunkEnd <= fileSize;
    }
}