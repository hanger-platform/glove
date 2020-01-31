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
package br.com.dafiti.google.adwords;

/**
 *
 * @author Valdiney V GOMES
 */
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.utils.v201809.SelectorBuilder;
import com.google.api.ads.adwords.axis.v201809.mcm.ManagedCustomer;
import com.google.api.ads.adwords.axis.v201809.mcm.ManagedCustomerPage;
import com.google.api.ads.adwords.axis.v201809.mcm.ManagedCustomerServiceInterface;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.client.AdWordsSession.ImmutableAdWordsSession;
import com.google.api.ads.adwords.lib.client.reporting.ReportingConfiguration;
import com.google.api.ads.adwords.lib.factory.AdWordsServicesInterface;
import com.google.api.ads.adwords.lib.jaxb.v201809.DateRange;
import com.google.api.ads.adwords.lib.jaxb.v201809.DownloadFormat;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinition;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionDateRangeType;
import com.google.api.ads.adwords.lib.jaxb.v201809.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.jaxb.v201809.Selector;
import com.google.api.ads.adwords.lib.selectorfields.v201809.cm.ManagedCustomerField;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponseException;
import com.google.api.ads.adwords.lib.utils.ReportException;
import com.google.api.ads.adwords.lib.utils.v201809.ReportDownloaderInterface;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.auth.OfflineCredentials.Api;
import com.google.api.ads.common.lib.conf.ConfigurationLoadException;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleAdwords {

    public static void main(String[] args) {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            Logger.getLogger(GoogleAdwords.class.getName()).log(Level.INFO, "GLOVE - Google Adwords (v201809) Extractor started");

            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("r", "type", "Adwords report type", "", true, false)
                    .addParameter("f", "field", "Report fields", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("u", "customer", "(Optional) Customer IDs, divided by + if has more than one", "")
                    .addParameter("w", "zero_impression", "(Optional) Include Zero Impressions. false as default", "false")
                    .addParameter("t", "threads", "(Optional)  Number customer reports being generated in parallel. 5 as default", "5")
                    .addParameter("z", "page_size", "(Optional)  Page size. 500 as default", "500")
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one", "");

            //Read the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutput(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Generates a refreshable OAuth2 credential.
            Credential credential = new OfflineCredentials.Builder()
                    .forApi(Api.ADWORDS)
                    .fromFile(cli.getParameter("credentials"))
                    .build()
                    .generateCredential();

            //Construct an ImmutableAdWordsSession to use as a prototype when creating a session for each managed customer.
            ImmutableAdWordsSession session = new AdWordsSession.Builder()
                    .fromFile(cli.getParameter("credentials"))
                    .withOAuth2Credential(credential)
                    .buildImmutable();

            AdWordsServicesInterface adWordsServicesInterface = AdWordsServices.getInstance();

            //Defines the output path. 
            File outputPath = Files.createTempDir();
            outputPath.mkdirs();

            LocalDate startDate = LocalDate.parse(cli.getParameter("start_date"), DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalDate endDate = LocalDate.parse(cli.getParameter("end_date"), DateTimeFormatter.ofPattern("yyyyMMdd"));
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);

            //Extract the report for each date.
            for (int i = 0; i <= daysBetween; i++) {
                String date = startDate.plusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

                Logger.getLogger(GoogleAdwords.class.getName()).log(Level.INFO, "GLOVE - Downloading reports of {0}", date);

                //Defines report date range.
                DateRange dateRange = new DateRange();
                dateRange.setMin(date);
                dateRange.setMax(date);

                //Defines report selector.
                Selector selector = new Selector();
                selector.getFields().addAll(mitt.getConfiguration().getOriginalFieldsName());
                selector.setDateRange(dateRange);

                //Defines report definition.
                ReportDefinition reportDefinition = new ReportDefinition();
                reportDefinition.setReportName(cli.getParameter("type"));
                reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.CUSTOM_DATE);
                reportDefinition.setReportType(ReportDefinitionReportType.valueOf(cli.getParameter("type").toUpperCase()));
                reportDefinition.setDownloadFormat(DownloadFormat.CSV);
                reportDefinition.setSelector(selector);

                //Defines report configuration.
                ReportingConfiguration reportingConfiguration = new ReportingConfiguration.Builder()
                        .skipReportHeader(true)
                        .skipColumnHeader(true)
                        .skipReportSummary(true)
                        .includeZeroImpressions(cli.getParameterAsBoolean("zero_impression"))
                        .build();

                //Create a thread pool for submitting report requests.
                int maxElapsedSecondsPerCustomer = 60 * 5;
                ExecutorService threadPool = Executors.newFixedThreadPool(cli.getParameterAsInteger("threads"));
                ExponentialBackOff.Builder backOffBuilder = new ExponentialBackOff.Builder().setMaxElapsedTimeMillis(maxElapsedSecondsPerCustomer * 1000);
                List<ReportDownloader> reportDownloadFutureTasks = new ArrayList<>();

                //Retrieve all accounts under the manager account.
                List<String> customers = cli.getParameterAsList("customer", "\\+");

                if (customers.isEmpty()) {
                    int offset = 0;
                    ManagedCustomerPage managedCustomerPage;
                    ManagedCustomerServiceInterface managedCustomerService = adWordsServicesInterface.get(session, ManagedCustomerServiceInterface.class);

                    SelectorBuilder selectorBuilder = new SelectorBuilder()
                            .fields(ManagedCustomerField.CustomerId)
                            .equals(ManagedCustomerField.CanManageClients, "false")
                            .limit(cli.getParameterAsInteger("page_size"))
                            .offset(0);

                    do {
                        selectorBuilder.offset(offset);
                        managedCustomerPage = managedCustomerService.get(selectorBuilder.build());

                        if (managedCustomerPage.getEntries() != null) {
                            for (ManagedCustomer managedCustomer : managedCustomerPage.getEntries()) {
                                customers.add(managedCustomer.getCustomerId().toString());
                            }
                        }

                        offset += cli.getParameterAsInteger("page_size");
                    } while (offset < managedCustomerPage.getTotalNumEntries());
                }

                //Dowload report for each customer. 
                for (String customer : customers) {
                    File outputFile = new File(outputPath, customer + "_" + date + ".csv");

                    ImmutableAdWordsSession sessionForCustomer = session
                            .newBuilder()
                            .withClientCustomerId(customer)
                            .withReportingConfiguration(reportingConfiguration)
                            .buildImmutable();

                    ReportDownloader reportDownloadFutureTask = new ReportDownloader(
                            new ReportDownloaderCallable(
                                    sessionForCustomer,
                                    adWordsServicesInterface,
                                    reportDefinition,
                                    outputFile,
                                    backOffBuilder.build()));

                    threadPool.execute(reportDownloadFutureTask);
                    reportDownloadFutureTasks.add(reportDownloadFutureTask);
                }

                threadPool.shutdown();
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                //Identifies if all reports was downloaded successfully. 
                reportDownloadFutureTasks.forEach((reportDownloadFutureTask) -> {
                    String clientCustomerId = reportDownloadFutureTask.getClientCustomerId();

                    try {
                        reportDownloadFutureTask.get();
                    } catch (CancellationException
                            | InterruptedException
                            | ExecutionException ex) {

                        Logger.getLogger(GoogleAdwords.class.getName()).log(Level.SEVERE, "Google Adwords Reporting failed for customer " + clientCustomerId, ex);
                        System.exit(1);
                    }
                });
            }

            //Writes all source files to a single target file.
            mitt.write(outputPath, "*.csv", ',', mitt.getConfiguration().getOriginalFieldsName());
        } catch (RemoteException
                | InterruptedException
                | ValidationException
                | ConfigurationLoadException
                | OAuthException
                | DuplicateEntityException ex) {

            Logger.getLogger(GoogleAdwords.class.getName()).log(Level.SEVERE, "Google Adwords Reporting failed due to ", ex);
            System.exit(1);
        } finally {
            mitt.close();
            Logger.getLogger(GoogleAdwords.class.getName()).log(Level.INFO, "GLOVE - Google Adwords Extractor finished");
        }
    }

    /**
     * Report dowloader futere task.
     */
    private static class ReportDownloader extends FutureTask<File> {

        private final String clientCustomerId;

        public ReportDownloader(ReportDownloaderCallable callable) {
            super(callable);
            this.clientCustomerId = callable.session.getClientCustomerId();
        }

        String getClientCustomerId() {
            return clientCustomerId;
        }
    }

    /**
     * Report dowloader futere callable.
     */
    private static class ReportDownloaderCallable implements Callable<File> {

        private final ImmutableAdWordsSession session;
        private final AdWordsServicesInterface adWordsServices;
        private final ReportDefinition reportDefinition;
        private final File reportOutputFile;
        private final ExponentialBackOff backOff;

        private ReportDownloaderCallable(
                ImmutableAdWordsSession session,
                AdWordsServicesInterface adWordsServices,
                ReportDefinition reportDefinition,
                File reportOutputFile,
                ExponentialBackOff backOff) {

            this.session = session;
            this.adWordsServices = adWordsServices;
            this.reportDefinition = reportDefinition;
            this.reportOutputFile = reportOutputFile;
            this.backOff = backOff;
        }

        @Override
        public File call() throws
                ReportException,
                ReportDownloadResponseException,
                IOException,
                InterruptedException {

            boolean go = true;
            ReportException reportException = null;
            ReportDownloaderInterface reportDownloaderInterface = adWordsServices.getUtility(session, ReportDownloaderInterface.class);

            do {
                try {
                    ReportDownloadResponse response = reportDownloaderInterface.downloadReport(reportDefinition);
                    response.saveToFile(reportOutputFile.getPath());
                    Logger.getLogger(GoogleAdwords.class.getName()).log(Level.INFO, "Report generated successfully for customer {0}", session.getClientCustomerId());

                    return reportOutputFile;
                } catch (ReportException ex) {
                    reportException = ex;
                    long sleep = backOff.nextBackOffMillis();

                    if (sleep == BackOff.STOP) {
                        go = false;
                        reportException = new ReportException("Report request failed after maximum elapsed millis: " + backOff.getMaxElapsedTimeMillis(), ex);
                    } else {
                        Thread.sleep(sleep);
                    }
                }
            } while (go);

            throw reportException;
        }
    }
}
