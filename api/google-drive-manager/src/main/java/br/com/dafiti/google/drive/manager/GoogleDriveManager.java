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
import com.google.api.services.drive.model.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
                .addParameter("s", "id", "file id (can use google spreadsheet id)", "", true, false)
                .addParameter("t", "title", "New file title", "", true, false)
                .addParameter("f", "folder", "(Optional) Folder id, if null save file in my drive.", "")
                .addParameter("a", "action", "(Optional) Action on Google Drive; COPY is default", "COPY");

        //Command Line.
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Define google drive API manager.
        GoogleDriveApi api = new GoogleDriveApi().authenticate(cli.getParameter("credentials"));

        //Defines the action.
        switch (cli.getParameter("action")) {
            case "COPY":
                //Copy a file by its ID.
                File copyMetadata = api.copy(cli.getParameter("id"), cli.getParameter("title"), cli.getParameterAsList("folder", "\\+"));

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "File copied successfully, new file id: {0}", copyMetadata.getId());

                //Copy original file permissions to new file.        
                api.copyPermissions(cli.getParameter("id"), copyMetadata.getId());

                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Permissions copied successfully to: {0}", copyMetadata.getName());
                break;
            default:
                Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.WARNING, "Service is not available.");
        }

        Logger.getLogger(GoogleDriveManager.class.getName()).log(Level.INFO, "Google Drive manager finalized.");
    }
}
