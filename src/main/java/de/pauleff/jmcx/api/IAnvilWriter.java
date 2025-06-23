package de.pauleff.jmcx.api;

import java.io.IOException;

/**
 * Interface for writing Minecraft Anvil (.mca) region files.
 *
 * @author Paul Ferlitz
 */
public interface IAnvilWriter extends AutoCloseable
{
    /**
     * Writes the complete region to file.
     *
     * @param region {@link IRegion} to write
     * @throws IOException if writing fails
     */
    void writeRegion(IRegion region) throws IOException;

    /**
     * Writes a chunk to the region file.
     *
     * @param chunk {@link IChunk} to write
     * @throws IOException if writing fails
     */
    void writeChunk(IChunk chunk) throws IOException;

    /**
     * Enables or disables automatic backup creation.
     *
     * @param enable true to create backups
     */
    void createBackup(boolean enable);

    /**
     * Checks if backup creation is enabled.
     *
     * @return true if backups enabled
     */
    boolean isBackupEnabled();

    /**
     * Gets target file path.
     *
     * @return file path
     */
    String getFilePath();

    /**
     * Checks if target file is writable.
     *
     * @return true if writable
     */
    boolean canWrite();

    /**
     * Validates region data before writing.
     *
     * @param region {@link IRegion} to validate
     * @return true if valid
     * @throws IOException if validation fails
     */
    boolean validateRegion(IRegion region) throws IOException;

    /**
     * Validates chunk data before writing.
     *
     * @param chunk {@link IChunk} to validate
     * @return true if valid
     * @throws IOException if validation fails
     */
    boolean validateChunk(IChunk chunk) throws IOException;

    /**
     * Flushes pending writes to disk.
     *
     * @throws IOException if flushing fails
     */
    void flush() throws IOException;

    /**
     * Closes writer and releases resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}