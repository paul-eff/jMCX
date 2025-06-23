package de.pauleff.jmcx.util;

import de.pauleff.jmcx.core.Location;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.BLOCKS_PER_CHUNK_SIDE;
import static de.pauleff.jmcx.util.AnvilConstants.MCA_EXTENSION;

/**
 * The AnvilUtils class provides utility methods for MCA file operations.
 */
public class AnvilUtils
{
    /**
     * The standard sector size for MCA files (4KiB).
     */
    public static final int SECTOR_SIZE = 4096; // 4KiB

    /**
     * Reads an integer from a byte array with the specified byte order.
     *
     * @param bytes the byte array to read from
     * @param order the byte order to use (BIG_ENDIAN or LITTLE_ENDIAN)
     * @return the integer value read from the byte array
     */
    public static int readInt(byte[] bytes, ByteOrder order)
    {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(order);
        buffer.position(4 - bytes.length);  // Align to the right for smaller sizes
        buffer.put(bytes);
        buffer.rewind();
        return buffer.getInt();
    }

    /**
     * Pads data to the specified sector size boundary.
     * According to the Minecraft Wiki, chunks are padded to 4KiB sector boundaries.
     *
     * @param data       the data to pad
     * @param sectorSize the sector size to pad to (should be 4096 for MCA files)
     * @return the padded data
     * @throws IllegalArgumentException if sectorSize is not positive
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

        // If data is already aligned, no padding needed
        int remainder = data.length % sectorSize;
        if (remainder == 0)
        {
            return data;
        }

        // Calculate padding needed
        int neededPadding = sectorSize - remainder;
        byte[] paddedData = new byte[data.length + neededPadding];

        // Copy original data
        System.arraycopy(data, 0, paddedData, 0, data.length);

        // Padding bytes are automatically zero-initialized
        return paddedData;
    }

    /**
     * Pads data to the standard MCA sector size (4KiB).
     *
     * @param data the data to pad
     * @return the padded data
     */
    public static byte[] padToSectorSize(byte[] data)
    {
        return padToSectorSize(data, SECTOR_SIZE);
    }

    /**
     * Calculates the number of sectors needed for the given data size.
     *
     * @param dataSize the size of the data in bytes
     * @return the number of 4KiB sectors needed
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
     * Validates that an offset is properly aligned to sector boundaries.
     *
     * @param offset the offset to validate
     * @return true if the offset is sector-aligned
     */
    public static boolean isSectorAligned(long offset)
    {
        return offset % SECTOR_SIZE == 0;
    }

    /**
     * Converts block coordinates to chunk coordinates.
     *
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return an array containing chunk X and Z coordinates
     */
    public static int[] blockToChunk(int blockX, int blockZ)
    {
        int chunkX = blockX / BLOCKS_PER_CHUNK_SIDE; // blockX / 16
        int chunkZ = blockZ / BLOCKS_PER_CHUNK_SIDE; // blockZ / 16
        return new int[]{chunkX, chunkZ};
    }

    /**
     * Converts chunk coordinates to region coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return an array containing region X and Z coordinates
     */
    public static int[] chunkToRegion(int chunkX, int chunkZ)
    {
        int regionX = chunkX / CHUNKS_PER_REGION_SIDE; // chunkX / 32
        int regionZ = chunkZ / CHUNKS_PER_REGION_SIDE; // chunkZ / 32
        return new int[]{regionX, regionZ};
    }

    /**
     * Converts block coordinates directly to region coordinates.
     *
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return an array containing region X and Z coordinates
     */
    public static int[] blockToRegion(int blockX, int blockZ)
    {
        int[] chunkCoords = blockToChunk(blockX, blockZ);
        return chunkToRegion(chunkCoords[0], chunkCoords[1]);
    }

    /**
     * Generates a region filename from region coordinates.
     *
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @return the region filename (e.g., "r.0.0.mca")
     */
    public static String generateRegionFilename(int regionX, int regionZ)
    {
        return "r." + regionX + "." + regionZ + MCA_EXTENSION;
    }

    /**
     * Parses region coordinates from a region filename.
     *
     * @param filename the region filename (e.g., "r.0.0.mca")
     * @return an array containing region X and Z coordinates
     * @throws IllegalArgumentException if the filename format is invalid
     */
    public static int[] parseRegionFilename(String filename)
    {
        if (filename == null || filename.trim().isEmpty())
        {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        String[] parts = filename.split("\\.");
        if (parts.length != 4 || !"r".equals(parts[0]) || !MCA_EXTENSION.substring(1).equals(parts[3]))
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
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                    "Invalid coordinates in filename: " + filename + ". Coordinates must be integers.", e
            );
        }
    }

    /**
     * Validates that a file is a valid region file based on its name and basic properties.
     *
     * @param file the file to validate
     * @return true if the file appears to be a valid region file
     */
    public static boolean isValidRegionFile(java.io.File file)
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            return false;
        }

        try
        {
            // Validate filename format
            parseRegionFilename(file.getName());
            
            // Basic size check - must be at least header size
            return file.length() >= 8192; // 8KiB header minimum
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    /**
     * Calculates the chunk index within a region from chunk coordinates.
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk index (0-1023) within the region
     */
    public static int calculateChunkIndex(int chunkX, int chunkZ)
    {
        // Convert to region-local coordinates and calculate index
        return (chunkZ % CHUNKS_PER_REGION_SIDE) * CHUNKS_PER_REGION_SIDE + (chunkX % CHUNKS_PER_REGION_SIDE);
    }

    /**
     * Standardized method for converting chunk coordinates to region-local index.
     * Uses the formula: (z % 32) * 32 + (x % 32)
     *
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk index (0-1023) within the region
     */
    public static int chunkCoordinatesToIndex(int chunkX, int chunkZ)
    {
        return (chunkZ % CHUNKS_PER_REGION_SIDE) * CHUNKS_PER_REGION_SIDE + (chunkX % CHUNKS_PER_REGION_SIDE);
    }

    /**
     * Calculates chunk coordinates from a region-local chunk index.
     *
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @param chunkIndex the chunk index within the region (0-1023)
     * @return an array containing global chunk X and Z coordinates
     * @throws IllegalArgumentException if chunk index is out of range
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
     * Creates a Location object from offset and sector count values.
     * Encodes the values into the 4-byte format expected by the Location constructor.
     *
     * @param offset the chunk offset in sectors (3 bytes, max value 16777215)
     * @param sectorCount the number of sectors (1 byte, max value 255)
     * @return a new Location object
     * @throws IllegalArgumentException if values are out of range
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
        
        // Encode into 4-byte array: [offset (3 bytes, big-endian)][sectorCount (1 byte)]
        byte[] locationBytes = new byte[4];
        
        // Write offset as 3 bytes (big-endian)
        locationBytes[0] = (byte) ((offset >> 16) & 0xFF);
        locationBytes[1] = (byte) ((offset >> 8) & 0xFF);
        locationBytes[2] = (byte) (offset & 0xFF);
        
        // Write sector count as 1 byte
        locationBytes[3] = (byte) sectorCount;
        
        return new Location(locationBytes);
    }

    /**
     * Validates sector alignment and size constraints for MCA format.
     *
     * @param offset the chunk offset in sectors
     * @param sectorCount the number of sectors
     * @param fileSize the total file size in bytes
     * @return true if the chunk placement is valid
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