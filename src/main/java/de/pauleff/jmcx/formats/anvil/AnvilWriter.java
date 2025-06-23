package de.pauleff.jmcx.formats.anvil;

import de.pauleff.jmcx.api.IAnvilWriter;
import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Region;
import de.pauleff.jmcx.util.AnvilUtils;

import static de.pauleff.jmcx.util.AnvilConstants.CHUNKS_PER_REGION;

import static de.pauleff.jmcx.util.AnvilConstants.SECTOR_SIZE_BYTES;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AnvilWriter implements IAnvilWriter
{
    private final File anvilFile;
    private final RandomAccessFile raf;
    private boolean backupEnabled = true;

    /**
     * Constructs an AnvilWriter object.
     *
     * @param anvilFile the Anvil file to write
     * @throws IOException if an I/O error occurs
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
        
        // Step 1: Allocate proper sectors for all chunks that have data
        int currentSectorOffset = 2; // Start after header (2 sectors = 8KiB)
        
        for (int i = 0; i < region.getChunks().size(); i++)
        {
            IChunk iChunk = region.getChunks().get(i);
            Chunk chunk = (Chunk) iChunk;
            
            if (chunk.getDataSize() > 0)
            {
                // Get the actual payload size
                byte[] fullPayload = chunk.getPayload().getFullPayload();
                int sectorsNeeded = AnvilUtils.calculateSectorCount(fullPayload.length);
                
                // Update chunk location with proper sector allocation
                chunk.getLocation().setOffset(currentSectorOffset);
                chunk.getLocation().setSectorCount(sectorsNeeded);
                
                currentSectorOffset += sectorsNeeded;
            }
            else
            {
                // Empty chunk - set to 0 offset and 0 sectors
                chunk.getLocation().setOffset(0);
                chunk.getLocation().setSectorCount(0);
            }
        }
        
        // Step 2: Write Header (locations, timestamps) - write in array order
        for (int i = 0; i < region.getChunks().size(); i++)
        {
            IChunk iChunk = region.getChunks().get(i);
            Chunk chunk = (Chunk) iChunk;
            
            // Write header entry at position i (array index corresponds to file header position)
            raf.seek(i * 4L);
            int offset = chunk.getLocation().getOffset();
            int sectorCount = chunk.getLocation().getSectorCount();

            if (offset < 0 || offset > 0xFFFFFF)
            { // 3-byte limit (24 bits)
                throw new IllegalArgumentException("Sector offset out of range: " + offset);
            }
            if (sectorCount < 0 || sectorCount > 0xFF)
            { // 1-byte limit (8 bits)
                throw new IllegalArgumentException("Sector count out of range: " + sectorCount);
            }

            raf.writeByte((offset >> 16) & 0xFF);
            raf.writeByte((offset >> 8) & 0xFF);
            raf.writeByte(offset & 0xFF);
            raf.writeByte(chunk.getLocation().getSectorCount() & 0xFF);
            
            // Write timestamp
            raf.seek(i * 4L + (long) SECTOR_SIZE_BYTES);
            raf.writeInt(chunk.getTimestamp());
        }

        // Step 3: Write chunk data in sector order
        for (IChunk iChunk : region.getChunks())
        {
            Chunk chunk = (Chunk) iChunk;
            if (chunk.getLocation().getOffset() == 0) continue; // Skip empty chunks
            
            raf.seek(chunk.getLocation().getOffset() * (long) SECTOR_SIZE_BYTES);

            // Get the complete payload (length + compression type + data)
            byte[] fullPayload = chunk.getPayload().getFullPayload();
            byte[] paddedData = AnvilUtils.padToSectorSize(fullPayload);

            // Validate that the chunk offset is sector-aligned
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
        // Don't close RAF here as it's managed by the close() method
    }

    @Override
    public void writeRegion(IRegion region) throws IOException
    {
        if (!(region instanceof Region))
        {
            throw new IllegalArgumentException("Region must be an instance of de.pauleff.jmcx.core.Region");
        }
        writeAnvilFile((Region) region);
    }

    @Override
    public void writeChunk(IChunk chunk) throws IOException
    {
        throw new UnsupportedOperationException("Individual chunk writing not yet implemented");
    }

    @Override
    public void createBackup(boolean enable)
    {
        this.backupEnabled = enable;
    }

    @Override
    public boolean isBackupEnabled()
    {
        return backupEnabled;
    }

    @Override
    public String getFilePath()
    {
        return anvilFile.getAbsolutePath();
    }

    @Override
    public boolean canWrite()
    {
        return anvilFile.canWrite() || (!anvilFile.exists() && anvilFile.getParentFile().canWrite());
    }

    public void setBackupEnabled(boolean enabled)
    {
        this.backupEnabled = enabled;
    }

    @Override
    public boolean validateRegion(IRegion region) throws IOException
    {
        if (region == null)
        {
            return false;
        }
        // Basic validation - could be expanded
        return region.getChunks().size() <= CHUNKS_PER_REGION;
    }

    @Override
    public boolean validateChunk(IChunk chunk) throws IOException
    {
        if (chunk == null)
        {
            return false;
        }
        // Basic validation - could be expanded
        return chunk.getX() >= 0 && chunk.getZ() >= 0;
    }

    @Override
    public void flush() throws IOException
    {
        if (raf != null)
        {
            raf.getFD().sync();
        }
    }

    @Override
    public void close() throws IOException
    {
        if (raf != null)
        {
            raf.close();
        }
    }
}
