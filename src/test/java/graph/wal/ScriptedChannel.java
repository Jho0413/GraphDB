package graph.wal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/** A channel over a real file whose writes, forces and truncations run a hook first, to fail or block them. */
final class ScriptedChannel extends FileChannel {

    interface Hook {
        void run() throws IOException;
    }

    private final FileChannel file;
    final AtomicInteger forces = new AtomicInteger();
    volatile Hook onWrite = () -> {};
    volatile Hook onForce = () -> {};
    volatile Hook onTruncate = () -> {};

    private ScriptedChannel(FileChannel file) {
        this.file = file;
    }

    static ScriptedChannel open(Path path) throws IOException {
        return new ScriptedChannel(FileChannel.open(path, READ, WRITE));
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        onWrite.run();
        return file.write(src, position);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        onWrite.run();
        return file.write(src);
    }

    @Override
    public void force(boolean metaData) throws IOException {
        forces.incrementAndGet();
        onForce.run();
        file.force(metaData);
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        onTruncate.run();
        file.truncate(size);
        return this;
    }

    // ============ Delegated unchanged ============

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return file.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return file.read(dsts, offset, length);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        onWrite.run();
        return file.write(srcs, offset, length);
    }

    @Override
    public long position() throws IOException {
        return file.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        file.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return file.size();
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        return file.transferTo(position, count, target);
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        return file.transferFrom(src, position, count);
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        return file.read(dst, position);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        return file.map(mode, position, size);
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        return file.lock(position, size, shared);
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return file.tryLock(position, size, shared);
    }

    @Override
    protected void implCloseChannel() throws IOException {
        file.close();
    }
}
