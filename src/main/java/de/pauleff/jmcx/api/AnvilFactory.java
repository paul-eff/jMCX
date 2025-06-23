package de.pauleff.jmcx.api;

import de.pauleff.jmcx.formats.FileFormat;
import de.pauleff.jmcx.formats.anvil.AnvilReader;
import de.pauleff.jmcx.formats.anvil.AnvilWriter;

import java.io.File;
import java.io.IOException;

/**
 * Factory class for creating Anvil file readers and writers.
 * Provides convenient factory methods with automatic format detection and validation.
 *
 * @author Paul Ferlitz
 * @since 0.2
 */
public final class AnvilFactory
{
    // Private constructor to prevent instantiation
    private AnvilFactory()
    {
    }

    /**
     * Creates a reader for the specified file.
     * Automatically detects and validates the file format.
     *
     * @param file the .mca file to read
     * @return a new reader instance
     * @throws IOException              if file access fails
     * @throws IllegalArgumentException if file format is unsupported
     */
    public static IAnvilReader createReader(File file) throws IOException
    {
        if (file == null)
        {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (!isValidAnvilFile(file))
        {
            throw new IllegalArgumentException("Invalid or unsupported file format: " + file.getName());
        }

        return new AnvilReader(file);
    }

    /**
     * Creates a reader for the specified file path.
     *
     * @param filePath the path to the .mca file
     * @return a new reader instance
     * @throws IOException              if file access fails
     * @throws IllegalArgumentException if file format is unsupported
     */
    public static IAnvilReader createReader(String filePath) throws IOException
    {
        if (filePath == null || filePath.trim().isEmpty())
        {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        return createReader(new File(filePath));
    }

    /**
     * Creates a writer for the specified file.
     * Automatically validates the file format and path.
     *
     * @param file the .mca file to write
     * @return a new writer instance
     * @throws IOException              if file access fails
     * @throws IllegalArgumentException if file format is unsupported
     */
    public static IAnvilWriter createWriter(File file) throws IOException
    {
        if (file == null)
        {
            throw new IllegalArgumentException("File cannot be null");
        }

        FileFormat format = FileFormat.detectFormat(file);
        if (format != FileFormat.ANVIL)
        {
            throw new IllegalArgumentException("Unsupported file format for writing: " + format.getDescription());
        }

        return new AnvilWriter(file);
    }

    /**
     * Creates a writer for the specified file path.
     *
     * @param filePath the path to the .mca file
     * @return a new writer instance
     * @throws IOException              if file access fails
     * @throws IllegalArgumentException if file format is unsupported
     */
    public static IAnvilWriter createWriter(String filePath) throws IOException
    {
        if (filePath == null || filePath.trim().isEmpty())
        {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        return createWriter(new File(filePath));
    }

    /**
     * Checks if the file is a valid Anvil file that can be processed.
     *
     * @param file the file to validate
     * @return true if file is valid and supported
     */
    public static boolean isValidAnvilFile(File file)
    {
        if (file == null || !file.exists() || !file.isFile())
        {
            return false;
        }

        FileFormat format = FileFormat.detectFormat(file);
        return format == FileFormat.ANVIL;
    }

    /**
     * Checks if the file path represents a valid Anvil file.
     *
     * @param filePath the file path to validate
     * @return true if file is valid and supported
     */
    public static boolean isValidAnvilFile(String filePath)
    {
        if (filePath == null || filePath.trim().isEmpty())
        {
            return false;
        }

        return isValidAnvilFile(new File(filePath));
    }
}