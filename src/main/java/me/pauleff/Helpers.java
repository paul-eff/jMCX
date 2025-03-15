package me.pauleff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Helpers class provides utility anything special needed in this project.
 */
public class Helpers
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

    public static byte[] padToSectorSize(byte[] data, int sectorSize)
    {
        int neededPadding = sectorSize - (data.length % sectorSize);
        byte[] paddedData = new byte[data.length + neededPadding];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        return paddedData;
    }
}