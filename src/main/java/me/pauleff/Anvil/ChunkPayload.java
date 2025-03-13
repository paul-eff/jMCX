package me.pauleff.Anvil;

    import me.pauleff.Helpers;

    import java.io.ByteArrayInputStream;
    import java.io.IOException;
    import java.nio.ByteOrder;
    import java.util.Arrays;
    import java.util.zip.GZIPInputStream;
    import java.util.zip.InflaterInputStream;

    /**
     * The ChunkPayload class represents the actual chunk's data.
     */
    public class ChunkPayload
    {
        private final int payloadLength;
        private final int length;
        private final byte compressionType;
        private final byte[] data;

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
                this.data = decompressData(Arrays.copyOfRange(payload, 5, 5 + this.length - 1), this.compressionType);
            }
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
         * Gets the decompressed chunk data.
         *
         * @return the decompressed chunk data
         */
        public byte[] getData()
        {
            return data;
        }

        /**
         * Decompresses chunk data using the specified compression type.
         *
         * @param data            The compressed chunk data.
         * @param compressionType The compression type.
         * @return The decompressed data.
         * @throws IOException If decompression fails.
         */
        private byte[] decompressData(byte[] data, byte compressionType) throws IOException
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