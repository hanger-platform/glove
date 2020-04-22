/*
 * Copyright (c) 2020 Dafiti Group
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
package br.com.dafiti.mitt.decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class ZipDecoder implements Decoder {

    /**
     *
     * @param file
     * @return
     */
    @Override
    public File decode(File file) {
        File decompress = null;

        try {
            ZipEntry zipEntry;
            ZipInputStream zip = new ZipInputStream(new FileInputStream(file));

            while ((zipEntry = zip.getNextEntry()) != null) {
                decompress = new File(file.getParent() + "/" + zipEntry.getName());

                try (OutputStream outputStream = Files.newOutputStream(decompress.toPath())) {
                    IOUtils.copy(zip, outputStream);
                }
            }

            Files.delete(file.toPath());
        } catch (IOException ex) {
            Logger.getLogger(GZipDecoder.class.getName()).log(Level.SEVERE, "Fail decoding ZIP file " + file.getName(), ex);
        }
        return decompress;
    }
}
