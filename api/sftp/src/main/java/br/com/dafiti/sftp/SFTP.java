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
package br.com.dafiti.sftp;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.MD5;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.joda.time.LocalDate;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class SFTP {

    /**
     * SFTP File transfer.
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        Path outputPath = null;

        Session session = null;
        Channel channel = null;

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("H", "host", "SFTP Host", "", true, false)
                    .addParameter("j", "credentials", "Credentials file", "", true, false)
                    .addParameter("D", "directory", "SFTP directory", "", true, false)
                    .addParameter("O", "output", "Output file", "", true, false)
                    .addParameter("sd", "start_date", "Start date", "", true, false)
                    .addParameter("ed", "end_date", "End date", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from the file", "", true, false)
                    .addParameter("d", "delimiter", "(Optional) File delimiter; ';' as default", ";")
                    .addParameter("P", "port", "(Optional) SFTP port; 22 as default", "22")
                    .addParameter("r", "pattern", "(Optional) SFTP file pattern; *.csv as default", "*.csv")
                    .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Read the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutput(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new MD5((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Opens an SSH session with username and password authentication or private key. 
            JSch jsch = new JSch();

            if (credentials.get("private_key") != null) {
                jsch.addIdentity((String) credentials.get("private_key"));
            }

            session = jsch.getSession(
                    (String) credentials.get("user"),
                    cli.getParameter("host"),
                    cli.getParameterAsInteger("port")
            );

            //Defines the password.
            session.setPassword((String) credentials.get("password"));

            //Configures a SSH session. 
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            //Opens a SFTP channel. 
            channel = session.openChannel("sftp");
            channel.connect();

            //Define a SFTP directory as default. 
            ChannelSftp channelSftp = (ChannelSftp) channel;
            channelSftp.cd(cli.getParameter("directory"));

            //Lists all files that satisfies a pattern.
            Vector<ChannelSftp.LsEntry> list = channelSftp.ls(cli.getParameter("pattern"));

            //Defines the output path. 
            outputPath = Files.createTempDirectory("sftp_");

            //Transfers a file.
            for (ChannelSftp.LsEntry entry : list) {
                LocalDate updatedDate = LocalDate.fromDateFields(
                        new Date(entry.getAttrs().getMTime() * 1000L)
                );

                //Identifies if the file modification date is between start_date and end_date.
                if (updatedDate.compareTo(LocalDate.parse(cli.getParameter("start_date"))) >= 0
                        && updatedDate.compareTo(LocalDate.parse(cli.getParameter("end_date"))) <= 0) {

                    ArchiveEntry archiveEntry;
                    File outputFile = new File(outputPath.toString() + "/" + entry.getFilename());

                    //Transfer a file to local filesystem. 
                    channelSftp.get(
                            entry.getFilename(),
                            outputFile.getAbsolutePath(),
                            new Monitor()
                    );

                    //Identifies if file is compressed.
                    if ("|zip|gz|tar|".contains(FilenameUtils.getExtension(outputFile.getName()))) {
                        try (ArchiveInputStream archiveInputStream
                                = new ArchiveStreamFactory()
                                        .createArchiveInputStream(
                                                new BufferedInputStream(
                                                        new FileInputStream(outputFile)))) {
                                    while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
                                        File decompressedOutputFile = new File(outputPath.toString() + "/" + archiveEntry.getName());

                                        try (OutputStream outputStream = Files.newOutputStream(decompressedOutputFile.toPath())) {
                                            IOUtils.copy(archiveInputStream, outputStream);
                                        }
                                    }
                                }

                                //Remove compressed file. 
                                Files.delete(outputFile.toPath());
                    }
                }
            }

            //Writes the final file.
            mitt.write(outputPath.toFile(), "*", cli.getParameter("delimiter").charAt(0));

            //Remove temporary path. 
            Files.delete(outputPath);
        } catch (JSchException
                | SftpException
                | IOException
                | ParseException
                | DuplicateEntityException
                | ArchiveException ex) {

            Logger.getLogger(SFTP.class.getName()).log(Level.SEVERE, "SFTP Failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            if (session != null) {
                session.disconnect();
            }

            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
