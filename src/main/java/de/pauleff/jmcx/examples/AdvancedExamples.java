package de.pauleff.jmcx.examples;

import de.pauleff.jmcx.api.*;
import de.pauleff.jmcx.builder.ChunkBuilder;
import de.pauleff.jmcx.util.ChunkFilter;
import de.pauleff.jnbt.api.ICompoundTag;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Advanced examples showing practical jMCX workflows.
 * Demonstrates chunk modification, batch processing, and NBT manipulation.
 *
 * @author Paul Ferlitz
 */
public class AdvancedExamples
{
    /**
     * Example 1: Modify chunk NBT data and save changes.
     */
    public static void modifyChunkExample()
    {
        try
        {
            File regionFile = new File("r.0.0.mca");
            
            // Read region
            IRegion region;
            try (IAnvilReader reader = AnvilFactory.createReader(regionFile))
            {
                region = reader.readRegion();
            }
            
            // Get a chunk and modify its NBT data
            Optional<IChunk> chunkOpt = region.getChunk(0, 0);
            if (chunkOpt.isPresent())
            {
                IChunk chunk = chunkOpt.get();
                
                // Access NBT data
                ICompoundTag nbtData = chunk.getNBTData();
                if (nbtData != null)
                {
                    // Example: Update the inhabitedTime value
                    long currentTime = nbtData.getLong("InhabitedTime");
                    nbtData.setLong("InhabitedTime", currentTime + 1000);
                    
                    // Save changes back to chunk
                    chunk.setNBTData(nbtData);
                    
                    System.out.println("Modified chunk (0,0) - updated InhabitedTime");
                }
            }
            
            // Write modified region back to file
            try (IAnvilWriter writer = AnvilFactory.createWriter(new File("r.0.0_modified.mca")))
            {
                writer.writeRegion(region);
                System.out.println("Saved modified region to r.0.0_modified.mca");
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not modify chunk: " + e.getMessage());
        }
    }

    /**
     * Example 2: Process multiple region files in a directory.
     */
    public static void batchProcessExample()
    {
        File regionDir = new File("region");
        
        if (!regionDir.exists() || !regionDir.isDirectory())
        {
            System.out.println("No 'region' directory found. Please create one with MCA files.");
            return;
        }
        
        File[] mcaFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
        if (mcaFiles == null || mcaFiles.length == 0)
        {
            System.out.println("No MCA files found in region directory.");
            return;
        }
        
        int totalChunks = 0;
        int processedFiles = 0;
        
        for (File mcaFile : mcaFiles)
        {
            try (IAnvilReader reader = AnvilFactory.createReader(mcaFile))
            {
                IRegion region = reader.readRegion();
                List<IChunk> nonEmptyChunks = ChunkFilter.filterNonEmpty(region.getChunks());
                
                totalChunks += nonEmptyChunks.size();
                processedFiles++;
                
                System.out.println(mcaFile.getName() + " - " + nonEmptyChunks.size() + " chunks");
            }
            catch (IOException e)
            {
                System.err.println("Error processing " + mcaFile.getName() + ": " + e.getMessage());
            }
        }
        
        System.out.println("Processed " + processedFiles + " files with " + totalChunks + " total chunks");
    }

    /**
     * Example 3: Create and copy chunks between regions.
     */
    public static void chunkCopyExample()
    {
        try
        {
            File sourceFile = new File("r.0.0.mca");
            
            // Read source region
            IRegion sourceRegion;
            try (IAnvilReader reader = AnvilFactory.createReader(sourceFile))
            {
                sourceRegion = reader.readRegion();
            }
            
            // Get a chunk from source
            Optional<IChunk> sourceChunkOpt = sourceRegion.getChunk(0, 0);
            if (sourceChunkOpt.isEmpty())
            {
                System.out.println("Source chunk (0,0) not found");
                return;
            }
            
            IChunk sourceChunk = sourceChunkOpt.get();
            
            // Create a copy of the chunk at new coordinates
            IChunk copiedChunk = ChunkBuilder.fromChunk(sourceChunk)
                    .withCoordinates(1, 1)  // New position
                    .withCurrentTimestamp()
                    .build();
            
            // Replace chunk in region
            sourceRegion.replaceChunk(copiedChunk);
            
            // Save region with copied chunk
            try (IAnvilWriter writer = AnvilFactory.createWriter(new File("r.0.0_with_copy.mca")))
            {
                writer.writeRegion(sourceRegion);
                System.out.println("Copied chunk from (0,0) to (1,1) and saved to r.0.0_with_copy.mca");
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not copy chunk: " + e.getMessage());
        }
    }

    /**
     * Example 4: Find and analyze chunks with specific properties.
     */
    public static void analyzeChunksExample()
    {
        try
        {
            File regionFile = new File("r.0.0.mca");
            
            try (IAnvilReader reader = AnvilFactory.createReader(regionFile))
            {
                IRegion region = reader.readRegion();
                List<IChunk> allChunks = region.getChunks();
                
                // Find chunks with ownable entities
                List<IChunk> ownableChunks = ChunkFilter.filterWithOwnables(allChunks);
                System.out.println("Found " + ownableChunks.size() + " chunks with ownable entities");
                
                // Analyze data versions
                int oldestVersion = Integer.MAX_VALUE;
                int newestVersion = Integer.MIN_VALUE;
                int analyzedChunks = 0;
                
                for (IChunk chunk : ChunkFilter.filterNonEmpty(allChunks))
                {
                    int version = chunk.getDataVersion();
                    oldestVersion = Math.min(oldestVersion, version);
                    newestVersion = Math.max(newestVersion, version);
                    analyzedChunks++;
                }
                
                if (analyzedChunks > 0)
                {
                    System.out.println("Analyzed " + analyzedChunks + " chunks:");
                    System.out.println("  Oldest data version: " + oldestVersion);
                    System.out.println("  Newest data version: " + newestVersion);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not analyze chunks: " + e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        System.out.println("jMCX Advanced Examples");
        System.out.println("======================");
        System.out.println();
        
        File testFile = new File("r.0.0.mca");
        if (!testFile.exists())
        {
            System.out.println("No test file found. Please copy an MCA file to 'r.0.0.mca' to run examples.");
            System.out.println("For batch processing example, create a 'region' folder with MCA files.");
            return;
        }
        
        System.out.println("Example 1: Modifying chunk NBT data");
        modifyChunkExample();
        System.out.println();
        
        System.out.println("Example 2: Batch processing multiple files");
        batchProcessExample();
        System.out.println();
        
        System.out.println("Example 3: Copying chunks between positions");
        chunkCopyExample();
        System.out.println();
        
        System.out.println("Example 4: Analyzing chunk properties");
        analyzeChunksExample();
    }
}