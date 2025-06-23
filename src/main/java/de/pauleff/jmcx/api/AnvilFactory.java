package de.pauleff.jmcx.api;

import de.pauleff.jmcx.formats.anvil.AnvilReader;
import de.pauleff.jmcx.formats.anvil.AnvilWriter;

import java.io.File;
import java.io.IOException;

import static de.pauleff.jmcx.util.AnvilConstants.MCA_EXTENSION;

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

        FileFormat format = detectFormat(file);
        if (format != FileFormat.ANVIL_MCA)
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
     * Detects the file format based on the file extension.
     *
     * @param file the file to analyze
     * @return the detected file format
     */
    public static FileFormat detectFormat(File file)
    {
        if (file == null)
        {
            return FileFormat.UNKNOWN;
        }

        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(MCA_EXTENSION))
        {
            return FileFormat.ANVIL_MCA;
        } else if (fileName.endsWith(".mcr"))
        {
            return FileFormat.MCREGION_MCR;
        }

        return FileFormat.UNKNOWN;
    }

    /**
     * Detects the file format based on the file path.
     *
     * @param filePath the file path to analyze
     * @return the detected file format
     */
    public static FileFormat detectFormat(String filePath)
    {
        if (filePath == null || filePath.trim().isEmpty())
        {
            return FileFormat.UNKNOWN;
        }

        return detectFormat(new File(filePath));
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

        FileFormat format = detectFormat(file);
        return format == FileFormat.ANVIL_MCA;
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

    /**
     * Validates that the filename follows the correct Anvil naming convention (r.x.z.mca).
     *
     * @param filename the filename to validate
     * @return true if filename follows Anvil convention
     */
    public static boolean isValidAnvilFilename(String filename)
    {
        if (filename == null || filename.trim().isEmpty())
        {
            return false;
        }

        String[] parts = filename.split("\\.");
        if (parts.length != 4 || !"r".equals(parts[0]) || !MCA_EXTENSION.substring(1).equals(parts[3]))
        {
            return false;
        }

        try
        {
            Integer.parseInt(parts[1]); // x coordinate
            Integer.parseInt(parts[2]); // z coordinate
            return true;
        } catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * Enumeration of supported Anvil file formats.
     */
    public enum FileFormat
    {
        /**
         * Anvil format (.mca files) - current Minecraft format
         */
        ANVIL_MCA("mca", "Anvil Format"),

        /**
         * McRegion format (.mcr files) - legacy format (not yet supported)
         */
        MCREGION_MCR("mcr", "McRegion Format"),

        /**
         * Unknown or unsupported format
         */
        UNKNOWN("unknown", "Unknown Format");

        private final String extension;
        private final String description;

        FileFormat(String extension, String description)
        {
            this.extension = extension;
            this.description = description;
        }

        /**
         * Gets the file extension for this format.
         *
         * @return the file extension
         */
        public String getExtension()
        {
            return extension;
        }

        /**
         * Gets the human-readable description of this format.
         *
         * @return the format description
         */
        public String getDescription()
        {
            return description;
        }
    }
}