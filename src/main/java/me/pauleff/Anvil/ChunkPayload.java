package me.pauleff.Anvil;

import me.pauleff.Exceptions.ChunkToLargeException;
import me.pauleff.Helpers;

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
     */
    public ChunkPayload(byte[] payload) throws IOException
    {
        this.payloadLength = payload.length;
        if (this.payloadLength == 0)
        {
            this.length = 0;
            // Compression type uncompressed used when chunk was not written yet
            this.compressionType = 3;
            this.data = new byte[0];
        } else
        {
            this.length = Helpers.readInt(Arrays.copyOfRange(payload, 0, 4), ByteOrder.BIG_ENDIAN);
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

    protected void compressAndSetData(byte[] data) throws IOException
    {
        byte[] buffer = compressData(data, getCompressionType());
        if ((buffer.length + 4 + 1) > 1048576) throw new ChunkToLargeException(buffer.length + 4 + 1);
        this.data = buffer;
        setLength(buffer.length);
        // Actual data length + 4 bytes for length + 1 byte for compression type
        setPayloadLength(4096 * (int) Math.ceil((buffer.length + 4 + 1) / 4096.0));
    }

    public byte[] getFullPayload()
    {
        ByteBuffer buffer = ByteBuffer.allocate(5 + this.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(this.length);
        buffer.put(this.compressionType);
        buffer.put(this.data);
        return Helpers.padToSectorSize(buffer.array(), 4096);
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
                    return new GZIPInputStream(byteStream).readAllBytes();
                case 2:
                    return new InflaterInputStream(byteStream).readAllBytes();
                case 3:
                    return data;
                default:
                    throw new IOException("Unknown/Unsupported compression type: " + compressionType);
            }
        }
    }

    /**
     * Compresses chunk data using the specified compression type.
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
                    try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream))
                    {
                        inputStream.transferTo(gzipStream);
                    }
                    break;
                case 2:
                    try (DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream))
                    {
                        inputStream.transferTo(deflaterStream);
                    }
                    break;
                case 3:
                    return data;
                default:
                    throw new IOException("Unknown/Unsupported compression type: " + compressionType);
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