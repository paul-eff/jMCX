package de.pauleff.jmcx.formats;

import java.io.File;

/**
 * Enumeration of supported Anvil file formats.
 */
public enum FileFormat
{
    /**
     * Anvil format (.mca files) - current Minecraft format
     */
    ANVIL("mca", "Anvil Format"),

    /**
     * McRegion format (.mcr files) - legacy format
     */
    MCREGION("mcr", "McRegion (Alpha) Format"),

    /**
     * Bedrock format (.mcworld files) - current Bedrock format
     */
    BEDROCK("mcworld", "Bedrock Format"),

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
     * Detects the file format based on the file extension.
     *
     * @param file the file to analyze
     * @return the detected file format
     */
    public static FileFormat detectFormat(File file)
    {
        if (file == null) return FileFormat.UNKNOWN;

        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(ANVIL.getExtension())) return ANVIL;
        if (fileName.endsWith(MCREGION.getExtension())) return MCREGION;
        if (fileName.endsWith(BEDROCK.getExtension())) return BEDROCK;

        return FileFormat.UNKNOWN;
    }

    /**
     * Detects the file format based on the file extension.
     *
     * @param fileName the file to analyze
     * @return the detected file format
     */
    public static FileFormat detectFormat(String fileName)
    {
        if (fileName == null || fileName.trim().isEmpty())
        {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        return detectFormat(new File(fileName));
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