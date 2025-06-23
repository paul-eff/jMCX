package de.pauleff.jmcx.api;

import java.io.IOException;
import java.util.Optional;

/**
 * Interface for reading Minecraft Anvil (.mca) region files.
 * Provides type-safe access to region and chunk data with automatic resource management.
 *
 * @author Paul Ferlitz
 * @since 0.2
 */
public interface IAnvilReader extends AutoCloseable
{
    /**
     * Reads the entire region from the .mca file.
     *
     * @return the complete region with all chunks
     * @throws IOException if reading fails
     */
    IRegion readRegion() throws IOException;

    /**
     * Reads a specific chunk from the region file.
     *
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return the chunk if it exists, empty otherwise
     * @throws IOException if reading fails
     */
    Optional<IChunk> readChunk(int chunkX, int chunkZ) throws IOException;

    /**
     * Gets the file format of the region file.
     *
     * @return the file format (e.g., "mca")
     */
    String getFileFormat();


    /**
     * Gets the region coordinates extracted from the filename.
     *
     * @return array of [regionX, regionZ] coordinates
     */
    int[] getRegionCoordinates();

    /**
     * Gets the source file path of this reader.
     *
     * @return the file path as string
     */
    String getFilePath();

    /**
     * Checks if the region file exists and is readable.
     *
     * @return true if file exists and is readable
     */
    boolean canRead();

    /**
     * Gets the file size in bytes.
     *
     * @return the file size in bytes
     * @throws IOException if file access fails
     */
    long getFileSize() throws IOException;

    /**
     * Closes the reader and releases any system resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}