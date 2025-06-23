package de.pauleff.jmcx.examples;

import de.pauleff.jmcx.api.*;
import de.pauleff.jmcx.util.ChunkFilter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Simple examples showing jMCX core functionality.
 * Demonstrates reading, writing, and basic chunk operations.
 *
 * @author Paul Ferlitz
 */
public class BasicUsageExamples
{
    /**
     * Example 1: Read and inspect a region file.
     */
    public static void readRegionExample()
    {
        try
        {
            File regionFile = new File("r.0.0.mca");
            
            // Read the region
            try (IAnvilReader reader = AnvilFactory.createReader(regionFile))
            {
                IRegion region = reader.readRegion();
                
                System.out.println("Region coordinates: (" + region.getX() + ", " + region.getZ() + ")");
                
                // Count non-empty chunks
                List<IChunk> nonEmptyChunks = ChunkFilter.filterNonEmpty(region.getChunks());
                System.out.println("Non-empty chunks: " + nonEmptyChunks.size() + " / 1024");
                
                // Get a specific chunk
                Optional<IChunk> chunk = region.getChunk(0, 0);
                if (chunk.isPresent())
                {
                    System.out.println("Chunk (0,0) data version: " + chunk.get().getDataVersion());
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not read region: " + e.getMessage());
        }
    }

    /**
     * Example 2: Copy a region file to a new location.
     */
    public static void copyRegionExample()
    {
        try
        {
            File sourceFile = new File("r.0.0.mca");
            File targetFile = new File("r.0.0_backup.mca");
            
            // Read source region
            IRegion region;
            try (IAnvilReader reader = AnvilFactory.createReader(sourceFile))
            {
                region = reader.readRegion();
            }
            
            // Write to target file
            try (IAnvilWriter writer = AnvilFactory.createWriter(targetFile))
            {
                writer.writeRegion(region);
                System.out.println("Region copied successfully");
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not copy region: " + e.getMessage());
        }
    }

    /**
     * Example 3: Filter chunks by different criteria.
     */
    public static void filterChunksExample()
    {
        try
        {
            File regionFile = new File("r.0.0.mca");
            
            try (IAnvilReader reader = AnvilFactory.createReader(regionFile))
            {
                IRegion region = reader.readRegion();
                List<IChunk> allChunks = region.getChunks();
                
                // Filter by coordinate range (spawn area)
                List<IChunk> spawnChunks = ChunkFilter.filterByCoordinateRange(allChunks, 0, 0, 15, 15);
                System.out.println("Chunks in spawn area (0,0 to 15,15): " + spawnChunks.size());
                
                // Filter chunks with ownable entities
                List<IChunk> ownableChunks = ChunkFilter.filterWithOwnables(allChunks);
                System.out.println("Chunks with ownable entities: " + ownableChunks.size());
                
                // Filter by data version (Minecraft 1.21+)
                List<IChunk> modernChunks = ChunkFilter.filterByDataVersion(allChunks, 4000, 5000);
                System.out.println("Modern chunks (1.21+): " + modernChunks.size());
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not filter chunks: " + e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        System.out.println("jMCX Basic Usage Examples");
        System.out.println("=========================");
        System.out.println();
        
        System.out.println("Note: These examples expect an MCA file named 'r.0.0.mca' in the current directory.");
        System.out.println("Copy an MCA file from your world/region/ folder to test these examples.");
        System.out.println();
        
        File testFile = new File("r.0.0.mca");
        if (!testFile.exists())
        {
            System.out.println("No test file found. Please copy an MCA file to 'r.0.0.mca' to run examples.");
            return;
        }
        
        System.out.println("Example 1: Reading a region file");
        readRegionExample();
        System.out.println();
        
        System.out.println("Example 2: Copying a region file");
        copyRegionExample();
        System.out.println();
        
        System.out.println("Example 3: Filtering chunks");
        filterChunksExample();
    }
}