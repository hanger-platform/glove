/*
 * Copyright (c) 2018 Dafiti Group
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
package br.com.dafiti.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

/**
 * This class read a plain csv and write into a plain csv file.
 *
 * @author Valdiney V GOMES
 */
public class CSVSplitter implements Runnable {

    private final File csvFile;
    private final int partitionColumn;
    private final Character delimiter;
    private final Character quote;
    private final Character quoteEscape;
    private final boolean header;
    private final boolean replace;

    /**
     * Constructor.
     *
     * @param csvFile Orc File
     * @param fieldPartition Partition field
     * @param delimiter File delimiter
     * @param quote File quote.
     * @param quoteEscape File escape.
     * @param header Identify if the file has header.
     * @param replace Identify if should replace the orignal file.
     */
    public CSVSplitter(
            File csvFile,
            int fieldPartition,
            Character delimiter,
            Character quote,
            Character quoteEscape,
            boolean header,
            boolean replace) {

        this.csvFile = csvFile;
        this.partitionColumn = fieldPartition;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.replace = replace;
        this.header = header;
    }

    /**
     * Convert a csv file to a csv file.
     */
    @Override
    public void run() {
        Logger.getLogger(this.getClass()).info("Converting CSV to CSV");

        try {
            String part = "";
            int lineNumber = 0;
            HashMap<String, BufferedWriter> partitions = new HashMap<>();
            LineIterator lineIterator = FileUtils.lineIterator(csvFile, "UTF-8");

            try {
                while (lineIterator.hasNext()) {
                    String line = lineIterator.nextLine();

                    if (!(lineNumber == 0 && header)) {
                        String[] split = line.split(delimiter.toString());

                        if (split[split.length - 1].startsWith("\"") && !split[split.length - 1].endsWith("\"")) {
                            part = line;
                        } else {
                            if (!part.isEmpty()) {
                                line = part + line;
                                part = "";
                                split = line.split(delimiter.toString());
                            }
                        }

                        if (part.isEmpty()) {
                            String partition = split[partitionColumn].replaceAll("\\W", "");

                            if (!partitions.containsKey(partition)) {
                                String partitionPath = csvFile.getParent() + "/" + partition;
                                Files.createDirectories(Paths.get(partitionPath));
                                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(partitionPath + "/" + UUID.randomUUID() + ".csv"));

                                partitions.put(partition, bufferedWriter);
                            }

                            partitions.get(partition).append(line + "\r\n");
                        }
                    }

                    lineNumber++;
                }
            } finally {
                LineIterator.closeQuietly(lineIterator);
            }

            //Flush and close the output stream.
            partitions.forEach((k, v) -> {
                try {
                    v.flush();
                    v.close();
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass()).error("Error [" + ex + "] closing writer");
                    System.exit(1);
                }
            });

            //Identify if should remove csv file. 
            if (replace) {
                csvFile.delete();
            }
        } catch (IOException ex) {
            Logger.getLogger(this.getClass()).error("Error [" + ex + "] converting CSV to CSV");
            System.exit(1);
        }
    }
}
