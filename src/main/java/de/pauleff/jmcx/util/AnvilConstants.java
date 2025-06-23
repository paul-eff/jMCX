package de.pauleff.jmcx.util;

/**
 * Constants used throughout the jMCX library for Anvil format operations.
 * This class centralizes all magic numbers and string literals to improve maintainability.
 */
public final class AnvilConstants
{

    // File format constants
    public static final String MCA_EXTENSION = ".mca";

    // Size constants (in bytes)
    public static final int SECTOR_SIZE_BYTES = 4096;
    public static final int MAX_CHUNK_SIZE_BYTES = 1048576; // 1MiB
    public static final int CHUNKS_PER_REGION = 1024;
    public static final int CHUNKS_PER_REGION_SIDE = 32;
    public static final int BLOCKS_PER_CHUNK_SIDE = 16;

    // Chunk size constants (formatted for error messages)
    public static final String MAX_CHUNK_SIZE_FORMATTED = "1,048,576 bytes";

    private AnvilConstants()
    {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}