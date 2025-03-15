package me.pauleff.IO;

import me.pauleff.Anvil.Chunk;
import me.pauleff.Anvil.Region;
import me.pauleff.Helpers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AnvilWriter
{
    private final File anvilFile;
    private final RandomAccessFile raf;

    /**
     * Constructs an AnvilReader object.
     *
     * @param anvilFile the Anvil file to read
     * @throws IOException if an I/O error occurs
     */
    public AnvilWriter(File anvilFile) throws IOException
    {
        this.anvilFile = anvilFile;
        this.raf = new RandomAccessFile(anvilFile, "rw");
    }

    public void writeAnvilFile(Region region) throws IOException
    {
        if (Files.exists(anvilFile.toPath()))
        {
            File backupFile = new File(anvilFile.getPath() + ".bak");
            Files.copy(anvilFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("Created backup of file %s%n", anvilFile.getName());
        }
        // Write Header (locations, timestamps)
        for (Chunk chunk : region.getChunks())
        {
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
        for (Chunk chunk : region.getChunks())
        {
            if (chunk.getLocation().getOffset() == 0) continue;
            raf.seek(chunk.getLocation().getOffset() * 4096L);

            int length = chunk.getPayload().getLength();
            int compressionType = chunk.getPayload().getCompressionType();
            byte[] data = chunk.getPayload().getData();

            ByteBuffer buffer = ByteBuffer.allocate(5 + data.length).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(length);
            buffer.put((byte) compressionType);
            buffer.put(data);
            byte[] paddedData = Helpers.padToSectorSize(buffer.array(), 4096);

            raf.write(paddedData);
        }
        raf.close();
    }
}
