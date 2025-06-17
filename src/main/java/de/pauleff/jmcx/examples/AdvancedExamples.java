package de.pauleff.jmcx.examples;

import de.pauleff.jmcx.api.AnvilFactory;
import de.pauleff.jmcx.api.IAnvilReader;
import de.pauleff.jmcx.api.IChunk;
import de.pauleff.jmcx.api.IRegion;
import de.pauleff.jmcx.util.ChunkFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Advanced examples for processing multiple files and complex operations.
 * Shows batch processing and real-world workflows.
 */
public class AdvancedExamples
{
    /**
     * Process all MCA files in a world directory.
     */
    public static void batchProcessing() throws IOException
    {
        Path worldRegionDir = Paths.get("world/region");

        if (!Files.exists(worldRegionDir))
        {
            System.out.println("No world/region directory found");
            return;
        }

        int totalRegions = 0;
        int totalChunks = 0;
        int ownableChunks = 0;

        // Process all MCA files
        try (Stream<Path> files = Files.walk(worldRegionDir))
        {
            List<File> mcaFiles = files.filter(path -> path.toString().endsWith(".mca"))
                    .map(Path::toFile)
                    .toList();

            for (File mcaFile : mcaFiles)
            {
                try (IAnvilReader reader = AnvilFactory.createReader(mcaFile))
                {
                    IRegion region = reader.readRegion();
                    List<IChunk> chunks = ChunkFilter.filterNonEmpty(region.getChunks());

                    totalRegions++;
                    totalChunks += chunks.size();
                    ownableChunks += ChunkFilter.filterWithOwnables(chunks).size();

                    System.out.println("Processed " + mcaFile.getName() +
                            " - " + chunks.size() + " chunks");
                } catch (IOException e)
                {
                    System.err.println("Error with " + mcaFile.getName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("\nSummary:");
        System.out.println("Regions: " + totalRegions);
        System.out.println("Total chunks: " + totalChunks);
        System.out.println("Chunks with ownables: " + ownableChunks);
    }

    /**
     * Extract chunks from spawn area across multiple regions.
     * Shows filtering across multiple files.
     */
    public static void extractSpawnChunks() throws IOException
    {
        Path worldRegionDir = Paths.get("world/region");

        if (!Files.exists(worldRegionDir))
        {
            System.out.println("No world/region directory found");
            return;
        }

        int spawnRadius = 160; // blocks
        int extractedCount = 0;

        try (Stream<Path> files = Files.walk(worldRegionDir))
        {
            List<File> mcaFiles = files.filter(path -> path.toString().endsWith(".mca"))
                    .map(Path::toFile)
                    .toList();

            for (File mcaFile : mcaFiles)
            {
                try (IAnvilReader reader = AnvilFactory.createReader(mcaFile))
                {
                    IRegion region = reader.readRegion();

                    // Filter chunks in spawn area
                    List<IChunk> spawnChunks = ChunkFilter.filterByBlockCoordinates(
                            region.getChunks(), -spawnRadius, -spawnRadius, spawnRadius, spawnRadius);

                    if (!spawnChunks.isEmpty())
                    {
                        extractedCount += spawnChunks.size();
                        System.out.println("Found " + spawnChunks.size() +
                                " spawn chunks in " + mcaFile.getName());
                    }
                } catch (IOException e)
                {
                    System.err.println("Error with " + mcaFile.getName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Total spawn chunks found: " + extractedCount);
    }

    public static void main(String[] args)
    {
        System.out.println("jMCX Advanced Examples");
        System.out.println("======================");

        try
        {
            // These require a world/region directory with MCA files
            /*
            batchProcessing();
            extractSpawnChunks();
            */

            System.out.println("Uncomment method calls to run with real world data");
        } catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
        }
    }
}