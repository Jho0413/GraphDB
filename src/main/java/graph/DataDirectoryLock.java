package graph;

import graph.exceptions.WalException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Exclusive ownership of a data directory, held through a lock on a dedicated {@code LOCK} file so that the
 * data files themselves stay readable (file locks are mandatory on Windows).
 */
final class DataDirectoryLock implements AutoCloseable {

    static final String LOCK_FILE_NAME = "LOCK";

    private final FileChannel channel;
    private final FileLock lock;

    private DataDirectoryLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    static DataDirectoryLock acquire(Path dataDirectory) {
        Path file = dataDirectory.resolve(LOCK_FILE_NAME);
        FileChannel channel = null;
        try {
            channel = FileChannel.open(file, CREATE, WRITE);
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new OverlappingFileLockException();
            }
            return new DataDirectoryLock(channel, lock);
        } catch (OverlappingFileLockException e) {
            closeQuietly(channel);
            throw new WalException("Data directory " + dataDirectory + " is already in use by another GraphDB instance");
        } catch (IOException e) {
            closeQuietly(channel);
            throw new WalException("Failed to lock data directory " + dataDirectory, e);
        }
    }

    @Override
    public void close() {
        try {
            lock.release();
        } catch (IOException ignored) {
        }
        closeQuietly(channel);
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
