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
package br.com.dafiti.googleanalyticsrealtime;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.MD5;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsScopes;
import com.google.api.services.analytics.model.RealtimeData;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Google Analytics real time extractor.
 *
 * @author Helio Leal
 */
public class GoogleAnalyticsRealTime {

    /**
     * Main Method.
     *
     * @param args the command line arguments
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     * @throws java.io.IOException
     * @throws java.security.GeneralSecurityException
     */
    public static void main(String[] args) throws
            DuplicateEntityException,
            IOException,
            GeneralSecurityException {
        Logger.getLogger(GoogleAnalyticsRealTime.class.getName()).info("Google Analytics Real Time API extration started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("j", "credentials", "Path and name of the json generate in Google Cloud Platform.", "", true, false)
                .addParameter("o", "output", "Identify the output path and file name", "", true, false)
                .addParameter("u", "user_id", "Identify google analytics user id to extract data", "", true, false)
                .addParameter("m", "metrics", "Identify the metrics you want to extract based on Google GA API", "", true, false)
                .addParameter("d", "dimensions", "(Optional) Which dimensions will show on file", "")
                .addParameter("k", "key", "Unique key, divided by + if has more than one field", "", true, false)
                .addParameter("p", "partition", "Define the partition field or fields, divided by +", "", true, false)
                .addParameter("s", "sort", "(Optional) How data will be sorted.", "")
                .addParameter("f", "filters", "(Optional) Add filters", "")
                .addParameter("r", "row_limit", "(Optional)(Default is 20000) Identify how many lines will be called at once.", "20000")
                .addParameter("re", "retries", "(Optional)(Default is 3) Identify how many retries will do when limit rate exceeded.", "3")
                .addParameter("de", "delimiter", "(Optional)(Default is ;) Identify the delimiter character", ";")
                .addParameter("q", "quote", "(Optional)(Default is \") Identify the quote character", "\"");

        //Read the command line interface. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Define output file.
        mitt.setOutput(cli.getParameter("output"));

        //Define fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                .addCustomField("etl_load_date", new Now())
                .addField(cli.getParameterAsList("metrics", ","))
                .addField(cli.getParameterAsList("dimensions", ","));

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

        //Load client secrets.
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(cli.getParameter("credentials"))));

        //Set up authorization code flow.
        GoogleAuthorizationCodeFlow flow
                = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
                        .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(System.getProperty("user.home"), ".store/analytics_sample")))
                        .build();

        //Set up and return Google Analytics API client.
        Analytics analytics = new Analytics.Builder(
                httpTransport,
                JSON_FACTORY,
                new AuthorizationCodeInstalledApp(
                        flow,
                        new LocalServerReceiver())
                        .authorize("user"))
                .setApplicationName(cli.getParameter("Google Analytics Real time extractor"))
                .build();

        Analytics.Data.Realtime.Get realtimeRequest = analytics
                .data()
                .realtime()
                .get(cli.getParameter("user_id"), cli.getParameter("metrics"));

        if (cli.getParameter("dimensions") != null
                && !cli.getParameter("dimensions").isEmpty()) {
            realtimeRequest.setDimensions(cli.getParameter("dimensions"));
        }

        if (cli.getParameter("filters") != null
                && !cli.getParameter("filters").isEmpty()) {
            realtimeRequest.setFilters(cli.getParameter("filters"));
        }

        if (cli.getParameter("sort") != null
                && !cli.getParameter("sort").isEmpty()) {
            realtimeRequest.setSort(cli.getParameter("sort"));
        }

        if (cli.getParameterAsInteger("row_limit") > 0) {
            realtimeRequest.setMaxResults(cli.getParameterAsInteger("row_limit"));
        }

        RealtimeData realtimeData = realtimeRequest.execute();

        if (realtimeData.getTotalResults() > 0) {
            for (List<String> row : realtimeData.getRows()) {
                mitt.write(row);
            }
        }

        mitt.close();
        Logger.getLogger(GoogleAnalyticsRealTime.class.getName()).info("Google Analytics Real Time API extraction finalized.");
    }

}
