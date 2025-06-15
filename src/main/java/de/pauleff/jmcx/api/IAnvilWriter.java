package de.pauleff.jmcx.api;

import java.io.IOException;

/**
 * Interface for writing Minecraft Anvil (.mca) region files.
 * Provides type-safe access to write region and chunk data with automatic backup and validation.
 *
 * @author Paul Ferlitz
 * @since 0.2
 */
public interface IAnvilWriter extends AutoCloseable
{
    /**
     * Writes the complete region to the .mca file.
     *
     * @param region the region to write
     * @throws IOException if writing fails
     */
    void writeRegion(IRegion region) throws IOException;

    /**
     * Writes a specific chunk to the region file.
     *
     * @param chunk the chunk to write
     * @throws IOException if writing fails
     */
    void writeChunk(IChunk chunk) throws IOException;

    /**
     * Enables or disables automatic backup creation before writing.
     *
     * @param enable true to create backups, false to disable
     */
    void createBackup(boolean enable);

    /**
     * Checks if backup creation is enabled.
     *
     * @return true if backups are enabled
     */
    boolean isBackupEnabled();

    /**
     * Gets the target file path of this writer.
     *
     * @return the file path as string
     */
    String getFilePath();

    /**
     * Checks if the target file can be written to.
     *
     * @return true if file is writable
     */
    boolean canWrite();

    /**
     * Validates the region data before writing.
     *
     * @param region the region to validate
     * @return true if region is valid for writing
     * @throws IOException if validation fails
     */
    boolean validateRegion(IRegion region) throws IOException;

    /**
     * Validates the chunk data before writing.
     *
     * @param chunk the chunk to validate
     * @return true if chunk is valid for writing
     * @throws IOException if validation fails
     */
    boolean validateChunk(IChunk chunk) throws IOException;

    /**
     * Flushes any pending writes to disk.
     *
     * @throws IOException if flushing fails
     */
    void flush() throws IOException;

    /**
     * Closes the writer and releases any system resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}