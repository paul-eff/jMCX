package me.pauleff;

import me.pauleff.Anvil.Chunk;
import me.pauleff.IO.AnvilReader;
import me.paulferlitz.IO.NBTReader;

import java.io.File;
import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException

    {
        AnvilReader aReader = new AnvilReader(new File("/path/to/world/entities/r.-1.0.mca"));
        NBTReader nReader;
        for (Chunk chunk : aReader.readAnvilFile().getChunks())
        {
            if (chunk.getPayload().getPayloadLength() > 0)
            {
                nReader = new NBTReader(NBTReader.byteArrayToDataInputStream(chunk.getPayload().getData()));
            }
        }
    }
}