package de.pauleff.jmcx.core;

import de.pauleff.jmcx.util.AnvilUtils;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Represents location information to locate chunk data in Anvil file.
 *
 * @author Paul Ferlitz
 */
public class Location
{
    private int offset;
    private int sectorCount;

    /**
     * Constructs a Location from byte array.
     *
     * @param locationBytes byte array representing location (must be 4 bytes)
     * @throws IllegalArgumentException if byte array length not 4
     */
    public Location(byte[] locationBytes)
    {
        if (locationBytes.length != 4)
        {
            throw new IllegalArgumentException("Location bytes must be 4 bytes!");
        }
        this.offset = AnvilUtils.readInt(Arrays.copyOf(locationBytes, 3), ByteOrder.BIG_ENDIAN);
        this.sectorCount = AnvilUtils.readInt(Arrays.copyOfRange(locationBytes, 3, 4), ByteOrder.BIG_ENDIAN);
    }

    public static Location createEmptyLocation()
    {
        return new Location(new byte[]{0, 0, 0, 0});
    }

    /**
     * Gets offset of location.
     *
     * @return offset
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Sets offset of location.
     *
     * @param offset the offset to set
     */
    public void setOffset(int offset)
    {
        this.offset = offset;
    }

    /**
     * Gets sector count of location.
     *
     * @return sector count
     */
    public int getSectorCount()
    {
        return sectorCount;
    }

    /**
     * Sets sector count of location.
     *
     * @param sectorCount the sector count to set
     */
    public void setSectorCount(int sectorCount)
    {
        this.sectorCount = sectorCount;
    }

    /**
     * Returns string representation of Location.
     *
     * @return string representation
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