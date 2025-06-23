package de.pauleff.jmcx.api;

import java.io.IOException;
import java.util.Optional;

/**
 * Interface for reading Minecraft Anvil (.mca) region files.
 *
 * @author Paul Ferlitz
 */
public interface IAnvilReader extends AutoCloseable
{
    /**
     * Reads the entire region from the file.
     *
     * @return the {@link IRegion} with all chunks
     * @throws IOException if reading fails
     */
    IRegion readRegion() throws IOException;

    /**
     * Reads a specific chunk from the region.
     *
     * @param chunkX chunk x-coordinate
     * @param chunkZ chunk z-coordinate
     * @return {@link IChunk} if exists, empty otherwise
     * @throws IOException if reading fails
     */
    Optional<IChunk> readChunk(int chunkX, int chunkZ) throws IOException;

    /**
     * Gets the file format.
     *
     * @return file format ("mca")
     */
    String getFileFormat();


    /**
     * Gets region coordinates from filename.
     *
     * @return [regionX, regionZ] coordinates
     */
    int[] getRegionCoordinates();

    /**
     * Gets source file path.
     *
     * @return file path
     */
    String getFilePath();

    /**
     * Checks if file exists and is readable.
     *
     * @return true if readable
     */
    boolean canRead();

    /**
     * Gets file size in bytes.
     *
     * @return file size
     * @throws IOException if access fails
     */
    long getFileSize() throws IOException;

    /**
     * Closes reader and releases resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}