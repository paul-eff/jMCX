package de.pauleff.IO;

import de.pauleff.Anvil.Chunk;
import de.pauleff.Anvil.Region;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The AnvilReader class is responsible for reading Anvil region files.
 * Currently supports .mca (Anvil format) files only.
 * Future versions may include .mcr (McRegion format) support.
 */
public class AnvilReader
{
    private final File anvilFile;
    private final RandomAccessFile raf;

    /**
     * Constructs an AnvilReader object.
     *
     * @param anvilFile the Anvil file to read (.mca format)
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the file format is not supported
     */
    public AnvilReader(File anvilFile) throws IOException
    {
        validateFileFormat(anvilFile);
        this.anvilFile = anvilFile;
        this.raf = new RandomAccessFile(anvilFile, "r");
    }

    /**
     * Reads the whole Anvil file and returns a Region object.
     * The created RandomAccessFile will be closed after completion.
     *
     * @return the Region object representing the region in the Anvil file
     * @throws IOException if an I/O error occurs
     */
    public Region readAnvilFile() throws IOException
    {
        Region region = null;
        try
        {
            int[] coordinates = parseFilenameToCoordinates(anvilFile.getName());
            region = new Region(coordinates[0], coordinates[1], raf);
        } finally
        {
            raf.close();
        }
        return region;
    }

    /**
     * Reads a specific chunk from the Anvil file.
     * The created RandomAccessFile will not be closed after completion.
     *
     * @param x the x-coordinate of the chunk
     * @param z the z-coordinate of the chunk
     * @return the Chunk object representing the chunk in the Anvil file
     */
    public Chunk readChunk(int x, int z)
    {
        return null;
    }

    /**
     * Validates that the file has a supported format (.mca only).
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if the file format is not supported
     */
    private void validateFileFormat(File file) throws IllegalArgumentException
    {
        String filename = file.getName().toLowerCase();
        if (!filename.endsWith(".mca"))
        {
            throw new IllegalArgumentException(
                "Unsupported file format. Currently only .mca (Anvil) files are supported, got: " + filename
            );
        }
    }

    /**
     * Gets the region file format based on file extension.
     *
     * @return "mca" for Anvil format
     */
    public String getFileFormat()
    {
        String filename = anvilFile.getName().toLowerCase();
        if (filename.endsWith(".mca"))
        {
            return "mca";
        }
        return "unknown";
    }

    /**
     * Parses the filename to extract the region coordinates.
     * Supports .mca filename format (r.x.z.mca).
     *
     * @param filename the filename of the region file
     * @return an array containing the x and z coordinates of the region
     * @throws IllegalArgumentException if the filename format is invalid
     */
    private int[] parseFilenameToCoordinates(String filename) throws IllegalArgumentException
    {
        String[] parts = filename.split("\\.");
        if (parts.length < 4 || !"r".equals(parts[0]))
        {
            throw new IllegalArgumentException(
                "Invalid region filename format. Expected: r.x.z.mca, got: " + filename
            );
        }
        
        try
        {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new int[]{x, z};
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(
                "Invalid coordinates in filename: " + filename + ". Coordinates must be integers.", e
            );
        }
    }
}