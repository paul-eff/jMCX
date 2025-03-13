package me.pauleff.IO;

    import me.pauleff.Anvil.Chunk;
    import me.pauleff.Anvil.Region;

    import java.io.File;
    import java.io.IOException;
    import java.io.RandomAccessFile;

    /**
     * The AnvilReader class is responsible for reading Anvil files.
     */
    public class AnvilReader
    {
        private final File anvilFile;
        private final RandomAccessFile raf;

        /**
         * Constructs an AnvilReader object.
         *
         * @param anvilFile the Anvil file to read
         * @throws IOException if an I/O error occurs
         */
        public AnvilReader(File anvilFile) throws IOException
        {
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
         * Parses the filename to extract the region coordinates.
         *
         * @param filename the filename of the Anvil file
         * @return an array containing the x and z coordinates of the region
         */
        private int[] parseFilenameToCoordinates(String filename)
        {
            String[] parts = filename.split("\\.");
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new int[]{x, z};
        }
    }