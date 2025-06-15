package de.pauleff.jmcx.core;

import de.pauleff.jmcx.exceptions.ChunkTooLargeException;
import de.pauleff.jmcx.util.AnvilUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * The ChunkPayload class represents the actual chunk's data.
 * According to the Minecraft Wiki, chunks are always less than 1MiB (1,048,576 bytes).
 */
public class ChunkPayload
{
    private int payloadLength;
    private int length;
    private final byte compressionType;
    private byte[] data;

    /**
     * Constructs a ChunkPayload object from a byte array.
     *
     * @param payload the byte array representing the chunk payload
     * @throws IOException if an I/O error occurs during decompression
     * @throws ChunkToLargeException if the payload exceeds the maximum chunk size
     */
    public ChunkPayload(byte[] payload) throws IOException
    {
        // Validate payload size
        if (payload.length > MAX_CHUNK_SIZE)
        {
            throw new ChunkTooLargeException(
                "Chunk payload exceeds maximum size. Size: " + payload.length + 
                " bytes, Maximum: " + MAX_CHUNK_SIZE + " bytes"
            );
        }
        
        this.payloadLength = payload.length;
        if (this.payloadLength == 0)
        {
            this.length = 0;
            // Compression type uncompressed used when chunk was not written yet
            this.compressionType = 3;
            this.data = new byte[0];
        } else
        {
            this.length = AnvilUtils.readInt(Arrays.copyOfRange(payload, 0, 4), ByteOrder.BIG_ENDIAN);
            
            // Validate length field doesn't exceed remaining payload
            if (this.length < 0 || this.length > payload.length - 5)
            {
                throw new IOException(
                    "Invalid chunk length field: " + this.length + 
                    ". Payload size: " + payload.length + " bytes"
                );
            }
            
            this.compressionType = payload[4];
            this.data = Arrays.copyOfRange(payload, 5, 5 + this.length);
        }
    }

    private void setPayloadLength(int payloadLength)
    {
        this.payloadLength = payloadLength;
    }

    private void setLength(int length)
    {
        this.length = length;
    }

    /**
     * The maximum chunk size as specified by Minecraft Wiki (1MiB).
     */
    public static final int MAX_CHUNK_SIZE = 1048576; // 1MiB = 1024 * 1024 bytes

    protected void compressAndSetData(byte[] data) throws IOException
    {
        // Validate input data size before compression
        if (data.length > MAX_CHUNK_SIZE)
        {
            throw new ChunkTooLargeException(
                "Uncompressed chunk data exceeds maximum size. Size: " + data.length + 
                " bytes, Maximum: " + MAX_CHUNK_SIZE + " bytes"
            );
        }
        
        byte[] buffer = compressData(data, getCompressionType());
        
        // Validate total payload size (compressed data + 4 bytes length + 1 byte compression type)
        int totalPayloadSize = buffer.length + 4 + 1;
        if (totalPayloadSize > MAX_CHUNK_SIZE)
        {
            throw new ChunkTooLargeException(
                "Compressed chunk payload exceeds maximum size. Size: " + totalPayloadSize + 
                " bytes, Maximum: " + MAX_CHUNK_SIZE + " bytes"
            );
        }
        
        this.data = buffer;
        setLength(buffer.length);
        // Calculate payload length using sector alignment
        int totalSize = buffer.length + 4 + 1; // data + length field + compression type
        setPayloadLength(AnvilUtils.calculateSectorCount(totalSize) * AnvilUtils.SECTOR_SIZE);
    }

