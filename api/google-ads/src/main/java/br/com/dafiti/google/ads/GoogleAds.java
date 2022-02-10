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
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.ads.googleads.lib.GoogleAdsClient;
import com.google.ads.googleads.v9.services.GoogleAdsRow;
import com.google.ads.googleads.v9.services.GoogleAdsServiceClient;
import com.google.ads.googleads.v9.services.SearchGoogleAdsStreamRequest;
import com.google.ads.googleads.v9.services.SearchGoogleAdsStreamResponse;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.StreamController;
import com.google.api.pathtemplate.ValidationException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.util.JsonFormat;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;

/**
 *
 * @author Helio Leal
 * @author Valdiney V GOMES
 */
public class GoogleAds {

    public static void main(String[] args) {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "GLOVE - Google Ads Extractor started");

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
            Configuration configuration = mitt.getConfiguration();

            if (cli.hasParameter("partition")) {
                configuration
                        .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")));
            }

            if (cli.hasParameter("key")) {
                configuration
                        .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")));
            }
            configuration
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));


            //Fixes could not find policy 'pick_first' error.
            LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());

            //Defines an authenticated client for Google Ads api.
            GoogleAdsClient googleAdsClient = GoogleAdsClient
                    .newBuilder()
                    .fromPropertiesFile(new File(cli.getParameter("credentials"))).
                    setLoginCustomerId(Long.parseLong(cli.getParameter("manager")))
                    .build();

            try (GoogleAdsServiceClient googleAdsServiceClient = googleAdsClient.getLatestVersion().createGoogleAdsServiceClient()) {
                ArrayList<String> accounts = new ArrayList<>();

                //Retrieves all child accounts of the manager, don't bring manager account.                
                String queryAccounts = "SELECT customer_client.manager, customer_client.id FROM customer_client WHERE customer_client.level <= 1 AND customer_client.manager = false";

                //Constructs the SearchGoogleAdsStreamRequest.
                SearchGoogleAdsStreamRequest searchGoogleAdsStreamRequest
                        = SearchGoogleAdsStreamRequest.newBuilder()
                                .setCustomerId(cli.getParameter("customer"))
                                .setQuery(queryAccounts)
                                .build();

                //API call returns a stream
                ServerStream<SearchGoogleAdsStreamResponse> searchGoogleAdsStreamResponse
                        = googleAdsServiceClient.searchStreamCallable().call(searchGoogleAdsStreamRequest);

                //Iterates through the results in the stream response.
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

                //Builds the query to be executed.
                StringBuilder query = new StringBuilder();
                query.append("SELECT ").append(cli.getParameter("field")).append(" FROM ").append(cli.getParameter("type"));

                if (!cli.getParameter("filter").isEmpty()) {
                    query.append(" WHERE ").append(cli.getParameter("filter"));
                }

                //Defines the output path. 
                Path outputPath = Files.createTempDirectory("google_ads_" + UUID.randomUUID());

                //Defines the listenable future collection.  
                List<ListenableFuture<ReportSummary>> futures = new ArrayList<>();

                for (String account : accounts) {
                    //Defines the report observer. 
                    ReportObserver responseObserver = new ReportObserver(
                            account,
                            outputPath,
                            mitt.getConfiguration().getOriginalFieldName());

                    //Executes the report asynchronously.
                    googleAdsServiceClient
                            .searchStreamCallable()
                            .call(
                                    SearchGoogleAdsStreamRequest.newBuilder()
                                            .setCustomerId(account)
                                            .setQuery(query.toString())
                                            .build(),
                                    responseObserver);

                    //Stores a future to retrieve the results.
                    futures.add(responseObserver.asFuture());
                }

                //Logs each report execution result. 
                for (ReportSummary reportSummary : Futures.allAsList(futures).get()) {
                    Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, reportSummary.toString());
                }

                //Writes to output.
                mitt.write(outputPath.toFile());

                //Removes temporary folder and its files. 
                Files.delete(outputPath);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.SEVERE, "Google Ads failed to load GoogleAdsClient configuration from file", ex);
            System.exit(1);

        } catch (ValidationException
                | DuplicateEntityException
                | IOException
                | InterruptedException
                | ExecutionException ex) {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.SEVERE, "Google Ads failed due to ", ex);
            System.exit(1);

        } finally {
            mitt.close();
            Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "GLOVE - Google Ads Extractor finished");
        }
    }

    /**
     *
     */
    private static class ReportObserver implements ResponseObserver<SearchGoogleAdsStreamResponse> {

        private final SettableFuture<ReportSummary> future = SettableFuture.create();
        private final String account;
        private final List<String> fields;
        private final Path outputPath;
        private final AtomicLong trips = new AtomicLong(0);
        private final AtomicLong records = new AtomicLong(0);

        ReportObserver(String account, Path outputPath, List<String> fields) {
            this.account = account;
            this.outputPath = outputPath;
            this.fields = fields;
        }

        @Override
        public void onStart(StreamController controller) {
            Logger.getLogger(GoogleAds.class.getName()).log(Level.INFO, "Retrievied data from account {0}", new Object[]{this.account});
        }

        @Override
        public void onResponse(SearchGoogleAdsStreamResponse response) {
            try {
                //Defines a temporary file to hold de reponse. 
                Path responsePath = Paths.get(this.outputPath.toString() + File.separator + account + "_" + trips);

                try (
                        CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(responsePath),
                                CSVFormat.Builder.create()
                                        .setDelimiter(';')
                                        .setQuote('"')
                                        .setHeader(fields.toArray(new String[0])).build())) {

                    for (GoogleAdsRow googleAdsRow : response.getResultsList()) {
                        ArrayList<Object> record = new ArrayList<>();

                        String json = JsonFormat
                                .printer()
                                .preservingProtoFieldNames()
                                .print(googleAdsRow);

                        for (String field : fields) {
                            try {
                                Object value = JsonPath.read(json, "$." + field);
                                record.add(value);
                            } catch (PathNotFoundException ex) {
                                record.add("");
                            }
                        }

                        csvPrinter.printRecord(record);
                    }

                    csvPrinter.flush();
                }

                //Identifies the number of trips. 
                trips.incrementAndGet();

                //Identifies the record count. 
                records.addAndGet(response.getResultsCount());
            } catch (IOException ex) {
                notifyResultReady(new ReportSummary(account, records.get(), ex));
            }
        }

        @Override
        public void onError(Throwable t) {
            notifyResultReady(new ReportSummary(account, records.get(), t));
        }

        @Override
        public void onComplete() {
            notifyResultReady(new ReportSummary(account, records.get()));
        }

        private void notifyResultReady(ReportSummary summary) {
            future.set(summary);
        }

        ListenableFuture<ReportSummary> asFuture() {
            return future;
        }
    }

    /**
     *
     */
    private static class ReportSummary {

        private final String account;
        private final long records;
        private final Throwable throwable;

        ReportSummary(String account, long records, Throwable throwable) {
            this.account = account;
            this.throwable = throwable;
            this.records = records;
        }

        ReportSummary(String customerId, long records) {
            this(customerId, records, null);
        }

        boolean isSuccess() {
            return throwable == null;
        }

        @Override
        public String toString() {
            return "Customer ID: "
                    + account
                    + " Records: "
                    + records
                    + " IsSuccess? "
                    + (isSuccess() ? "Yes!" : "No :-( Why? " + throwable.getMessage());
        }
    }
}
