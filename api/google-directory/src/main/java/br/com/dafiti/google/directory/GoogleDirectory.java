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
package br.com.dafiti.google.directory;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Helio Leal
 */
public class GoogleDirectory {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final Logger LOG = Logger.getLogger(GoogleDirectory.class.getName());

    public static void main(String[] args) {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            LOG.log(Level.INFO, "GLOVE - Google Directory Extractor started");

            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved in a JsonPath fashion", "", true, false)
                    .addParameter("s", "show_deleted", "(Optional) If set to 'true', retrieves the list of deleted users. (Default: 'false').", "false")
                    .addParameter("e", "max_results", "(Optional) Maximum number of results to return in a call. (Default: 50).", "50")
                    .addParameter("z", "customer", "(Optional) The unique ID for the customer's Google Workspace account. (Default: my_customer).", "my_customer")
                    .addParameter("q", "query", "(Optional) Query string for searching user fields.", "")
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Read the command line interface.
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Define the transport.
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            //Load client secrets.
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream((String) cli.getParameter("credentials"))));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, DirectoryScopes.all())
                    .setDataStoreFactory(new FileDataStoreFactory(new File(System.getProperty("user.home"), ".store/google_directory_api")))
                    .build();

            //Authorize.
            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

            //Construct the Google Directory service object.
            Directory service = new Directory.Builder(httpTransport, JSON_FACTORY, setHttpTimeout(credential))
                    .setApplicationName("Google Admin SDK Directory API")
                    .build();

            LOG.log(Level.INFO, "Retrieving users...");
            boolean process = true;
            String pageToken = "";
            do {
                com.google.api.services.directory.model.Users result = service.users().list()
                        .setCustomer(cli.getParameter("customer"))
                        .setShowDeleted(cli.getParameter("show_deleted"))
                        .setMaxResults(cli.getParameterAsInteger("max_results"))
                        .setQuery(cli.getParameter("query"))
                        .setPageToken(pageToken)
                        .execute();

                List<User> users = result.getUsers();

                if (users == null || users.isEmpty()) {
                    process = false;
                } else {
                    for (User user : users) {
                        String json = user.toString();
                        List record = new ArrayList();

                        mitt.getConfiguration()
                                .getOriginalFieldName()
                                .forEach(field -> {
                                    try {
                                        record.add(JsonPath.read(json, "$." + field));
                                    } catch (PathNotFoundException ex) {
                                        record.add("");
                                    }
                                });

                        mitt.write(record);
                    }
                }

                pageToken = result.getNextPageToken();

                if (pageToken == null || pageToken.isEmpty()) {
                    process = false;
                }

            } while (process);

        } catch (DuplicateEntityException | GeneralSecurityException | IOException ex) {

            LOG.log(Level.SEVERE, "Google Directory fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
            LOG.log(Level.INFO, "GLOVE - Google Directory Extractor finished");
        }

    }

    /**
     * Set the Google Directory API timeout.
     *
     * @param requestInitializer
     * @return
     */
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return (HttpRequest httpRequest) -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(5 * 60000);
            httpRequest.setReadTimeout(5 * 60000);
        };
    }
}
