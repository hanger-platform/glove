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
package br.com.dafiti.googlesearchconsole;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Fixed;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.webmasters.Webmasters;
import com.google.api.services.webmasters.WebmastersScopes;
import com.google.api.services.webmasters.model.ApiDataRow;
import com.google.api.services.webmasters.model.ApiDimensionFilter;
import com.google.api.services.webmasters.model.ApiDimensionFilterGroup;
import com.google.api.services.webmasters.model.SearchAnalyticsQueryRequest;
import com.google.api.services.webmasters.model.SearchAnalyticsQueryResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Google Search console extractor main class.
 *
 * @author Helio Leal
 */
public class GoogleSearchConsole {

    /**
     * Main Method.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Logger.getLogger(GoogleSearchConsole.class.getName()).info("GLOVE - Google Serch Console Extractor started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Define parameters.
            mitt.getConfiguration()
                    .addParameter("j", "json_key_path", "Path and name of the json generate in Google Cloud Platform.", "", true, false)
                    .addParameter("a", "application_name", "Any application name of your choosing.", "", true, false)
                    .addParameter("o", "output", "Identify the output path and file name", "", true, false)
                    .addParameter("s", "site", "Site URL to be extracted data.", "", true, false)
                    .addParameter("S", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("d", "dimension", "Which dimensions will show on file", "", true, false)
                    .addParameter("k", "key", "Unique key, divided by + if has more than one field", "", true, false)
                    .addParameter("p", "partition", "Define the partition field or fields, divided by +", "", true, false)
                    .addParameter("D", "device", "(Available: desktop, mobile and tablet) Which devices will be considered. Example: desktop+mobile+tablet", "", true, false)
                    .addParameter("t", "type", "(Optional)(Default is web)(Available: web, image or video) The search type to filter for.", "web")
                    .addParameter("da", "delimiter", "(Optional)(Default is ;) Identify the delimiter character", ";")
                    .addParameter("q", "quote", "(Optional)(Default is \") Identify the quote character", "\"")
                    .addParameter("r", "retries", "(Optional)(Default is 3) Identify how many retries will do when limit rate exceeded.", "3")
                    .addParameter("w", "wait_time", "(Optional)(Default is 15 seconds) Identify time for thread to wait until next query execution.", "60")
                    .addParameter("ro", "row_limit", "(Optional)(Default is 25000) Identify how many lines will be called at once.", "25000");

            //Read the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Define output file.
            mitt.setOutput(cli.getParameter("output"));

            //Define fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField("type")
                    .addCustomField("site", new Fixed(cli.getParameter("site")))
                    .addField("clicks")
                    .addField("impressions")
                    .addField("ctr")
                    .addField("position")
                    .addField(cli.getParameterAsList("dimension", "\\+"));

            //Define the http request configurations.
            HttpRequestInitializer httpRequestInitializer = (HttpRequest httpRequest) -> {
                GoogleCredential.fromStream(
                        new FileInputStream(cli.getParameter("json_key_path")))
                        .createScoped(Collections.singleton(WebmastersScopes.WEBMASTERS))
                        .initialize(httpRequest);
                httpRequest.setReadTimeout(0);
            };

            //Create a new authorized API client.
            Webmasters webMasters = new Webmasters.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), httpRequestInitializer)
                    .setApplicationName(cli.getParameter("application_name"))
                    .build();

            for (String type : cli.getParameterAsList("type", "\\+")) {
                for (String device : cli.getParameterAsList("device", "\\+")) {
                    int rowStart = 0;
                    boolean retry = true;

                    while (retry) {
                        //Define query filters. 
                        ApiDimensionFilter apiDimensionFilter = new ApiDimensionFilter()
                                .setDimension("device")
                                .setExpression(device)
                                .setOperator("equals");

                        ArrayList<ApiDimensionFilter> apiDimensionFilters = new ArrayList<>();
                        apiDimensionFilters.add(apiDimensionFilter);
                        ApiDimensionFilterGroup apiDimensionJavaFilterGroup = new ApiDimensionFilterGroup().setFilters(apiDimensionFilters);
                        List<ApiDimensionFilterGroup> apiDimensionFilterGroups = new ArrayList<>();
                        apiDimensionFilterGroups.add(apiDimensionJavaFilterGroup);

                        //Define query parameters. 
                        SearchAnalyticsQueryRequest searchAnalyticsQueryRequest = new SearchAnalyticsQueryRequest();

                        searchAnalyticsQueryRequest.setDimensionFilterGroups(apiDimensionFilterGroups);
                        searchAnalyticsQueryRequest.setStartDate(cli.getParameter("start_date"));
                        searchAnalyticsQueryRequest.setEndDate(cli.getParameter("end_date"));
                        searchAnalyticsQueryRequest.setDimensions(cli.getParameterAsList("dimension", "\\+"));
                        searchAnalyticsQueryRequest.setRowLimit(cli.getParameterAsInteger("row_limit"));
                        searchAnalyticsQueryRequest.setStartRow(rowStart);
                        searchAnalyticsQueryRequest.setSearchType(type);

                        List<ApiDataRow> apiDataRows = null;

                        for (int tries = 0; tries < cli.getParameterAsInteger("retries"); tries++) {
                            try {
                                //Execute query.
                                SearchAnalyticsQueryResponse searchAnalyticsQueryResponse = webMasters
                                        .searchanalytics()
                                        .query(cli.getParameter("site"), searchAnalyticsQueryRequest)
                                        .execute();

                                //Identifies if at least a record is got. 
                                if (searchAnalyticsQueryResponse.getRows() != null && searchAnalyticsQueryResponse.getRows().size() > 0) {
                                    apiDataRows = searchAnalyticsQueryResponse.getRows();
                                }

                                break;
                            } catch (GoogleJsonResponseException gjrException) {
                                Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.SEVERE, "Rate Limit Exceeded", gjrException);
                                Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.INFO, "Try {0} of {1}", new Object[]{String.valueOf(tries + 1), cli.getParameter("retries")});
                                Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.INFO, "Waiting {0} seconds for next query execution.", cli.getParameter("wait_time"));

                                try {
                                    Thread.sleep(cli.getParameterAsInteger("wait_time") * 1000);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.SEVERE, "Sleep failure ", ex);
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                        if (apiDataRows != null) {
                            for (ApiDataRow apiDataRow : apiDataRows) {
                                //Define a record. 
                                ArrayList<Object> record = new ArrayList<>();

                                record.add(type);
                                record.add(apiDataRow.getClicks());
                                record.add(apiDataRow.getImpressions());
                                record.add(apiDataRow.getCtr());
                                record.add(apiDataRow.getPosition());

                                apiDataRow.getKeys().forEach((key) -> {
                                    record.add(key);
                                });

                                //Write a record to output file. 
                                mitt.write(record);

                                //Identifies the number of records processed. 
                                rowStart++;
                            }
                        } else {
                            retry = false;
                        }
                    }
                }
            }
        } catch (DuplicateEntityException
                | GeneralSecurityException
                | IOException ex) {

            Logger.getLogger(GoogleSearchConsole.class.getName()).log(Level.SEVERE, "Google Serch Console Extractor Failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(GoogleSearchConsole.class.getName()).info("GLOVE - Google Serch Console Extractor finalized.");
    }
}
