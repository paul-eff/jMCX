package de.pauleff.jmcx.formats.anvil;

import de.pauleff.jmcx.api.IAnvilReader;
import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.core.Chunk;
import de.pauleff.jmcx.core.Location;
import de.pauleff.jmcx.core.Region;
import de.pauleff.jmcx.exceptions.ChunkTooLargeException;
import de.pauleff.jmcx.formats.FileFormat;
import de.pauleff.jmcx.util.AnvilUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static de.pauleff.jmcx.util.AnvilConstants.*;

/**
 * Implementation of {@link IAnvilReader} for reading Anvil region files.
 *
 * @author Paul Ferlitz
 */
public class AnvilReader implements IAnvilReader
{
    private static final int HEADER_SIZE = SECTOR_SIZE_BYTES + SECTOR_SIZE_BYTES;
    private static final int LOCATION_ENTRY_SIZE = 4;
    private static final int TIMESTAMP_ENTRY_SIZE = 4;
    private static final int MINIMUM_SECTOR_OFFSET = 2;

    private final File anvilFile;
    private final RandomAccessFile raf;

    /**
     * Constructs an AnvilReader object.
     *
     * @param anvilFile the Anvil file to read
     * @throws IOException if I/O error occurs or file format unsupported
     */
    public AnvilReader(File anvilFile) throws IOException
    {
        validateFileFormat(anvilFile);
        this.anvilFile = anvilFile;
        this.raf = new RandomAccessFile(anvilFile, "r");
        validateMcaHeader();
    }

    /**
     * Reads the complete region from file.
     *
     * @return {@link IRegion} representing the region
     * @throws IOException if I/O error occurs
     */
    @Override
    public IRegion readRegion() throws IOException
    {
        try
        {
            int[] coordinates = parseFilenameToCoordinates(anvilFile.getName());
            return readRegionWithValidation(coordinates[0], coordinates[1]);
        } finally
        {
            raf.close();
        }
    }

    /**
     * Reads a specific chunk from the region.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return {@link IChunk} if exists, empty otherwise
     * @throws IOException if I/O error occurs
     */
    @Override
    public Optional<IChunk> readChunk(int chunkX, int chunkZ) throws IOException
    {
        try
        {
            int chunkIndex = AnvilUtils.chunkCoordinatesToIndex(chunkX, chunkZ);

            Location location = readChunkLocation(chunkIndex);
            int timestamp = readChunkTimestamp(chunkIndex);

            if (location.getOffset() == 0)
            {
                return Optional.empty();
            }

            try
            {
                byte[] chunkData = readAndValidateChunkData(location);
                Chunk chunk = new Chunk(chunkIndex, location, timestamp, chunkData);
                return Optional.of(chunk);
            } catch (IOException | RuntimeException corruptionException)
            {
                System.err.printf("Warning: Corrupt chunk at coordinates (%d,%d), index %d: %s%n",
                        chunkX, chunkZ, chunkIndex, corruptionException.getMessage());
                System.err.printf("  Location: offset=%d, sectorCount=%d, timestamp=%d%n",
                        location.getOffset(), location.getSectorCount(), timestamp);

                return Optional.empty();
            }
        } catch (Exception e)
        {
            throw new IOException(
                    String.format("Critical failure reading chunk at coordinates (%d,%d): %s",
                            chunkX, chunkZ, e.getMessage()), e);
        }
    }


