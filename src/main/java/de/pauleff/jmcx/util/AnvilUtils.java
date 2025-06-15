package de.pauleff.jmcx.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The AnvilUtils class provides utility methods for MCA file operations.
 */
public class AnvilUtils
{
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
     * The standard sector size for MCA files (4KiB).
     */
    public static final int SECTOR_SIZE = 4096; // 4KiB

    /**
     * Pads data to the specified sector size boundary.
     * According to the Minecraft Wiki, chunks are padded to 4KiB sector boundaries.
     *
     * @param data the data to pad
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
}