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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.parquet.io.PositionOutputStream;

/**
 *
 * @author Valdiney V GOMES
 */
public class OutputFile implements org.apache.parquet.io.OutputFile {

    private final OutputStream outputStream;

    /**
     *
     * @param file
     * @throws FileNotFoundException
     */
    public OutputFile(File file) throws FileNotFoundException {
        this.outputStream = new FileOutputStream(file);
    }

    private PositionOutputStream getPositionOutputstream() {
        return new PositionOutputStream() {
            int position = 0;

            @Override
            public long getPos() {
                return position;
            }

            @Override
            public void write(int b) throws IOException {
                position++;
                outputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                outputStream.write(b, off, len);
                position += len;
            }

            @Override
            public void flush() throws IOException {
                outputStream.flush();
            }

            @Override
            public void close() throws IOException {
                outputStream.close();
            }
        };
    }

    @Override
    public PositionOutputStream create(long blockSizeHint) {
        return this.getPositionOutputstream();
    }

    @Override
    public PositionOutputStream createOrOverwrite(long blockSizeHint) {
        return this.getPositionOutputstream();
    }

    @Override
    public boolean supportsBlockSize() {
        return false;
    }

    @Override
    public long defaultBlockSize() {
        return 0;
    }
}
