/*
 * Copyright (c) 2022 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.parquet.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.parquet.io.DelegatingSeekableInputStream;
import org.apache.parquet.io.SeekableInputStream;

/**
 * @author Mark Taylor from uk.ac.starlink.parquet.
 * @author Valdiney V GOMES
 */
public class InputFile implements org.apache.parquet.io.InputFile {

    private final File file;

    public InputFile(File file) {
        this.file = file;
    }

    @Override
    public long getLength() {
        return file.length();
    }

    @Override
    public SeekableInputStream newStream() throws IOException {
        return new FileSeekableInputStream(new FileInputStream(file));
    }

    private static class FileSeekableInputStream extends DelegatingSeekableInputStream {

        private final FileInputStream fileInputStream;
        private long position;

        public FileSeekableInputStream(FileInputStream fileInputStream) {
            super(fileInputStream);
            this.fileInputStream = fileInputStream;
        }

        @Override
        public long getPos() {
            return position;
        }

        @Override
        public void seek(long newPos) throws IOException {
            fileInputStream.skip(newPos - position);
            position = newPos;
        }

        @Override
        public int read() throws IOException {
            int b = fileInputStream.read();
            if (b >= 0) {
                position++;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int nb = fileInputStream.read(b, off, len);
            if (nb > 0) {
                position += nb;
            }
            return nb;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int nb = fileInputStream.read(b);
            if (nb > 0) {
                position += nb;
            }
            return nb;
        }

        @Override
        public long skip(long nreq) throws IOException {
            long nb = fileInputStream.skip(nreq);
            position += nb;
            return nb;
        }

        @Override
        public void readFully(byte[] b) throws IOException {
            super.readFully(b);
            position += b.length;
        }

        @Override
        public void readFully(byte[] b, int start, int len)
                throws IOException {
            super.readFully(b, start, len);
            position += len;
        }

        @Override
        public void readFully(ByteBuffer bbuf) throws IOException {
            int nb = bbuf.remaining();
            super.readFully(bbuf);
            position += nb;
        }
    }
}
