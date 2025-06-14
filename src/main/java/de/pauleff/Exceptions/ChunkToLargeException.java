package de.pauleff.Exceptions;

/**
 * Exception thrown when a chunk exceeds the maximum size limit.
 * According to the Minecraft Wiki, chunks are always less than 1MiB (1,048,576 bytes).
 */
public class ChunkToLargeException extends RuntimeException
{
    /**
     * Constructs a ChunkToLargeException with a size-based message.
     *
     * @param size the size that exceeded the limit
     */
    public ChunkToLargeException(int size)
    {
        super(String.format("Chunks written to MCA files cannot exceed the size of 1MiB (1,048,576 bytes)! Tried to write %d bytes.", size));
    }

    /**
     * Constructs a ChunkToLargeException with a custom message.
     *
     * @param message the custom error message
     */
    public ChunkToLargeException(String message)
    {
        super(message);
    }

    /**
     * Constructs a ChunkToLargeException with a custom message and cause.
     *
     * @param message the custom error message
     * @param cause the cause of the exception
     */
    public ChunkToLargeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