    /**
     * Gets region coordinates from filename.
     *
     * @return [regionX, regionZ] coordinates
     */
    @Override
    public int[] getRegionCoordinates()
    {
        return parseFilenameToCoordinates(anvilFile.getName());
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
     * Checks if file can be read.
     *
     * @return true if file exists and is readable
     */
    @Override
    public boolean canRead()
    {
        return anvilFile.exists() && anvilFile.canRead();
    }

    /**
     * Gets file size in bytes.
     *
     * @return file size
     * @throws IOException if I/O error occurs
     */
    @Override
    public long getFileSize() throws IOException
    {
        return anvilFile.length();
    }

    /**
     * Closes the file reader.
     *
     * @throws IOException if I/O error occurs
     */
    @Override
    public void close() throws IOException
    {
        if (raf != null)
        {
            raf.close();
        }
    }

    /**
     * Validates the MCA file header structure and basic integrity.
     *
     * @throws IOException if header validation fails or I/O error occurs
     */
    private void validateMcaHeader() throws IOException
    {
        long fileSize = raf.length();

        if (fileSize < HEADER_SIZE)
        {
            throw new IOException(
                    String.format("Invalid MCA file: file size %d bytes is less than required header size %d bytes",
                            fileSize, HEADER_SIZE)
            );
        }

        if (HEADER_SIZE != 2 * SECTOR_SIZE_BYTES)
        {
            throw new IOException(
                    String.format("Invalid header size: expected %d bytes (2 sectors), got %d bytes",
                            2 * SECTOR_SIZE_BYTES, HEADER_SIZE)
            );
        }

        if (fileSize % SECTOR_SIZE_BYTES != 0)
        {
            throw new IOException(
                    String.format("File size %d bytes is not sector-aligned (must be multiple of %d)",
                            fileSize, SECTOR_SIZE_BYTES)
            );
        }

        long maxReasonableFileSize = 256L * 1024 * 1024; // 256MB
        if (fileSize > maxReasonableFileSize)
        {
            throw new IOException(
                    String.format("File size %d bytes exceeds reasonable limit %d bytes",
                            fileSize, maxReasonableFileSize)
            );
        }

    }

    /**
     * Reads the region with comprehensive validation of all chunks.
     *
     * @param regionX the X coordinate of the region
     * @param regionZ the Z coordinate of the region
     * @return the validated Region object
     * @throws IOException if reading or validation fails
     */
    private IRegion readRegionWithValidation(int regionX, int regionZ) throws IOException
    {
        Location[] locations = readAndValidateLocationTable();

        int[] timestamps = readTimestampTable();

        java.util.List<IChunk> chunks = new java.util.ArrayList<>(CHUNKS_PER_REGION);

        int corruptChunkCount = 0;

        for (int i = 0; i < CHUNKS_PER_REGION; i++)
        {
            Location location = locations[i];
            int timestamp = timestamps[i];

            if (location.getOffset() == 0)
            {
                chunks.add(new Chunk(i, Location.createEmptyLocation(), 0, new byte[0]));
            } else
            {
                try
                {
                    byte[] chunkData = readAndValidateChunkData(location);
                    chunks.add(new Chunk(i, location, timestamp, chunkData));
                } catch (IOException | RuntimeException e)
                {
                    corruptChunkCount++;
                    int chunkX = regionX * 32 + (i % 32);
                    int chunkZ = regionZ * 32 + (i / 32);

                    System.err.printf("Warning: Corrupt chunk at index %d (chunk coordinates %d,%d): %s%n",
                            i, chunkX, chunkZ, e.getMessage());

                    chunks.add(new Chunk(i, Location.createEmptyLocation(), 0, new byte[0]));

                    System.err.printf("  Location: offset=%d, sectorCount=%d%n",
                            location.getOffset(), location.getSectorCount());
                    System.err.printf("  Timestamp: %d%n", timestamp);
                    if (e.getCause() != null)
                    {
                        System.err.printf("  Root cause: %s%n", e.getCause().getMessage());
                    }
                }
            }
        }

        if (corruptChunkCount > 0)
        {
            System.err.printf("Region (%d,%d): Found %d corrupt chunks, replaced with empty chunks%n",
                    regionX, regionZ, corruptChunkCount);
        }

        return new Region(regionX, regionZ, chunks);
    }

    /**
     * Reads and validates the location table from the MCA file header.
     *
     * @return array of Location objects for all CHUNKS_PER_REGION chunks
     * @throws IOException if reading or validation fails
     */
    private Location[] readAndValidateLocationTable() throws IOException
    {
        Location[] locations = new Location[CHUNKS_PER_REGION];
        long fileSize = raf.length();

        raf.seek(0);

        for (int i = 0; i < CHUNKS_PER_REGION; i++)
        {
            byte[] locationBytes = new byte[LOCATION_ENTRY_SIZE];
            int bytesRead = raf.read(locationBytes);

            if (bytesRead != LOCATION_ENTRY_SIZE)
            {
                throw new IOException(
                        String.format("Failed to read location entry %d: expected %d bytes, got %d",
                                i, LOCATION_ENTRY_SIZE, bytesRead)
                );
            }

            Location location = parseAndValidateLocation(locationBytes, i, fileSize);
            locations[i] = location;
        }

        return locations;
    }

    /**
     * Parses and validates a single location entry.
     *
     * @param locationBytes the 4-byte location entry
     * @param chunkIndex    the chunk index for error reporting
     * @param fileSize      the total file size for bounds checking
     * @return validated Location object
     * @throws IOException if validation fails
     */
    private Location parseAndValidateLocation(byte[] locationBytes, int chunkIndex, long fileSize) throws IOException
    {
        byte[] offsetBytes = Arrays.copyOfRange(locationBytes, 0, 3);
        byte[] sectorCountBytes = Arrays.copyOfRange(locationBytes, 3, 4);

        int offset = AnvilUtils.readInt(offsetBytes, ByteOrder.BIG_ENDIAN);
        int sectorCount = AnvilUtils.readInt(sectorCountBytes, ByteOrder.BIG_ENDIAN);

        if (offset != 0)
        {
            if (offset < MINIMUM_SECTOR_OFFSET)
            {
                throw new IOException(
                        String.format("Invalid chunk %d: sector offset %d is less than minimum %d (sectors 0-1 reserved for header)",
                                chunkIndex, offset, MINIMUM_SECTOR_OFFSET)
                );
            }

            if (sectorCount <= 0 || sectorCount > 255)
            {
                throw new IOException(
                        String.format("Invalid chunk %d: sector count %d must be between 1 and 255 (MCA format limit)",
                                chunkIndex, sectorCount)
                );
            }

            if (offset > 0xFFFFFF) // 24-bit limit
            {
                throw new IOException(
                        String.format("Invalid chunk %d: sector offset %d exceeds 24-bit limit %d (MCA format constraint)",
                                chunkIndex, offset, 0xFFFFFF)
                );
            }

            if (!AnvilUtils.isValidChunkPlacement(offset, sectorCount, fileSize))
            {
                throw new IOException(
                        String.format("Invalid chunk %d: placement at offset %d with %d sectors exceeds file size %d",
                                chunkIndex, offset, sectorCount, fileSize)
                );
            }

            long chunkStart = (long) offset * SECTOR_SIZE_BYTES;
            if (chunkStart % SECTOR_SIZE_BYTES != 0)
            {
                throw new IOException(
                        String.format("Invalid chunk %d: start position %d is not sector-aligned",
                                chunkIndex, chunkStart)
                );
            }
        } else if (sectorCount != 0)
        {
            throw new IOException(
                    String.format("Invalid chunk %d: empty chunk (offset=0) must have sector count 0, got %d",
                            chunkIndex, sectorCount)
            );
        }

        return new Location(locationBytes);
    }

    /**
     * Reads the timestamp table from the MCA file header.
     *
     * @return array of timestamps for all CHUNKS_PER_REGION chunks
     * @throws IOException if reading fails
     */
    private int[] readTimestampTable() throws IOException
    {
        int[] timestamps = new int[CHUNKS_PER_REGION];

        raf.seek(SECTOR_SIZE_BYTES);

        for (int i = 0; i < CHUNKS_PER_REGION; i++)
        {
            byte[] timestampBytes = new byte[TIMESTAMP_ENTRY_SIZE];
            int bytesRead = raf.read(timestampBytes);

            if (bytesRead != TIMESTAMP_ENTRY_SIZE)
            {
                throw new IOException(
                        String.format("Failed to read timestamp entry %d: expected %d bytes, got %d",
                                i, TIMESTAMP_ENTRY_SIZE, bytesRead)
                );
            }

            timestamps[i] = AnvilUtils.readInt(timestampBytes, ByteOrder.BIG_ENDIAN);

            if (timestamps[i] < 0)
            {
                throw new IOException(
                        String.format("Invalid timestamp for chunk %d: %d (timestamps cannot be negative)",
                                i, timestamps[i])
                );
            }

            long currentEpoch = Instant.now().getEpochSecond();
            long maxFutureEpoch = currentEpoch + (100L * 365 * 24 * 60 * 60); // 100 years
            if (timestamps[i] > maxFutureEpoch)
            {
                System.err.printf("Warning: chunk %d has timestamp %d which is more than 100 years in the future%n",
                        i, timestamps[i]);
            }
        }

        return timestamps;
    }

    /**
     * Reads a specific chunk's location from the location table.
     *
     * @param chunkIndex the index of the chunk (0-1023)
     * @return the Location object for the chunk
     * @throws IOException if reading fails
     */
    private Location readChunkLocation(int chunkIndex) throws IOException
    {
        if (chunkIndex < 0 || chunkIndex >= CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException(
                    String.format("Chunk index %d is out of range (0-%d)", chunkIndex, CHUNKS_PER_REGION - 1)
            );
        }

        long locationOffset = (long) chunkIndex * LOCATION_ENTRY_SIZE;
        raf.seek(locationOffset);

        byte[] locationBytes = new byte[LOCATION_ENTRY_SIZE];
        int bytesRead = raf.read(locationBytes);

        if (bytesRead != LOCATION_ENTRY_SIZE)
        {
            throw new IOException(
                    String.format("Failed to read location for chunk %d: expected %d bytes, got %d",
                            chunkIndex, LOCATION_ENTRY_SIZE, bytesRead)
            );
        }

        return parseAndValidateLocation(locationBytes, chunkIndex, raf.length());
    }

    /**
     * Reads a specific chunk's timestamp from the timestamp table.
     *
     * @param chunkIndex the index of the chunk (0-1023)
     * @return the timestamp for the chunk
     * @throws IOException if reading fails
     */
    private int readChunkTimestamp(int chunkIndex) throws IOException
    {
        if (chunkIndex < 0 || chunkIndex >= CHUNKS_PER_REGION)
        {
            throw new IllegalArgumentException(
                    String.format("Chunk index %d is out of range (0-%d)", chunkIndex, CHUNKS_PER_REGION - 1)
            );
        }

        long timestampOffset = SECTOR_SIZE_BYTES + (long) chunkIndex * TIMESTAMP_ENTRY_SIZE;
        raf.seek(timestampOffset);

        byte[] timestampBytes = new byte[TIMESTAMP_ENTRY_SIZE];
        int bytesRead = raf.read(timestampBytes);

        if (bytesRead != TIMESTAMP_ENTRY_SIZE)
        {
            throw new IOException(
                    String.format("Failed to read timestamp for chunk %d: expected %d bytes, got %d",
                            chunkIndex, TIMESTAMP_ENTRY_SIZE, bytesRead)
            );
        }

        return AnvilUtils.readInt(timestampBytes, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Reads and validates chunk data from the file.
     *
     * @param location the Location object containing offset and sector count
     * @return the validated chunk data
     * @throws IOException if reading or validation fails
     */
    private byte[] readAndValidateChunkData(Location location) throws IOException
    {
        int offset = location.getOffset();
        int sectorCount = location.getSectorCount();

        long filePosition = (long) offset * AnvilUtils.SECTOR_SIZE;
        int sectorDataSize = sectorCount * AnvilUtils.SECTOR_SIZE;

        raf.seek(filePosition);
        byte[] sectorData = new byte[sectorDataSize];
        int bytesRead = raf.read(sectorData);

        if (bytesRead != sectorDataSize)
        {
            throw new IOException(
                    String.format("Failed to read chunk sector data: expected %d bytes, got %d",
                            sectorDataSize, bytesRead)
            );
        }

        if (sectorData.length < 4)
        {
            throw new IOException("Chunk data too small: missing length field");
        }

        byte[] lengthBytes = Arrays.copyOfRange(sectorData, 0, 4);
        int chunkLength = AnvilUtils.readInt(lengthBytes, ByteOrder.BIG_ENDIAN);

        if (chunkLength <= 0)
        {
            throw new IOException(
                    String.format("Invalid chunk length: %d (must be positive)", chunkLength)
            );
        }

        if (chunkLength > MAX_CHUNK_SIZE_BYTES)
        {
            throw new ChunkTooLargeException(
                    String.format("Chunk length %d exceeds maximum size %d bytes", chunkLength, MAX_CHUNK_SIZE_BYTES)
            );
        }

        int totalChunkSize = chunkLength + 4; // +4 for length field itself
        if (totalChunkSize > sectorDataSize)
        {
            throw new IOException(
                    String.format("Chunk length field %d (+4 for length field = %d) exceeds available sector data %d",
                            chunkLength, totalChunkSize, sectorDataSize)
            );
        }

        int expectedSectorCount = AnvilUtils.calculateSectorCount(totalChunkSize);
        if (sectorCount != expectedSectorCount)
        {
            throw new IOException(
                    String.format("Sector count mismatch: location specifies %d sectors, but chunk size %d requires %d sectors",
                            sectorCount, totalChunkSize, expectedSectorCount)
            );
        }

        return sectorData;
    }

    /**
     * Validates that the file has a supported format (.mca only).
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if the file format is not supported
     */
    private void validateFileFormat(File file) throws IllegalArgumentException
    {
        String filename = file.getName().toLowerCase();
        if (!filename.endsWith(FileFormat.ANVIL.getExtension()))
        {
            throw new IllegalArgumentException(
                    "Unsupported file format. Currently only .mca (Anvil) files are supported, got: " + filename
            );
        }
    }

    /**
     * Gets file format.
     *
     * @return "mca" for Anvil format
     */
    @Override
    public String getFileFormat()
    {
        return "mca";
    }

    /**
     * Parses the filename to extract the region coordinates.
     * Delegates to AnvilUtils for consistent parsing logic.
     *
     * @param filename the filename of the region file
     * @return an array containing the x and z coordinates of the region
     * @throws IllegalArgumentException if the filename format is invalid
     */
    private int[] parseFilenameToCoordinates(String filename) throws IllegalArgumentException
    {
        return AnvilUtils.parseRegionFilename(filename);
    }
}