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

import static de.pauleff.jmcx.util.AnvilConstants.MAX_CHUNK_SIZE_BYTES;

/**
 * Represents chunk data with compression handling.
 *
 * @author Paul Ferlitz
 */
public class ChunkPayload
{
    private final byte compressionType;
    private int payloadLength;
    private int length;
    private byte[] compressedData;

    /**
     * Constructs a ChunkPayload from byte array.
     *
     * @param payload byte array representing chunk payload
     * @throws IOException if I/O error occurs during decompression
     * @throws ChunkTooLargeException if payload exceeds maximum chunk size
     */
    public ChunkPayload(byte[] payload) throws IOException
    {
        if (payload.length > MAX_CHUNK_SIZE_BYTES)
        {
            throw new ChunkTooLargeException(
                    "Chunk payload exceeds maximum size. Size: " + payload.length +
                            " bytes, Maximum: " + MAX_CHUNK_SIZE_BYTES + " bytes"
            );
        }

        this.payloadLength = payload.length;
        if (this.payloadLength == 0)
        {
            this.length = 0;
            this.compressionType = 3;
            this.compressedData = new byte[0];
        } else
        {
            this.length = AnvilUtils.readInt(Arrays.copyOfRange(payload, 0, 4), ByteOrder.BIG_ENDIAN);

            if (this.length < 0 || this.length > payload.length - 5)
            {
                throw new IOException(
                        "Invalid chunk length field: " + this.length +
                                ". Payload size: " + payload.length + " bytes"
                );
            }

            this.compressionType = payload[4];
            this.compressedData = Arrays.copyOfRange(payload, 5, 5 + this.length);
        }
    }

    /**
     * Compresses data using current compression type and updates internal state.
     *
     * @param data uncompressed chunk data
     * @throws IOException if compression fails
     * @throws ChunkTooLargeException if compressed data exceeds limits
     */
    protected void compressAndSetData(byte[] data) throws IOException
    {
        if (data.length > MAX_CHUNK_SIZE_BYTES)
        {
            throw new ChunkTooLargeException(
                    "Uncompressed chunk data exceeds maximum size. Size: " + data.length +
                            " bytes, Maximum: " + MAX_CHUNK_SIZE_BYTES + " bytes"
            );
        }

        byte[] buffer = compressData(data, getCompressionType());

        int totalPayloadSize = buffer.length + 4 + 1;
        if (totalPayloadSize > MAX_CHUNK_SIZE_BYTES)
        {
            throw new ChunkTooLargeException(
                    "Compressed chunk payload exceeds maximum size. Size: " + totalPayloadSize +
                            " bytes, Maximum: " + MAX_CHUNK_SIZE_BYTES + " bytes"
            );
        }

        this.compressedData = buffer;
        setLength(buffer.length);
        int totalSize = buffer.length + 4 + 1;
        setPayloadLength(AnvilUtils.calculateSectorCount(totalSize) * AnvilUtils.SECTOR_SIZE);
    }

    /**
     * Gets full payload with sector alignment.
     *
     * @return full padded payload
     */
    public byte[] getFullPayload()
    {
        ByteBuffer buffer = ByteBuffer.allocate(5 + this.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(this.length);
        buffer.put(this.compressionType);
        buffer.put(this.compressedData);
        return AnvilUtils.padToSectorSize(buffer.array());
    }

    /**
     * Gets payload length.
     *
     * @return payload length
     */
    public int getPayloadLength()
    {
        return payloadLength;
    }

    /**
     * Sets payload length with sector alignment.
     *
     * @param payloadLength payload length in bytes
     */
    private void setPayloadLength(int payloadLength)
    {
        this.payloadLength = payloadLength;
    }

    /**
     * Gets length of chunk data.
     *
     * @return length of chunk data
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Sets length of compressed chunk data.
     *
     * @param length data length in bytes
     */
    private void setLength(int length)
    {
        this.length = length;
    }

    /**
     * Gets compression type of chunk data.
     *
     * @return compression type
     */
    public byte getCompressionType()
    {
        return compressionType;
    }

    /**
     * Gets (possibly compressed) chunk data.
     *
     * @return (possibly compressed) chunk data
     */
    public byte[] getData()
    {
        return compressedData;
    }

    /**
     * Gets decompressed chunk data.
     *
     * @return decompressed chunk data
     * @throws IOException if decompression fails
     */
    public byte[] getDecompressedData() throws IOException
    {
        return decompressData(getData(), getCompressionType());
    }

    /**
     * Decompresses chunk data using specified compression type.
     *
     * @param data compressed chunk data
     * @param compressionType compression type (1=GZip, 2=Zlib, 3=Uncompressed)
     * @return decompressed data
     * @throws IOException if decompression fails
     */
    public byte[] decompressData(byte[] data, byte compressionType) throws IOException
    {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data))
        {
            return switch (compressionType)
            {
                case 1 -> new GZIPInputStream(byteStream).readAllBytes();
                case 2 -> new InflaterInputStream(byteStream).readAllBytes();
                case 3 -> data;
                case 4 -> throw new IOException("LZ4 compression (type 4) is not yet implemented");
                case 127 -> throw new IOException("Custom compression (type 127) is not supported");
                default -> throw new IOException("Unknown compression type: " + compressionType +
                        ". Supported types: 1 (GZip), 2 (Zlib), 3 (Uncompressed)");
            };
        }
    }

    /**
     * Compresses chunk data using specified compression type.
     *
     * @param data uncompressed chunk data
     * @param compressionType compression type (1=GZip, 2=Zlib, 3=Uncompressed)
     * @return compressed data
     * @throws IOException if compression fails
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
                case 4:
                    throw new IOException("LZ4 compression (type 4) is not yet implemented");
                case 127:
                    throw new IOException("Custom compression (type 127) is not supported");
                default:
                    throw new IOException("Unknown compression type: " + compressionType +
                            ". Supported types: 1 (GZip), 2 (Zlib), 3 (Uncompressed)");
            }
            return byteStream.toByteArray();
        }
    }

    /**
     * Returns string representation of ChunkPayload.
     *
     * @return string representation
     */
    @Override
    public String toString()
    {
        return "ChunkData{" +
                "payloadLength=" + payloadLength +
                ", length=" + length +
                ", compressionType=" + compressionType +
                ", chunkData (Bytes)=" + compressedData.length +
                '}';
    }
}