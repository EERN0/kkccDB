package kangc.kkccdb.backend.manager.data.logger;

import kangc.kkccdb.backend.common.Parser;
import kangc.kkccdb.utils.Panic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public interface Logger {
    void log(byte[] data);

    void truncate(long x) throws Exception;

    byte[] next();

    void rewind();

    void close();

    public static Logger create(String path) {
        File f = new File(path + LoggerImpl.LOG_SUFFIX);
        try {
            if (!f.createNewFile()) {
                Panic.panic(new RuntimeException("文件已存在!"));
            }
        } catch (Exception e) {
            Panic.panic(e);
        }
        if (!f.canRead() || !f.canWrite()) {
            Panic.panic(new RuntimeException("无权限读写文件"));
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }

        ByteBuffer buf = ByteBuffer.wrap(Parser.int2Byte(0));
        try {
            fc.position(0);
            fc.write(buf);
            fc.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }

        return new LoggerImpl(raf, fc, 0);
    }

    public static Logger open(String path) {
        File f = new File(path + LoggerImpl.LOG_SUFFIX);
        if (!f.exists()) {
            Panic.panic(new RuntimeException("文件不存在!"));
        }
        if (!f.canRead() || !f.canWrite()) {
            Panic.panic(new RuntimeException("无权限读写文件"));
        }

        FileChannel fc = null;
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(f, "rw");
            fc = raf.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }

        LoggerImpl lg = new LoggerImpl(raf, fc);
        lg.init();

        return lg;
    }
}

