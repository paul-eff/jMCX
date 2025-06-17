package de.pauleff.jmcx.examples;

import de.pauleff.jmcx.api.*;
import de.pauleff.jmcx.builder.RegionBuilder;
import de.pauleff.jmcx.util.ChunkFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Simple examples showing core jMCX functionality.
 * Learn the basics: read, write, filter, and build.
 */
public class BasicUsageExamples
{
    /**
     * Read an MCA file and examine its chunks.
     */
    public static void readExample() throws IOException
    {
        File mcaFile = new File("world/region/r.0.0.mca");

        try (IAnvilReader reader = AnvilFactory.createReader(mcaFile))
        {
            IRegion region = reader.readRegion();

            System.out.println("Region (" + region.getX() + ", " + region.getZ() + ")");
            System.out.println("Non-empty chunks: " + ChunkFilter.filterNonEmpty(region.getChunks()).size());
        }
    }

    /**
     * Write a region to a new file.
     */
    public static void writeExample() throws IOException
    {
        File inputFile = new File("world/region/r.0.0.mca");
        File outputFile = new File("world/region/r.0.0_copy.mca");

        // Read region
        IRegion region;
        try (IAnvilReader reader = AnvilFactory.createReader(inputFile))
        {
            region = reader.readRegion();
        }

        // Write region
        try (IAnvilWriter writer = AnvilFactory.createWriter(outputFile))
        {
            writer.writeRegion(region);
        }
    }

    /**
     * Filter chunks by different criteria.
     */
    public static void filterExample() throws IOException
    {
        File mcaFile = new File("world/region/r.0.0.mca");

        try (IAnvilReader reader = AnvilFactory.createReader(mcaFile))
        {
            IRegion region = reader.readRegion();
            List<IChunk> chunks = region.getChunks();

            // Filter by data version (modern chunks 1.21.5-1.21.6 Release Candidate 1)
            List<IChunk> modernChunks = ChunkFilter.filterByDataVersion(chunks, 4325, 4434);

            // Filter by coordinates (spawn area)
            List<IChunk> spawnChunks = ChunkFilter.filterByCoordinateRange(chunks, -5, -5, 5, 5);

            // Filter chunks with entities that have owners
            List<IChunk> ownableChunks = ChunkFilter.filterWithOwnables(chunks);

            System.out.println("Modern chunks: " + modernChunks.size());
            System.out.println("Spawn area chunks: " + spawnChunks.size());
            System.out.println("Chunks with ownables: " + ownableChunks.size());
        }
    }

    /**
     * Build a new region programmatically.
     */
    public static void buildExample() throws IOException
    {
        // Create new region from scratch
        IRegion newRegion = RegionBuilder.create()
                .withCoordinates(0, 0)
                .addEmptyChunk(0, 0)  // Chunk (0,0) belongs to region (0,0)
                .build();

        // Or load and modify existing region
        File existingFile = new File("world/region/r.0.0.mca");
        if (existingFile.exists())
        {
            IRegion modifiedRegion = RegionBuilder.fromFile(existingFile)
                    .addEmptyChunk(15, 15)
                    .build();

            System.out.println("Modified region has " +
                    ChunkFilter.filterNonEmpty(modifiedRegion.getChunks()).size() + " chunks");
        }
    }

    public static void main(String[] args)
    {
        System.out.println("jMCX Basic Examples");
        System.out.println("===================");

        try
        {
            // These examples require actual MCA files to work
            // Comment out if you don't have world files
            /*
            readExample();
            writeExample(); 
            filterExample();
            */

            buildExample();

        } catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
        }
    }
}