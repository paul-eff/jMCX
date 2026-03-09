package de.pauleff.jmcx.util;

/**
 * Constants used throughout the jMCX library for Anvil format operations.
 *
 * @author Paul Ferlitz
 */
public final class AnvilConstants
{
    public static final int SECTOR_SIZE_BYTES = 4096;
    public static final int MAX_CHUNK_SIZE_BYTES = 1048576;
    public static final int CHUNKS_PER_REGION = 1024;
    public static final int CHUNKS_PER_REGION_SIDE = 32;
    public static final int CHUNKS_PER_REGION_SIDE_SHIFT = 5;
    public static final int BLOCKS_PER_CHUNK_SIDE = 16;
    public static final int BLOCKS_PER_CHUNK_SIDE_SHIFT = 4;

    public static final String MAX_CHUNK_SIZE_FORMATTED = "1,048,576 bytes";

    private AnvilConstants()
    {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}