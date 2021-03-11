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
package br.com.dafiti.google.ads;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v6.services.GoogleAdsRow;
import com.google.ads.googleads.v6.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v6.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v6.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.pathtemplate.ValidationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.util.JsonFormat;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Helio Leal
 * @author Valdiney V GOMES
 */
public class GoogleAds {

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "GLOVE - Google Ads (v6) Extractor started");

            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("r", "type", "Ads report type", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("o", "field", "Fields", "", true, false)
                    .addParameter("m", "manager", "Manager account ID", "")
                    .addParameter("z", "customer", "The ID of the customer being queried", "")
                    .addParameter("f", "filter", "(Optional) Filter of query")
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one", "")
                    .addParameter("d", "debug", "(Optional)  Debug mode. false as default", "false");

            //Defines the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\,"));

            //Get original fields.
            List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

            //Defines an authenticated client for Google ads api.
            GoogleAdsClient googleAdsClient = GoogleAdsClient
                    .newBuilder()
                    .fromPropertiesFile(new File(cli.getParameter("credentials"))).
                    setLoginCustomerId(Long.parseLong(cli.getParameter("manager")))
                    .build();

            try (GoogleAdsServiceClient googleAdsServiceClient
                    = googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
                ArrayList<String> accounts = new ArrayList();

                //Retrieves all child accounts of the manager, don't bring manager account.                
                String queryAccounts = "SELECT customer_client.manager, customer_client.id FROM customer_client WHERE customer_client.level <= 1 AND customer_client.manager = false";

                // Constructs the SearchGoogleAdsStreamRequest.
                SearchGoogleAdsStreamRequest searchGoogleAdsStreamRequest
                        = SearchGoogleAdsStreamRequest.newBuilder()
                                .setCustomerId(cli.getParameter("customer"))
                                .setQuery(queryAccounts)
                                .build();

                // API call returns a stream
                ServerStream<SearchGoogleAdsStreamResponse> searchGoogleAdsStreamResponse
                        = googleAdsServiceClient.searchStreamCallable().call(searchGoogleAdsStreamRequest);

                // Iterates through the results in the stream response.
                for (SearchGoogleAdsStreamResponse response : searchGoogleAdsStreamResponse) {
                    for (GoogleAdsRow googleAdsRow : response.getResultsList()) {

                        //Convert row to json.
                        String json = JsonFormat
                                .printer()
                                .preservingProtoFieldNames()
                                .print(googleAdsRow);

                        //Feed children accounts
                        accounts.add(JsonPath.read(json, "$.customer_client.id"));
                    }
                }

                Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "Retrieving data from {0} accounts", accounts.size());

                for (String account : accounts) {

                    //Builds the query to be executed.
                    StringBuilder query = new StringBuilder();
                    query.append("SELECT ");
                    query.append(cli.getParameter("field"));
                    query.append(" FROM ");
                    query.append(cli.getParameter("type"));

                    //Identifies if query has a filter.
                    if (!cli.getParameter("filter").isEmpty()) {
                        query.append(" WHERE ");
                        query.append(cli.getParameter("filter"));
                    }

                    // Constructs the SearchGoogleAdsStreamRequest.
                    SearchGoogleAdsStreamRequest request
                            = SearchGoogleAdsStreamRequest.newBuilder()
                                    .setCustomerId(account)
                                    .setQuery(query.toString())
                                    .build();

                    // API call returns a stream
                    ServerStream<SearchGoogleAdsStreamResponse> stream
                            = googleAdsServiceClient.searchStreamCallable().call(request);

                    //Count of rows of an account.
                    int count = 0;

                    // Iterates through the results in the stream response.
                    for (SearchGoogleAdsStreamResponse response : stream) {
                        count = response.getResultsList().size();

                        for (GoogleAdsRow googleAdsRow : response.getResultsList()) {
                            ArrayList<Object> record = new ArrayList();

                            //Convert row to json.
                            String json = JsonFormat
                                    .printer()
                                    .preservingProtoFieldNames()
                                    .print(googleAdsRow);

                            if (cli.getParameterAsBoolean("debug")) {
                                Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "Row: {0}", json);
                            }

                            for (String field : fields) {
                                // Get the json value.
                                try {
                                    Object value = JsonPath.read(json, "$." + field);
                                    record.add(value);
                                } catch (PathNotFoundException ex) {
                                    if (cli.getParameterAsBoolean("debug")) {
                                        Logger.getLogger(GoogleAds.class.getName()).log(Level.SEVERE, "Error getting field: " + field + ", setting null value.", ex);
                                    }
                                    record.add("");
                                }
                            }
                            mitt.write(record);
                        }
                    }
                    Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "Retrievied data from account {0} ({1} rows)", new Object[]{account, count});
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.SEVERE, "Google Ads failed to load GoogleAdsClient configuration from file", ex);
            System.exit(1);

        } catch (ValidationException
                | DuplicateEntityException
                | IOException ex) {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.SEVERE, "Google Ads failed due to ", ex);
            System.exit(1);

        } finally {
            mitt.close();
            Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "GLOVE - Google Ads Extractor finished");
        }
    }
}
