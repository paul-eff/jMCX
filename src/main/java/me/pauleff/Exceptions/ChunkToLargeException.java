package me.pauleff.Exceptions;

public class ChunkToLargeException extends RuntimeException
{
    public ChunkToLargeException(int size)
    {
        super(String.format("Chunks written to MCA files cannot exceed the size of 1MB (1048576 bytes)! Tried to write %d bytes.", size));
    }
}
