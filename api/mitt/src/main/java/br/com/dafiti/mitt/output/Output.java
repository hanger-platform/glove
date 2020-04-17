/*
 * Copyright (c) 2019 Dafiti Group
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
package br.com.dafiti.mitt.output;

import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.settings.ReaderSettings;
import br.com.dafiti.mitt.settings.WriterSettings;
import com.google.common.io.PatternFilenameFilter;
import com.univocity.parsers.csv.CsvWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class Output {

    private CsvWriter writer;
    private OutputProcessor outputProcessor;
    private final Configuration configuration;
    private final ReaderSettings readerSettings;
    private final WriterSettings writerSettings;

    /**
     *
     * @param configuration
     * @param readerSettings
     * @param writerSettings
     */
    public Output(
            Configuration configuration,
            ReaderSettings readerSettings,
            WriterSettings writerSettings) {

        this.configuration = configuration;
        this.readerSettings = readerSettings;
        this.writerSettings = writerSettings;
    }

    /**
     *
     * @return
     */
    public CsvWriter getWriter() {
        if (writer == null) {
            writer = this.getOutputProcessor().getWriter();
        }

        return writer;
    }

    /**
     *
     * @return
     */
    public OutputProcessor getOutputProcessor() {
        if (outputProcessor == null) {
            outputProcessor = new OutputProcessor(
                    configuration,
                    readerSettings,
                    writerSettings,
                    null,
                    false);
        }

        return outputProcessor;
    }

    /**
     *
     * @param files
     */
    public void write(File[] files) {
        File outputFile = writerSettings.getOutputFile();
        boolean parallel = FilenameUtils.getExtension(outputFile.getName()).isEmpty();

        //Identifies if there are trash. 
        if (outputFile.isDirectory()) {
            File[] garbage = outputFile.listFiles(new PatternFilenameFilter(".+\\.csv"));

            for (File file : garbage) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (IOException ex) {
                    Logger.getLogger(Output.class.getName()).log(Level.SEVERE, "Fail deleting file " + file.getName(), ex);
                }
            }
        }

        //Identifies if is a parallel process. 
        if (parallel) {
            ExecutorService executor = Executors.newFixedThreadPool(writerSettings.getThreadPool());

            //Process each input file. 
            for (File file : files) {
                //Empty file should be ignored. 
                if (file.length() != 0) {
                    executor.execute(new OutputProcessor(
                            configuration,
                            readerSettings,
                            writerSettings,
                            file,
                            true));
                }
            }

            executor.shutdown();

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                Logger.getLogger(Output.class.getName()).log(Level.SEVERE, "Fail waiting executor termination", ex);
            }
        } else {
            OutputProcessor processor = null;

            //Process each input file. 
            for (File file : files) {
                //Empty file should be ignored. 
                if (file.length() != 0) {
                    if (processor == null) {
                        processor = new OutputProcessor(
                                configuration,
                                readerSettings,
                                writerSettings,
                                file,
                                false);
                    }

                    processor.write();
                }
            }

            if (processor != null) {
                processor.close();
            }
        }
    }
}
