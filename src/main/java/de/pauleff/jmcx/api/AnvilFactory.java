package de.pauleff.jmcx.api;

import de.pauleff.jmcx.formats.FileFormat;
import de.pauleff.jmcx.formats.anvil.AnvilReader;
import de.pauleff.jmcx.formats.anvil.AnvilWriter;

import java.io.File;
import java.io.IOException;

/**
 * Factory class for creating Anvil file readers and writers.
 *
 * @author Paul Ferlitz
 */
public final class AnvilFactory
{
    private AnvilFactory()
    {
    }

    /**
     * Creates a reader for the specified file.
     *
     * @param anvilFile .mca file to read
     * @return new {@link IAnvilReader} instance
     * @throws IOException if file access fails
     * @throws IllegalArgumentException if file format unsupported
     */
    public static IAnvilReader createReader(File anvilFile) throws IOException
    {
        if (anvilFile == null)
        {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (!isValidAnvilFile(anvilFile))
        {
            throw new IllegalArgumentException("Invalid or unsupported file format: " + anvilFile.getName());
        }

        return new AnvilReader(anvilFile);
    }

    /**
     * Creates a reader for the specified file path.
     *
     * @param filePath path to .mca file
     * @return new {@link IAnvilReader} instance
     * @throws IOException if file access fails
     * @throws IllegalArgumentException if file format unsupported
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
     *
     * @param anvilFile .mca file to write
     * @return new {@link IAnvilWriter} instance
     * @throws IOException if file access fails
     * @throws IllegalArgumentException if file format unsupported
     */
    public static IAnvilWriter createWriter(File anvilFile) throws IOException
    {
        if (anvilFile == null)
        {
            throw new IllegalArgumentException("File cannot be null");
        }

        FileFormat format = FileFormat.detectFormat(anvilFile);
        if (format != FileFormat.ANVIL)
        {
            throw new IllegalArgumentException("Unsupported file format for writing: " + format.getDescription());
        }

        return new AnvilWriter(anvilFile);
    }

    /**
     * Creates a writer for the specified file path.
     *
     * @param filePath path to .mca file
     * @return new {@link IAnvilWriter} instance
     * @throws IOException if file access fails
     * @throws IllegalArgumentException if file format unsupported
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
     * Checks if file is a valid Anvil file.
     *
     * @param file file to validate
     * @return true if valid and supported
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
     * Checks if file path represents a valid Anvil file.
     *
     * @param filePath file path to validate
     * @return true if valid and supported
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