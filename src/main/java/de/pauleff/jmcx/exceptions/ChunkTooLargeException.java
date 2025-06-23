package de.pauleff.jmcx.exceptions;

import static de.pauleff.jmcx.util.AnvilConstants.MAX_CHUNK_SIZE_FORMATTED;

/**
 * Exception thrown when a chunk exceeds the maximum size limit.
 *
 * @author Paul Ferlitz
 */
public class ChunkTooLargeException extends RuntimeException
{
    /**
     * Constructs a ChunkTooLargeException with a size-based message.
     *
     * @param dataSize the size that exceeded the limit
     */
    public ChunkTooLargeException(int dataSize)
    {
        super(String.format("Chunks written to MCA files cannot exceed the size of 1MiB (" + MAX_CHUNK_SIZE_FORMATTED + ")! Tried to write %d bytes.", dataSize));
    }

    /**
     * Constructs a ChunkTooLargeException with a custom message.
     *
     * @param message the custom error message
     */
    public ChunkTooLargeException(String message)
    {
        super(message);
    }

    /**
     * Constructs a ChunkTooLargeException with a custom message and cause.
     *
     * @param message the custom error message
     * @param cause   the cause of the exception
     */
    public ChunkTooLargeException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
