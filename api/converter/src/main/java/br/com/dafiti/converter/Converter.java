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
package br.com.dafiti.converter;

import br.com.dafiti.csv.CSVSplitter;
import br.com.dafiti.schema.Extractor;
import br.com.dafiti.orc.CSVToORC;
import br.com.dafiti.parquet.CSVToParquet;
import br.com.dafiti.schema.Parser;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.avro.Schema;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.orc.TypeDescription;

/**
 * Convert file format.
 *
 * @author Valdiney V GOMES
 */
public class Converter {

    public static final String DIALECT = "spectrum";
    public static final String TARGET = "parquet";
    public static final String COMPRESSION = "gzip";
    public static final String SPLIT_STRATEGY = "secure";
    public static final Character DELIMITER = ';';
    public static final Character QUOTE = '"';
    public static final Character QUOTE_ESCAPE = '"';

    public static Options options;

    /**
     * Constructor.
     */
    public Converter() {
        options = new Options();

        options.addOption("F", "folder", true, "Folder where the files to be converted to parquet are");
        options.addOption("f", "filename", true, "Filename, with wildcard if necessary, to be converted");
        options.addOption("s", "schema", true, "Avro schema to be used on conversion");
        options.addOption("D", "delimiter", true, "Delimiter ofd csv files");
        options.addOption("T", "target", true, "Identify the target format");
        options.addOption("r", "replace", false, "Identify if csv files will be replaced to parquet files");
        options.addOption("d", "debug", true, "Show full log messages");
        options.addOption("t", "thread", true, "Limit of thread");
        options.addOption("o", "output", true, "Identify the output path");
        options.addOption("q", "quote", true, "Identify the quote character");
        options.addOption("e", "escape", true, "Identify the quote escape character");
        options.addOption("H", "header", false, "Identify the csv file has a header");
        options.addOption("c", "field", true, "Identify the header fields of a csv file");
        options.addOption("S", "sample", true, "Define the data sample to be analized at metadata extraction process");
        options.addOption("i", "metadata", true, "Identify the csv field metadata");
        options.addOption("C", "compression", true, "Identify the compression to be applied");
        options.addOption("y", "dialect", true, "Identify the metadata dialect");
        options.addOption("h", "help", false, "Show help and usage message");
        options.addOption("p", "partition", true, "Partition column");
        options.addOption("k", "fieldkey", true, "Unique key field");
        options.addOption("d", "merge", true, "Identify if should merge existing files");
        options.addOption("z", "duplicated", true, "Identify if duplicated is allowed");
        options.addOption("b", "bucket", true, "Identify the storage bucket");
        options.addOption("m", "mode", true, "Identify the partition mode");
        options.addOption("w", "reservedWords", true, "Identify the reserved words file list");
        options.addOption("ps", "splitStrategy", true, "Identify the split strategy");
    }

