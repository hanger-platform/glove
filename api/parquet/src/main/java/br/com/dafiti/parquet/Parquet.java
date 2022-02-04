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
package br.com.dafiti.parquet;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.parquet.util.Parser;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;
import org.apache.avro.Schema;

/**
 *
 * @author Helio Leal
 */
public class Parquet {

    private static final Logger LOG = Logger.getLogger(Parquet.class.getName());

    public static void main(String[] args) {
        LOG.info("GLOVE - Parquet 1.12.0 converter started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("fo", "folder", "Folder where the files to be converted to parquet are", "", true, false)
                    .addParameter("s", "schema", "Avro schema to be used on conversion", "", true, false)
                    .addParameter("f", "filename", "(Optional) Filename, with wildcard if necessary, to be converted")
                    .addParameter("h", "header", "(Optional) Identifies the csv file has a header")
                    .addParameter("r", "replace", "(Optional) Identifies if csv files will be replaced to parquet files")
                    .addParameter("t", "thread", "(Optional) Limit of thread, default is 1", "1")
                    .addParameter("c", "compression", "(Optional) Identifies the compression to be applied, default is gzip", "gzip")
                    .addParameter("d", "delimiter", "(Optional) Delimiter of csv files, default is ;", ";")
                    .addParameter("q", "quote", "(Optional) Identifies the quote character, default is \"", "\"")
                    .addParameter("e", "escape", "(Optional) Identifies the quote escape character, default is \"", "\"")
                    .addParameter("k", "fieldkey", "(Optional) Unique key field, default is -1", "-1")
                    .addParameter("z", "duplicated", "(Optional) Identifies if duplicated is allowed, default is 0", "0")
                    .addParameter("m", "merge", "(Optional) Identifies if should merge existing files, default is 0", "0")
                    .addParameter("D", "debug", "(Optional) Show full log messages, default is 0", "0")
                    .addParameter("b", "bucket", "(Optional) Identifies the storage bucket", "")
                    .addParameter("M", "mode", "(Optional) Identifies the partition mode", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            // Filter specif files.
            FileFilter fileFilter = null;
            String fileName = "";

            //Get the file to convert.
            if (cli.hasParameter("filename")) {
                fileName = cli.getParameter("filename").replace("file:", "");
                fileFilter = new WildcardFileFilter(fileName);
            }

            //Get the folder. 
            String folderName = cli.getParameter("folder").replace("file:", "");
            File[] files = new File(folderName).listFiles(fileFilter);

            //Identifies if there are files to process.
            if (files != null) {

                //Define the number of threads.
                ExecutorService executor = Executors.newFixedThreadPool(cli.getParameterAsInteger("thread"));

                //Convert a csv file to parquet.
                Schema schema = new Parser(new File(cli.getParameter("schema"))).getAvroSchema();

                for (File file : files) {
                    if (file.isDirectory() || "csv".equals(FilenameUtils.getExtension(file.getName()))) {
                        executor.execute(
                                new Converter(
                                        file,
                                        cli.getParameter("compression"),
                                        cli.getParameter("delimiter").charAt(0),
                                        cli.getParameter("quote").charAt(0),
                                        cli.getParameter("escape").charAt(0),
                                        schema,
                                        cli.hasParameter("header"),
                                        cli.hasParameter("replace"),
                                        cli.getParameterAsInteger("fieldkey"),
                                        cli.getParameterAsInteger("duplicated") == 1,
                                        cli.getParameterAsInteger("merge") == 1,
                                        cli.getParameter("bucket"),
                                        cli.getParameter("mode"),
                                        cli.getParameterAsInteger("debug") == 1)
                        );
                    }
                }

                //Exit the thread executor.
                executor.shutdown();
            }

        } catch (DuplicateEntityException
                | IOException
                | IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "GLOVE - Parquet 1.12.0 converter fail: ", ex);
            System.exit(1);

        } finally {
            mitt.close();
        }
    }
}
