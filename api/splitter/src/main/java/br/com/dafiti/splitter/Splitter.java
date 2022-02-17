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
package br.com.dafiti.splitter;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Fernando Saga
 */
public class Splitter {

    private static final Logger LOG = Logger.getLogger(Splitter.class.getName());

    public static void main(String[] args) {
        LOG.info("GLOVE - CSV splitter started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("f", "folder", "Folder where the files to be split are located", "", true, false)
                    .addParameter("fn", "filename", "(Optional) Filename, with wildcard if necessary, to be converted")
                    .addParameter("h", "header", "(Optional) Identifies the csv file has a header", "true")
                    .addParameter("r", "replace", "(Optional) Identifies whether csv files will be replaced by partitioned files", "true")
                    .addParameter("t", "thread", "(Optional) Limit of thread, default is 1", "1")
                    .addParameter("d", "delimiter", "(Optional) Delimiter of csv files, default is ;", ";")
                    .addParameter("q", "quote", "(Optional) Identifies the quote character, default is \"", "\"")
                    .addParameter("e", "escape", "(Optional) Identifies the quote escape character, default is \"", "\"")
                    .addParameter("D", "debug", "(Optional) Show full log messages, default is 0", "0")
                    .addParameter("p", "partition", "(Optional) Partition column", "0")
                    .addParameter("ss", "splitStrategy", "(Optional) Identifies the split strategy", "SECURE")
                    .addParameter("re", "readable", "(Optional) Identifies if partition name should be readable at runtime", "false");

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

                for (File file : files) {
                    String extension = FilenameUtils.getExtension(file.getName());

                    if (extension.equalsIgnoreCase("csv")) {
                        executor.execute(
                                new CSVSplitter(
                                        file,
                                        cli.getParameterAsInteger("partition"),
                                        cli.getParameter("delimiter").charAt(0),
                                        cli.getParameter("quote").charAt(0),
                                        cli.getParameter("escape").charAt(0),
                                        cli.getParameterAsBoolean("header"),
                                        cli.getParameterAsBoolean("replace"),
                                        cli.getParameterAsBoolean("readable"),
                                        cli.getParameter("splitStrategy")));
                    } else {
                        LOG.log(Level.SEVERE, "Unsupported file ".concat(extension));
                    }
                }

                //Exit the thread executor.
                executor.shutdown();
            }

        } catch (DuplicateEntityException
                | IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "GLOVE - CSV splitter fail: ", ex);
            System.exit(1);

        } finally {
            mitt.close();

            LOG.info("GLOVE - CSV splitter finished");
        }
    }
}
