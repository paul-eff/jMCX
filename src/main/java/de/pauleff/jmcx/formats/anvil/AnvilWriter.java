package de.pauleff.jmcx.formats.anvil;

import de.pauleff.jmcx.api.IAnvilWriter;
import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Region;
import de.pauleff.jmcx.util.AnvilUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;
import static de.pauleff.jmcx.util.AnvilConstants.SECTOR_SIZE_BYTES;

/**
 * Implementation of {@link IAnvilWriter} for writing Anvil region files.
 *
 * @author Paul Ferlitz
 */
public class AnvilWriter implements IAnvilWriter
{
    private final File anvilFile;
    private final RandomAccessFile raf;
    private boolean backupEnabled = true;

    /**
     * Constructs an AnvilWriter object.
     *
     * @param anvilFile the Anvil file to write
     * @throws IOException if I/O error occurs
     */
    public AnvilWriter(File anvilFile) throws IOException
    {
        this.anvilFile = anvilFile;
        this.raf = new RandomAccessFile(anvilFile, "rw");
    }

    private void writeAnvilFile(Region region) throws IOException
    {
        if (backupEnabled && Files.exists(anvilFile.toPath()))
        {
            File backupFile = new File(anvilFile.getPath() + ".bak");
            Files.copy(anvilFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("Created backup of file %s%n", anvilFile.getName());
        }

        int currentSectorOffset = 2; // Start after header (2 sectors = 8KiB)

        for (int i = 0; i < region.getChunks().size(); i++)
        {
            IChunk iChunk = region.getChunks().get(i);
            Chunk chunk = (Chunk) iChunk;

            if (chunk.getDataSize() > 0)
            {
                byte[] fullPayload = chunk.getPayload().getFullPayload();
                int sectorsNeeded = AnvilUtils.calculateSectorCount(fullPayload.length);

                chunk.getLocation().setOffset(currentSectorOffset);
                chunk.getLocation().setSectorCount(sectorsNeeded);

                currentSectorOffset += sectorsNeeded;
            } else
            {
                chunk.getLocation().setOffset(0);
                chunk.getLocation().setSectorCount(0);
            }
        }

        for (int i = 0; i < region.getChunks().size(); i++)
        {
            IChunk iChunk = region.getChunks().get(i);
            Chunk chunk = (Chunk) iChunk;

            raf.seek(i * 4L);
            int offset = chunk.getLocation().getOffset();
            int sectorCount = chunk.getLocation().getSectorCount();

            if (offset < 0 || offset > 0xFFFFFF)
            {
                throw new IllegalArgumentException("Sector offset out of range: " + offset);
            }
            if (sectorCount < 0 || sectorCount > 0xFF)
            {
                throw new IllegalArgumentException("Sector count out of range: " + sectorCount);
            }

            raf.writeByte((offset >> 16) & 0xFF);
            raf.writeByte((offset >> 8) & 0xFF);
            raf.writeByte(offset & 0xFF);
            raf.writeByte(chunk.getLocation().getSectorCount() & 0xFF);

            raf.seek(i * 4L + (long) SECTOR_SIZE_BYTES);
            raf.writeInt(chunk.getTimestamp());
        }

        for (IChunk iChunk : region.getChunks())
        {
            Chunk chunk = (Chunk) iChunk;
            if (chunk.getLocation().getOffset() == 0) continue;

            raf.seek(chunk.getLocation().getOffset() * (long) SECTOR_SIZE_BYTES);

            byte[] fullPayload = chunk.getPayload().getFullPayload();
            byte[] paddedData = AnvilUtils.padToSectorSize(fullPayload);

            long writeOffset = chunk.getLocation().getOffset() * (long) SECTOR_SIZE_BYTES;
            if (!AnvilUtils.isSectorAligned(writeOffset))
            {
                throw new IOException(
                        "Chunk offset is not sector-aligned. Offset: " + writeOffset +
                                ", should be multiple of " + AnvilUtils.SECTOR_SIZE
                );
            }

            raf.write(paddedData);
        }
    }

    /**
     * Writes region to file.
     *
     * @param region {@link IRegion} to write
     * @throws IOException if I/O error occurs
     */
    @Override
    public void writeRegion(IRegion region) throws IOException
    {
        if (!(region instanceof Region))
        {
            throw new IllegalArgumentException("Region must be an instance of de.pauleff.jmcx.core.Region");
        }
        writeAnvilFile((Region) region);
    }

    /**
     * Writes individual chunk (not implemented).
     *
     * @param chunk {@link IChunk} to write
     * @throws UnsupportedOperationException always
     */
    @Override
    public void writeChunk(IChunk chunk)
    {
        throw new UnsupportedOperationException("Individual chunk writing not yet implemented");
    }

    /**
     * Enables or disables backup creation.
     *
     * @param enable whether to create backup
     */
    @Override
    public void createBackup(boolean enable)
    {
        this.backupEnabled = enable;
    }

    /**
     * Checks if backup is enabled.
     *
     * @return true if backup enabled
     */
    @Override
    public boolean isBackupEnabled()
    {
        return backupEnabled;
    }

    /**
     * Sets backup enabled state.
     *
     * @param enabled whether backup enabled
     */
    public void setBackupEnabled(boolean enabled)
    {
        this.backupEnabled = enabled;
    }

    /**
     * Gets absolute file path.
     *
     * @return absolute file path
     */
    @Override
    public String getFilePath()
    {
        return anvilFile.getAbsolutePath();
    }

    /**
     * Checks if file can be written.
     *
     * @return true if file is writable
     */
    @Override
    public boolean canWrite()
    {
        return anvilFile.canWrite() || (!anvilFile.exists() && anvilFile.getParentFile().canWrite());
    }

    /**
     * Validates region before writing.
     *
     * @param region {@link IRegion} to validate
     * @return true if valid
     * @throws IOException if validation fails
     */
    @Override
    public boolean validateRegion(IRegion region)
    {
        if (region == null)
        {
            return false;
        }
        return region.getChunks().size() <= CHUNKS_PER_REGION;
    }

    /**
     * Validates chunk before writing.
     *
     * @param chunk {@link IChunk} to validate
     * @return true if valid
     * @throws IOException if validation fails
     */
    @Override
    public boolean validateChunk(IChunk chunk)
    {
        if (chunk == null)
        {
            return false;
        }
        return chunk.getX() >= 0 && chunk.getZ() >= 0;
    }

    /**
     * Flushes pending writes.
     *
     * @throws IOException if I/O error occurs
     */
    @Override
    public void flush() throws IOException
    {
        raf.getFD().sync();
    }

    /**
     * Closes the file writer.
     *
     * @throws IOException if I/O error occurs
     */
    @Override
    public void close() throws IOException
    {
        raf.close();
    }
}
