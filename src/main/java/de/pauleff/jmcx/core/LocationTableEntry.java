package de.pauleff.jmcx.core;

import de.pauleff.jmcx.util.AnvilUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * The LocationTableEntry class represents a single entry in the MCA file's location table.
 * Each entry contains information about where a chunk's data is located within the file.
 */
public class LocationTableEntry
{
    private final int sectorOffset;
    private final int sectorCount;
    private final boolean isEmpty;

    /**
     * Constructs a LocationTableEntry with the specified offset and sector count.
     *
     * @param sectorOffset the sector offset where the chunk data starts
     * @param sectorCount  the number of sectors the chunk data occupies
     */
    private LocationTableEntry(int sectorOffset, int sectorCount)
    {
        this.sectorOffset = sectorOffset;
        this.sectorCount = sectorCount;
        this.isEmpty = (sectorOffset == 0 && sectorCount == 0);
    }

    /**
     * Creates an empty LocationTableEntry representing a non-existent chunk.
     *
     * @return an empty LocationTableEntry with offset 0 and sector count 0
     */
    public static LocationTableEntry empty()
    {
        return new LocationTableEntry(0, 0);
    }

    /**
     * Creates a LocationTableEntry from a 4-byte array representing the raw location data.
     * <p>
     * The byte array format is:
     * - Bytes 0-2: 24-bit big-endian sector offset
     * - Byte 3: 8-bit sector count
     *
     * @param locationBytes the 4-byte array containing the location data
     * @return a LocationTableEntry parsed from the byte array
     * @throws IllegalArgumentException if the byte array is not exactly 4 bytes long
     */
    public static LocationTableEntry fromBytes(byte[] locationBytes)
    {
        if (locationBytes.length != 4)
        {
            throw new IllegalArgumentException("Location bytes must be exactly 4 bytes, got: " + locationBytes.length);
        }

        // Extract the 24-bit offset from the first 3 bytes
        int sectorOffset = AnvilUtils.readInt(Arrays.copyOfRange(locationBytes, 0, 3), ByteOrder.BIG_ENDIAN);

        // Extract the 8-bit sector count from the last byte
        int sectorCount = AnvilUtils.readInt(Arrays.copyOfRange(locationBytes, 3, 4), ByteOrder.BIG_ENDIAN);

        return new LocationTableEntry(sectorOffset, sectorCount);
    }

    /**
     * Converts this LocationTableEntry to a 4-byte array suitable for writing to an MCA file.
     * <p>
     * The byte array format is:
     * - Bytes 0-2: 24-bit big-endian sector offset
     * - Byte 3: 8-bit sector count
     *
     * @return a 4-byte array representing this location entry
     */
    public byte[] toBytes()
    {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);

        // Write the 24-bit offset as 3 bytes
        buffer.put((byte) ((sectorOffset >> 16) & 0xFF));
        buffer.put((byte) ((sectorOffset >> 8) & 0xFF));
        buffer.put((byte) (sectorOffset & 0xFF));

        // Write the 8-bit sector count
        buffer.put((byte) (sectorCount & 0xFF));

        return buffer.array();
    }

    /**
     * Validates that this LocationTableEntry represents a valid chunk location.
     * <p>
     * A valid location entry must satisfy:
     * - If not empty: sector offset >= 2 (after the 8KiB header)
     * - If not empty: sector count between 1 and 255 (inclusive)
     * - If empty: both offset and sector count must be 0
     *
     * @return true if this location entry is valid, false otherwise
     */
    public boolean isValid()
    {
        if (isEmpty)
        {
            return sectorOffset == 0 && sectorCount == 0;
        }

        // Non-empty chunks must have valid offset and sector count
        return sectorOffset >= 2 && sectorCount >= 1 && sectorCount <= 255;
    }

    /**
     * Calculates the total size in bytes that this chunk occupies in the MCA file.
     * <p>
     * This is calculated as: sector count × sector size (4096 bytes)
     *
     * @return the total size in bytes, or 0 if this is an empty entry
     */
    public int getTotalSizeInBytes()
    {
        if (isEmpty)
        {
            return 0;
        }
        return sectorCount * AnvilUtils.SECTOR_SIZE;
    }

    /**
     * Gets the sector offset where the chunk data starts.
     *
     * @return the sector offset (0 if empty)
     */
    public int getSectorOffset()
    {
        return sectorOffset;
    }

    /**
     * Gets the number of sectors the chunk data occupies.
     *
     * @return the sector count (0 if empty)
     */
    public int getSectorCount()
    {
        return sectorCount;
    }

    /**
     * Checks if this location entry represents an empty/non-existent chunk.
     *
     * @return true if this entry is empty (offset 0, sector count 0)
     */
    public boolean isEmpty()
    {
        return isEmpty;
    }

    /**
     * Calculates the absolute byte offset in the file where this chunk's data starts.
     *
     * @return the absolute byte offset, or 0 if this is an empty entry
     */
    public long getAbsoluteByteOffset()
    {
        if (isEmpty)
        {
            return 0;
        }
        return (long) sectorOffset * AnvilUtils.SECTOR_SIZE;
    }

    /**
     * Calculates the absolute byte offset in the file where this chunk's data ends.
     *
     * @return the absolute byte offset where the chunk ends, or 0 if this is an empty entry
     */
    public long getAbsoluteByteEnd()
    {
        if (isEmpty)
        {
            return 0;
        }
        return getAbsoluteByteOffset() + getTotalSizeInBytes();
    }

    /**
     * Returns a string representation of this LocationTableEntry.
     *
     * @return a string representation containing offset, sector count, and validity status
     */
    @Override
    public String toString()
    {
        if (isEmpty)
        {
            return "LocationTableEntry{empty}";
        }

        return "LocationTableEntry{" +
                "sectorOffset=" + sectorOffset +
                ", sectorCount=" + sectorCount +
                ", byteOffset=" + getAbsoluteByteOffset() +
                ", totalSize=" + getTotalSizeInBytes() +
                ", valid=" + isValid() +
                '}';
    }

    /**
     * Checks if this LocationTableEntry is equal to another object.
     * Two LocationTableEntry objects are considered equal if they have the same
     * sector offset and sector count.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }
        LocationTableEntry that = (LocationTableEntry) obj;
        return sectorOffset == that.sectorOffset && sectorCount == that.sectorCount;
    }

    /**
     * Returns a hash code for this LocationTableEntry.
     * The hash code is based on the sector offset and sector count.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(sectorOffset, sectorCount);
    }
}