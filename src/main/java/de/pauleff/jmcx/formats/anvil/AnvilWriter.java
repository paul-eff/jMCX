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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        // Write Header (locations, timestamps)
        for (IChunk iChunk : region.getChunks())
        {
            Chunk chunk = (Chunk) iChunk;
            raf.seek(chunk.getIndex() * 4L);
            int offset = chunk.getLocation().getOffset();
            int sectorCount = chunk.getLocation().getSectorCount();

            if (chunk.getIndex() < 0 || chunk.getIndex() >= 1024)
            {
                throw new IllegalArgumentException("Chunk index out of bounds (0-1023): " + chunk.getIndex());
            }
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
            raf.seek(chunk.getIndex() * 4L + 4096L);
            raf.writeInt(chunk.getTimestamp());
        }

        // Write Chunks + padding
        for (IChunk iChunk : region.getChunks())
        {
            Chunk chunk = (Chunk) iChunk;
            if (chunk.getLocation().getOffset() == 0) continue;
            raf.seek(chunk.getLocation().getOffset() * 4096L);

            int length = chunk.getPayload().getLength();
            int compressionType = chunk.getPayload().getCompressionType();
            byte[] data = chunk.getPayload().getData();

            ByteBuffer buffer = ByteBuffer.allocate(5 + data.length).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(length);
            buffer.put((byte) compressionType);
            buffer.put(data);
            byte[] paddedData = AnvilUtils.padToSectorSize(buffer.array());

            // Validate that the chunk offset is sector-aligned
            long writeOffset = chunk.getLocation().getOffset() * 4096L;
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

    @Override
    public boolean validateRegion(IRegion region) throws IOException
    {
        if (region == null)
        {
            return false;
        }
        // TODO: Basic validation - could be expanded
        return region.getChunks().size() <= 1024;
    }

    @Override
    public boolean validateChunk(IChunk chunk) throws IOException
    {
        if (chunk == null)
        {
            return false;
        }
        // TODO: Basic validation - could be expanded
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
