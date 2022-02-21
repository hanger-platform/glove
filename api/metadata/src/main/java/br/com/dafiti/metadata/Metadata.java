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
package br.com.dafiti.metadata;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author Helio Leal
 */
public class Metadata {

    private static final Logger LOG = Logger.getLogger(Metadata.class.getName());

    public static void main(String[] args) {
        LOG.info("GLOVE - Metadata Inference started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("fo", "folder", "Folder where the files to be converted to parquet are", "", true, false)
                    .addParameter("d", "dialect", "(Optional) Identifies the metadata dialect", "spectrum")
                    .addParameter("s", "sample", "(Optional) Define the data sample to be analized at metadata extraction process", "100000")
                    .addParameter("d", "delimiter", "(Optional) Delimiter of csv files, default is ;", ";")
                    .addParameter("q", "quote", "(Optional) Identifies the quote character, default is \"", "\"")
                    .addParameter("e", "escape", "(Optional) Identifies the quote escape character, default is \"", "\"")
                    .addParameter("t", "thread", "(Optional) Limit of thread, default is 1", "1")
                    .addParameter("b", "field", "(Optional) Identifies the header fields of a csv file", "")
                    .addParameter("m", "metadata", "(Optional) Identifies the csv field metadata", "")
                    .addParameter("o", "output", "(Optional) Identifies the output path", "")
                    .addParameter("f", "filename", "(Optional) Filename, with wildcard if necessary.")
                    .addParameter("r", "reservedWords", "(Optional) Identifies the reserved words file list.");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            // Filter specific files.
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

            if (files != null) {
                //Define the number of threads.
                ExecutorService executor = Executors.newFixedThreadPool(cli.getParameterAsInteger("thread"));

                //Get the reserved words file.
                File reserverWordsFile = null;

                if (cli.hasParameter("reservedWords")) {
                    reserverWordsFile = new File(cli.getParameter("reservedWords").replace("file:", ""));
                }

                for (File file : files) {
                    executor.execute(
                            new Extractor(
                                    file,
                                    reserverWordsFile,
                                    cli.getParameter("delimiter").charAt(0),
                                    cli.getParameter("quote").charAt(0),
                                    cli.getParameter("escape").charAt(0),
                                    cli.getParameter("field"),
                                    cli.getParameter("metadata"),
                                    cli.getParameter("output"),
                                    cli.getParameter("dialect"),
                                    cli.getParameterAsInteger("sample")));
                }

                executor.shutdown();
            }

        } catch (DuplicateEntityException
                | IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "GLOVE - Metadata Inference fail: ", ex);
            System.exit(1);

        } finally {
            mitt.close();
        }
    }
}
