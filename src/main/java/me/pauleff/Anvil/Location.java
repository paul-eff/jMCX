package me.pauleff.Anvil;

import me.pauleff.Helpers;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The Location class represents information to locate a chunk's data in an Anvil file.
 */
public class Location
{
    private final int offset;
    private final int sectorCount;

    /**
     * Constructs a Location object from a byte array.
     *
     * @param locationBytes the byte array representing the location
     * @throws IllegalArgumentException if the byte array length is not 4
     */
    public Location(byte[] locationBytes)
    {
        if (locationBytes.length != 4)
        {
            throw new IllegalArgumentException("Location bytes must be 4 bytes!");
        }
        this.offset = Helpers.readInt(Arrays.copyOf(locationBytes, 3), ByteOrder.BIG_ENDIAN);
        this.sectorCount = Helpers.readInt(Arrays.copyOfRange(locationBytes, 3, 4), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Gets the offset of the location.
     *
     * @return the offset
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Gets the sector count of the location.
     *
     * @return the sector count
     */
    public int getSectorCount()
    {
        return sectorCount;
    }

    /**
     * Returns a string representation of the Location object.
     *
     * @return a string representation of the Location object
     */
    @Override
    public String toString()
    {
        return "Location{" +
                "offset=" + offset +
                ", sectorCount=" + sectorCount +
                '}';
    }
}