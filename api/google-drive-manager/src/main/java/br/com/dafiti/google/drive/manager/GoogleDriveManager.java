/*
 * Copyright (c) 2021 Dafiti Group
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
package br.com.dafiti.google.drive.manager;

import br.com.dafiti.google.drive.manager.api.GoogleDriveApi;
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.services.drive.model.File;
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Flávia Lima
 * @author Helio Leal
 */
public class GoogleDriveManager {

    public static void main(String[] args) throws DuplicateEntityException, IOException, GeneralSecurityException {
        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "GLOVE - Google Drive manager started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, true)
                .addParameter("s", "id", "(Optional) file id (can use google spreadsheet id); Required for COPY, IMPORT and EXPORT", "")
                .addParameter("t", "title", "(Optional)  New file title; Required for COPY and UPLOAD", "")
                .addParameter("f", "folder", "(Optional) Folder id, if null save file in my drive.", "")
                .addParameter("a", "action", "(Optional) Action on Google Drive; COPY is default", "COPY")
                .addParameter("o", "output", "(Optional) Output file; Required for IMPORT", "")
                .addParameter("p", "properties", "(Optional) Reader properties.", "")
                .addParameter("f", "field", "(Optional) Fields to be extracted from the file, Required for IMPORT", "")
                .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field")
                .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                .addParameter("h", "input", "(Optional) Input file; Required for UPLOAD", "")
                .addParameter("n", "notification", "(Optional) Send notification email; COPY only; FALSE is default", "false")
                .addParameter("m", "mimetype", "(Optional) download file format; DOWNLOAD only; application/vnd.openxmlformats-officedocument.spreadsheetml.sheet is default", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        //Command Line.
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Define google drive API manager.
        GoogleDriveApi api = new GoogleDriveApi().authenticate(cli.getParameter("credentials"));

        //Defines the action.
        switch (cli.getParameter("action").toUpperCase()) {
            case "COPY":
                if ((cli.getParameter("title") != null) && (!cli.getParameter("title").isEmpty())) {
                    //Copy a file by its ID.
                    File copyMetadata = api.copy(cli.getParameter("id"), cli.getParameter("title"), cli.getParameterAsList("folder", "\\+"));

                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File copied successfully, new file id: {0}", copyMetadata.getId());

                    //Copy original file permissions to new file.        
                    api.copyPermissions(cli.getParameter("id"), copyMetadata.getId(), cli.getParameterAsBoolean("notification"));

                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Permissions copied successfully to: {0}", copyMetadata.getName());

                    if (cli.getParameterAsBoolean("notification")) {
                        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Notification is enabled. Users were notified by email.");
                    } else {
                        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Notification sending is disabled.");
                    }

                    //Create output file containing id of the new document.
                    if ((cli.getParameter("output") != null) && (!cli.getParameter("output").isEmpty())) {
                        java.io.File file = new java.io.File(cli.getParameter("output"));

                        FileUtils.writeStringToFile(file, copyMetadata.getId(), "UTF-8");

                        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Output file created successfully: {0}", cli.getParameter("output"));
                    }

                } else {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter title is empty. For COPY, it is required.");
                }

                break;
            case "IMPORT":
                if (Strings.isNullOrEmpty((cli.getParameter("output")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter output is empty. For IMPORT, it is required.");
                    break;
                }

                if (Strings.isNullOrEmpty((cli.getParameter("field")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter field is empty. For IMPORT, it is required.");
                    break;
                }

                //Defines output file.
                mitt.setOutputFile(cli.getParameter("output"));

                //Defines fields.
                mitt.getConfiguration()
                        .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                        .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                        .addCustomField("etl_load_date", new Now())
                        .addField(cli.getParameterAsList("field", "\\+"));

                //Download file from Google Drive.
                Path outputPath = api.download(cli.getParameter("id"));

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File downloaded successfully to {0}", outputPath.toString());

                //Defines the file properties.
                if (cli.getParameter("properties") != null) {
                    mitt.getReaderSettings().setProperties(cli.getParameter("properties"));
                }

                mitt.write(outputPath.toFile());

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File successfully written to {0}", cli.getParameter("output"));

                //Remove temporary path. 
                Files.delete(outputPath);

                break;
            case "UPLOAD":
                if (Strings.isNullOrEmpty((cli.getParameter("title")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter title is empty.");
                    break;
                }

                if (Strings.isNullOrEmpty((cli.getParameter("input")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter input is empty.");
                    break;
                }

                //Upload a file to Google Drive.
                File metadata = api.upload(cli.getParameter("title"), cli.getParameter("input"), cli.getParameterAsList("folder", "\\+"));

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File successfully uploaded, new file id: {0}", metadata.getId());

                break;
            case "EXPORT":
                if (Strings.isNullOrEmpty((cli.getParameter("id")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter id is empty.");
                    break;
                }

                if (Strings.isNullOrEmpty((cli.getParameter("output")))) {
                    Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.SEVERE, "Parameter output is empty.");
                    break;
                }

                //Export file of Google Drive.
                api.export(cli.getParameter("id"), cli.getParameter("mimetype"), cli.getParameter("output"));

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File exported successfully to {0}", cli.getParameter("output"));

                break;
            default:
                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.WARNING, "Service is not available.");
                break;
        }

        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Google Drive manager finalized.");
    }
}
