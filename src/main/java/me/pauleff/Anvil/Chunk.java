package me.pauleff.Anvil;

    import me.paulferlitz.IO.NBTReader;
    import me.paulferlitz.NBTTags.Tag_Compound;
    import me.paulferlitz.NBTTags.Tag_Int_Array;

    import java.io.IOException;

    /**
     * The Chunk class represents a chunk in the Anvil file format.
     */
    public class Chunk
    {
        private final int x;
        private final int z;
        private final int dataVersion;
        private final int index;
        private final Location location;
        private final int timestamp;
        private final ChunkPayload payload;

        /**
         * Constructs a Chunk object.
         *
         * @param index the index of the chunk
         * @param location the location of the chunk in the region file
         * @param timestamp the timestamp of the chunk
         * @param payload the byte array representing the chunk payload
         * @throws IOException if an I/O error occurs during payload processing
         */
        public Chunk(int index, Location location, int timestamp, byte[] payload) throws IOException
        {
            this.index = index;
            this.location = location;
            this.timestamp = timestamp;
            this.payload = new ChunkPayload(payload);
            // If there is an actual payload to process
            if (this.payload.getPayloadLength() > 0)
            {
                NBTReader reader = new NBTReader(NBTReader.byteArrayToDataInputStream(this.payload.getData()));
                Tag_Compound root = reader.read();
                // If Position is found, we are dealing with an entity file, else it is an actual world region file
                if (root.getTagByName("Position") instanceof Tag_Int_Array position)
                {
                    this.x = position.getData()[0];
                    this.z = position.getData()[1];
                } else
                {
                    this.x = (int) root.getTagByName("xPos").getData();
                    this.z = (int) root.getTagByName("zPos").getData();
                }
                this.dataVersion = (int) root.getTagByName("DataVersion").getData();
            } else
            {
                this.x = 0;
                this.z = 0;
                this.dataVersion = 0;
            }
        }

        /**
         * Gets the location of the chunk.
         *
         * @return the location of the chunk
         */
        public Location getLocation()
        {
            return location;
        }

        /**
         * Gets the timestamp of the chunk.
         *
         * @return the timestamp of the chunk
         */
        public int getTimestamp()
        {
            return timestamp;
        }

        /**
         * Gets the payload of the chunk.
         *
         * @return the payload of the chunk
         */
        public ChunkPayload getPayload()
        {
            return payload;
        }

        /**
         * Gets the x-coordinate of the chunk.
         *
         * @return the x-coordinate of the chunk
         */
        public int getX()
        {
            return x;
        }

        /**
         * Gets the z-coordinate of the chunk.
         *
         * @return the z-coordinate of the chunk
         */
        public int getZ()
        {
            return z;
        }

        /**
         * Gets the data version of the chunk.
         *
         * @return the data version of the chunk
         */
        public int getDataVersion()
        {
            return dataVersion;
        }

        /**
         * Gets the index of the chunk.
         *
         * @return the index of the chunk
         */
        public int getIndex()
        {
            return index;
        }

        /**
         * Converts the chunk coordinates to region coordinates.
         *
         * @return an array containing the region x and z coordinates
         */
        public int[] chunkToRegionCoordinate()
        {
            int regionX = (int) Math.floor(this.x / 32.0f);
            int regionZ = (int) Math.floor(this.z / 32.0f);
            return new int[]{regionX, regionZ};
        }

        /**
         * Gets the starting block coordinates of the chunk.
         *
         * @return an array containing the x and z starting block coordinates
         */
        public int[] chunkStartingBlockCoordinate()
        {
            int[] regionCoordinate = chunkToRegionCoordinate();
            int chunkX = regionCoordinate[0] + (this.index % 32 * 16);
            int chunkZ = regionCoordinate[1] + (this.index % 32 * 16);
            return new int[]{chunkX, chunkZ};
        }

        /**
         * Returns a string representation of the Chunk object.
         *
         * @return a string representation of the Chunk object
         */
        @Override
        public String toString()
        {
            return "Chunk{" +
                    "location=" + location +
                    ", timestamp=" + timestamp +
                    ", chunkData (Bytes)=" + payload.getPayloadLength() +
                    '}';
        }
    }