    /**
     * Define the help information.
     */
    public void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("dafiti-parquet", options, true);
        System.exit(0);
    }

    /**
     * Convert a CSV<->Plain Parquet..
     *
     * @param args (See help)
     */
    public static void main(String[] args) {
        Converter converter;
        FileFilter fileFilter = null;
        File[] files = null;
        File reserverWordsFile = null;
        String target = TARGET;
        String dialect = DIALECT;
        String compression = COMPRESSION;
        Character delimiter = DELIMITER;
        Character quote = QUOTE;
        Character quoteEscape = QUOTE_ESCAPE;
        String splitStrategy = SPLIT_STRATEGY;
        String fileName = "";
        String field = "";
        String metadata = "";
        String folderName = "";
        String schemaFile = "";
        String outputFolder = "";
        String bucketPath = "";
        String mode = "";
        boolean replace;
        boolean header;
        boolean merge = false;
        boolean duplicated = false;
        boolean debug = false;
        int thread = 1;
        int fieldKeyPos = -1;
        int fieldPartitionPos = 0;
        int sample = 100000;

        //Configure Log4J.
        BasicConfigurator.configure();

        //Configure HADOOP_HOME.
        System.setProperty("hadoop.home.dir", "/");

        //Initiate the converter.
        converter = new Converter();

        try {
            Logger.getLogger(Converter.class).info("GLOVE - File converter started");

            //Parse the options.
            CommandLine line = new DefaultParser().parse(options, args);

            //Get the file to convert.
            if (line.hasOption("compression")) {
                compression = line.getOptionValue("compression").toLowerCase();
            }

            //Get the file to convert.
            if (line.hasOption("filename")) {
                fileName = line.getOptionValue("filename").replace("file:", "");
                fileFilter = new WildcardFileFilter(fileName);
            }

            //Get the reserved words file.
            if (line.hasOption("reservedWords")) {
                reserverWordsFile = new File(line.getOptionValue("reservedWords").replace("file:", ""));
            }

            //Get the folder. 
            if (line.hasOption("folder")) {
                folderName = line.getOptionValue("folder").replace("file:", "");
                files = new File(folderName).listFiles(fileFilter);
            }

            //Get the output folder. 
            if (line.hasOption("output")) {
                outputFolder = line.getOptionValue("output");
            }

            //Get the schema file.
            if (line.hasOption("schema")) {
                schemaFile = line.getOptionValue("schema");
            }

            //Get the delimiter.
            if (line.hasOption("delimiter")) {
                delimiter = line.getOptionValue("delimiter").charAt(0);
            }

            //Get the quote character.
            if (line.hasOption("quote")) {
                quote = line.getOptionValue("quote").charAt(0);
            }

            //Get the escape character.
            if (line.hasOption("escape")) {
                quoteEscape = line.getOptionValue("escape").charAt(0);
            }

            //Get the fields header.
            if (line.hasOption("field")) {
                field = line.getOptionValue("field");
            }

            //Get the fields metadata.
            if (line.hasOption("metadata")) {
                metadata = line.getOptionValue("metadata").toLowerCase();
            }

            //Get the target. 
            if (line.hasOption("target")) {
                target = line.getOptionValue("target").toLowerCase();
            }

            //Get the dialect. 
            if (line.hasOption("dialect")) {
                dialect = line.getOptionValue("dialect").toLowerCase();
            }

            //Get the data sample. 
            if (line.hasOption("sample")) {
                sample = Integer.valueOf(line.getOptionValue("sample"));
            }

            //Identify the log level.
            if (line.hasOption("debug")) {
                debug = (Integer.valueOf(line.getOptionValue("debug")) == 1);

                if (debug) {
                    Logger.getRootLogger().setLevel(Level.DEBUG);
                } else {
                    Logger.getRootLogger().setLevel(Level.ERROR);
                }
            }

            //Identify how many files should be converted simultaneously. 
            if (line.hasOption("thread")) {
                thread = Integer.valueOf(line.getOptionValue("thread"));
            }

            //Identify the csv partition column. 
            if (line.hasOption("partition")) {
                fieldPartitionPos = Integer.valueOf(line.getOptionValue("partition"));
            }

            //Identify the csv partition column. 
            if (line.hasOption("fieldkey")) {
                fieldKeyPos = Integer.valueOf(line.getOptionValue("fieldkey"));
            }

            //Identify if duplicated is allowed. 
            if (line.hasOption("duplicated")) {
                duplicated = (Integer.valueOf(line.getOptionValue("duplicated")) == 1);
            }

            //Identify if should merge existing files. 
            if (line.hasOption("merge")) {
                merge = (Integer.valueOf(line.getOptionValue("merge")) == 1);
            }

            //Get the storage bucket.
            if (line.hasOption("bucket")) {
                bucketPath = line.getOptionValue("bucket").toLowerCase();
            }

            //Get the partition mode. 
            if (line.hasOption("mode")) {
                mode = line.getOptionValue("mode").toLowerCase();
            }

            //Get the split strategy. 
            if (line.hasOption("splitStrategy")) {
                splitStrategy = line.getOptionValue("splitStrategy").toUpperCase();
            }

            //Identify if should remove the csv file. 
            replace = line.hasOption("replace");

            //Identify if the csv file has header. 
            header = line.hasOption("header");

            //Identify if should show help or convert files.
            if (line.hasOption("help") && args.length == 1) {
                converter.help();
            }

            //Identify if there are files to process.
            if (files != null) {
                //Define the number of threads.
                ExecutorService executor = Executors.newFixedThreadPool(thread);

                //Convert a file to a specified target.
                if (target.equalsIgnoreCase("parquet")) {
                    Schema schema = new Parser(new File(schemaFile)).getAvroSchema();

                    for (File file : files) {
                        if (file.isDirectory() || "csv".equals(FilenameUtils.getExtension(file.getName()))) {
                            executor.execute(
                                    new CSVToParquet(
                                            file,
                                            compression,
                                            delimiter,
                                            quote,
                                            quoteEscape,
                                            schema,
                                            header,
                                            replace,
                                            fieldKeyPos,
                                            duplicated,
                                            merge,
                                            bucketPath,
                                            mode,
                                            debug)
                            );
                        }
                    }

                } else if (target.equalsIgnoreCase("orc")) {
                    TypeDescription schema = new Parser(new File(schemaFile)).getOrcSchema();

                    for (File file : files) {
                        if (file.isDirectory() || "csv".equals(FilenameUtils.getExtension(file.getName()))) {
                            executor.execute(
                                    new CSVToORC(
                                            file,
                                            compression,
                                            delimiter,
                                            quote,
                                            quoteEscape,
                                            schema,
                                            header,
                                            replace,
                                            fieldKeyPos,
                                            duplicated,
                                            merge,
                                            bucketPath,
                                            mode,
                                            debug)
                            );
                        }
                    }

                } else if (target.equalsIgnoreCase("csv")) {
                    for (File file : files) {
                        String extension = FilenameUtils.getExtension(file.getName());

                        switch (extension) {
                            case "csv":
                                executor.execute(
                                        new CSVSplitter(
                                                file,
                                                fieldPartitionPos,
                                                delimiter,
                                                quote,
                                                quoteEscape,
                                                header,
                                                replace,
                                                splitStrategy));
                                break;
                            default:
                                Logger.getLogger(Converter.class).error("Unsupported file ".concat(extension));
                                break;
                        }
                    }

                } else if (target.equalsIgnoreCase("metadata")) {
                    for (File file : files) {
                        executor.execute(
                                new Extractor(
                                        file,
                                        delimiter,
                                        quote,
                                        quoteEscape,
                                        field,
                                        metadata,
                                        outputFolder,
                                        dialect,
                                        sample,
                                        reserverWordsFile));
                    }
                }

                //Exit the thread executor.
                executor.shutdown();
            }
        } catch (ParseException | NumberFormatException | IOException ex) {
            if (ex instanceof ParseException) {
                converter.help();
            }

            Logger.getLogger(Converter.class).error(ex);
            System.exit(1);
        }
    }
}
