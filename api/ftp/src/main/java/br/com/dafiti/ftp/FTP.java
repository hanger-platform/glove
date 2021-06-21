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
package br.com.dafiti.ftp;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.joda.time.LocalDate;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class FTP {

    /**
     * FTP File transfer.
     *
     * @param args cli parameteres provided by command line.
     * @throws java.text.ParseException
     */
    public static void main(String[] args) throws java.text.ParseException {
        Logger.getLogger(FTP.class.getName()).info("Glove - FTP Extractor started.");

        //Path where FTP files will stored.
        Path outputPath = null;

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines a FTP client.
        FTPClient ftpClient = new FTPClient();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("d", "directory", "FTP directory", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from input file", "", true, false)
                    .addParameter("de", "delimiter", "(Optional) File delimiter; ';' as default", ";")
                    .addParameter("p", "pattern", "(Optional) FTP file pattern; *.csv as default", "*.csv")
                    .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                    .addParameter("ps", "passive", "(Optional) Define the connection mode. Default is true (passive)", "true")
                    .addParameter("en", "encode", "(Optional) Encode file.", "auto")
                    .addParameter("pr", "properties", "(Optional) Reader properties", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Connects to FTP server.
            ftpClient.connect((String) credentials.get("host"), Integer.parseInt((String) credentials.get("port")));
            ftpClient.login((String) credentials.get("user"), (String) credentials.get("password"));

            //Defines a FTP directory as default. 
            ftpClient.changeWorkingDirectory(cli.getParameter("directory"));

            //Define the buffer size.
            ftpClient.setBufferSize(5 * (1024 * 1024));

            //Defines de ftp connection mode.
            if (cli.getParameterAsBoolean("passive")) {
                ftpClient.enterLocalPassiveMode();
            } else {
                ftpClient.enterLocalActiveMode();
            }

            //Lists all files that satisfies a pattern.
            FTPFile[] ftpFiles = ftpClient.listFiles(cli.getParameter("pattern"));

            //Defines the output path.
            outputPath = Files.createTempDirectory("ftp_");

            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.getSize() > 0) {
                    LocalDate updatedDate = LocalDate
                            .fromCalendarFields(ftpFile.getTimestamp());

                    //Identifies if the file modification date is between start_date and end_date.
                    if (updatedDate.compareTo(LocalDate.parse(cli.getParameter("start_date"))) >= 0
                            && updatedDate.compareTo(LocalDate.parse(cli.getParameter("end_date"))) <= 0) {

                        Logger.getLogger(FTP.class.getName()).log(Level.INFO, "Transfering: {0} of {1}", new Object[]{ftpFile.getName(), updatedDate});

                        //Defines output file.
                        File outputFile = new File(outputPath.toString() + "/" + ftpFile.getName());

                        //Downloads file from FTP.
                        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                            //Downloads file from FTP.
                            if (!ftpClient.retrieveFile(ftpFile.getName(), outputStream)) {
                                Logger.getLogger(FTP.class.getName()).log(Level.SEVERE, "Fail downloading file {0} ", new Object[]{ftpFile.getName()});
                            }
                        }
                    }
                }
            }

            Logger.getLogger(FTP.class.getName()).log(Level.INFO, "Writing output file to: {0}", cli.getParameter("output"));

            //Write to the output.
            mitt.getReaderSettings().setDelimiter(cli.getParameter("delimiter").charAt(0));
            
            if (cli.getParameter("properties") != null) {
                mitt.getReaderSettings().setProperties(cli.getParameter("properties"));
            }
            
            mitt.getReaderSettings().setEncode(cli.getParameter("encode"));
            mitt.write(outputPath.toFile(), "*");
            FileUtils.deleteDirectory(outputPath.toFile());
        } catch (DuplicateEntityException
                | IOException
                | ParseException ex) {

            Logger.getLogger(FTP.class.getName()).log(Level.SEVERE, "FTP Failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            try {
                //Disconnects from FTP server.
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                Logger.getLogger(FTP.class.getName()).log(Level.SEVERE, "FTP disconnect failure: ", ex);
            }
        }

        Logger.getLogger(FTP.class.getName()).info("Glove - FTP Extractor finalized.");
    }
}