    /**
     * Gets the full payload with proper sector alignment.
     * The payload includes: 4-byte length + 1-byte compression type + data + padding to 4KiB boundary.
     *
     * @return the full padded payload
     */
    public byte[] getFullPayload()
    {
        ByteBuffer buffer = ByteBuffer.allocate(5 + this.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(this.length);
        buffer.put(this.compressionType);
        buffer.put(this.data);
        return AnvilUtils.padToSectorSize(buffer.array());
    }

    /**
     * Gets the payload length.
     *
     * @return the payload length
     */
    public int getPayloadLength()
    {
        return payloadLength;
    }

    /**
     * Gets the length of the chunk data.
     *
     * @return the length of the chunk data
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Gets the compression type of the chunk data.
     *
     * @return the compression type
     */
    public byte getCompressionType()
    {
        return compressionType;
    }

    /**
     * Gets the (possibly compressed) chunk data.
     *
     * @return the (possibly compressed) chunk data
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * Gets the decompressed chunk data.
     *
     * @return the decompressed chunk data
     */
    public byte[] getDecompressedData() throws IOException
    {
        return decompressData(getData(), getCompressionType());
    }

    /**
     * Decompresses chunk data using the specified compression type.
     * 
     * Compression types according to Minecraft Wiki:
     * 1 = GZip (unused in practice)
     * 2 = Zlib (standard compression)
     * 3 = Uncompressed
     * 4 = LZ4 (not yet implemented)
     * 127 = Custom compression (not supported)
     *
     * @param data            The compressed chunk data.
     * @param compressionType The compression type.
     * @return The decompressed data.
     * @throws IOException If decompression fails.
     */
    public byte[] decompressData(byte[] data, byte compressionType) throws IOException
    {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data))
        {
            switch (compressionType)
            {
                case 1:
                    // GZip compression (unused in practice but supported for compatibility)
                    return new GZIPInputStream(byteStream).readAllBytes();
                case 2:
                    // Zlib compression (standard)
                    return new InflaterInputStream(byteStream).readAllBytes();
                case 3:
                    // Uncompressed
                    return data;
                case 4:
                    // LZ4 compression (not yet implemented)
                    throw new IOException("LZ4 compression (type 4) is not yet implemented");
                case 127:
                    // Custom compression (not supported)
                    throw new IOException("Custom compression (type 127) is not supported");
                default:
                    throw new IOException("Unknown compression type: " + compressionType + 
                        ". Supported types: 1 (GZip), 2 (Zlib), 3 (Uncompressed)");
            }
        }
    }

    /**
     * Compresses chunk data using the specified compression type.
     * 
     * Compression types according to Minecraft Wiki:
     * 1 = GZip (unused in practice)
     * 2 = Zlib (standard compression)
     * 3 = Uncompressed
     * 4 = LZ4 (not yet implemented)
     * 127 = Custom compression (not supported)
     *
     * @param data            The uncompressed chunk data.
     * @param compressionType The compression type.
     * @return The compressed data.
     * @throws IOException If compression fails.
     */
    public byte[] compressData(byte[] data, byte compressionType) throws IOException
    {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ByteArrayInputStream inputStream = new ByteArrayInputStream(data))
        {
            switch (compressionType)
            {
                case 1:
                    // GZip compression (unused in practice but supported for compatibility)
                    try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream))
                    {
                        inputStream.transferTo(gzipStream);
                    }
                    break;
                case 2:
                    // Zlib compression (standard)
                    try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream))
                    {
                        inputStream.transferTo(deflaterStream);
                    }
                    break;
                case 3:
                    // Uncompressed
                    return data;
                case 4:
                    // LZ4 compression (not yet implemented)
                    throw new IOException("LZ4 compression (type 4) is not yet implemented");
                case 127:
                    // Custom compression (not supported)
                    throw new IOException("Custom compression (type 127) is not supported");
                default:
                    throw new IOException("Unknown compression type: " + compressionType + 
                        ". Supported types: 1 (GZip), 2 (Zlib), 3 (Uncompressed)");
            }
            return byteStream.toByteArray();
        }
    }

    /**
     * Returns a string representation of the ChunkPayload object.
     *
     * @return a string representation of the ChunkPayload object
     */
    @Override
    public String toString()
    {
        return "ChunkData{" +
                "payloadLength=" + payloadLength +
                ", length=" + length +
                ", compressionType=" + compressionType +
                ", chunkData (Bytes)=" + data.length +
                '}';
    }
}