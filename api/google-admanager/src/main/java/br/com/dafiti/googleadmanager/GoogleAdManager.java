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
package br.com.dafiti.googleadmanager;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import com.google.api.ads.admanager.axis.factory.AdManagerServices;
import com.google.api.ads.admanager.axis.utils.v202105.DateTimes;
import com.google.api.ads.admanager.axis.utils.v202105.ReportDownloader;
import com.google.api.ads.admanager.axis.utils.v202105.StatementBuilder;
import com.google.api.ads.admanager.axis.v202105.Column;
import com.google.api.ads.admanager.axis.v202105.DateRangeType;
import com.google.api.ads.admanager.axis.v202105.Dimension;
import com.google.api.ads.admanager.axis.v202105.DimensionAttribute;
import com.google.api.ads.admanager.axis.v202105.ExportFormat;
import com.google.api.ads.admanager.axis.v202105.ReportDownloadOptions;
import com.google.api.ads.admanager.axis.v202105.ReportJob;
import com.google.api.ads.admanager.axis.v202105.ReportQuery;
import com.google.api.ads.admanager.axis.v202105.ReportQueryAdUnitView;
import com.google.api.ads.admanager.axis.v202105.ReportServiceInterface;
import com.google.api.ads.admanager.lib.client.AdManagerSession;
import com.google.api.ads.common.lib.auth.OfflineCredentials;
import com.google.api.ads.common.lib.exception.OAuthException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Google Ad manager extractor.
 *
 * @author Helio Leal
 */
public class GoogleAdManager {

    public static void main(String[] args) throws
            IOException,
            InterruptedException,
            DuplicateEntityException,
            GeneralSecurityException,
            ValidationException,
            OAuthException {

        Logger.getLogger(GoogleAdManager.class.getName()).info("Google Ad Manager extration started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters.
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Path and name of the json generate in Google Cloud Platform", "", true, false)
                .addParameter("o", "output", "Identify the output path", "", true, false)
                .addParameter("nc", "network_code", "Code of the network user in ad manager", "", true, false)
                .addParameter("an", "application_name", "Any application name of your choosing", "", true, false)
                .addParameter("sd", "start_date", "Start date", "", true, false)
                .addParameter("ed", "end_date", "End date", "", true, false)
                .addParameter("bk", "key", "Business Key", "", true, false)
                .addParameter("d", "dimensions", "Dimensions", "", true, false)
                .addParameter("p", "partition", "define the file partition", "", true, false)
                .addParameter("d", "columns", "define the columns that will be loaded", "", true, false)
                .addParameter("da", "dimensions_attributes", "define the columns that will be loaded", "", true, false)
                .addParameter("f", "filters", "Query filter", "")
                .addParameter("tz", "time_zone", "Time zone", "America/Sao_Paulo")
                .addParameter("de", "delimiter", "Identify the delimiter character", ",")
                .addParameter("q", "quote", "Identify the quote character", "\"");

        //Read the command line interface. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Define output file.
        mitt.setOutputFile(cli.getParameter("output"));

        //Define fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+"), "|"))
                .addCustomField("etl_load_date", new Now())
                .addField(cli.getParameterAsList("dimensions", "\\+"))
                .addField(cli.getParameterAsList("dimensions_attributes", "\\+"))
                .addField(cli.getParameterAsList("columns", "\\+"));

        //Prepare ad manager session.
        AdManagerSession adManagerSession = new AdManagerSession.Builder()
                .withApplicationName(cli.getParameter("application_name"))
                .withNetworkCode(cli.getParameter("network_code"))
                .withOAuth2Credential(new OfflineCredentials.Builder()
                        .forApi(OfflineCredentials.Api.AD_MANAGER)
                        .withJsonKeyFilePath(cli.getParameter("credentials"))
                        .build()
                        .generateCredential())
                .build();

        //Define query.
        ReportQuery reportQuery = new ReportQuery();

        //Define query dimensions.
        int dimensionLength = cli.getParameterAsList("dimensions", "\\+").size();
        if (dimensionLength > 0) {
            Dimension[] dimensions = new Dimension[dimensionLength];
            for (int i = 0; i < dimensionLength; i++) {
                dimensions[i] = Dimension.fromString(cli.getParameterAsList("dimensions", "\\+").get(i).replace("Dimension.", ""));
            }
            reportQuery.setDimensions(dimensions);
        }

        //Define query columns.
        int columnLength = cli.getParameterAsList("columns", "\\+").size();
        if (columnLength > 0) {
            Column[] columns = new Column[columnLength];
            for (int i = 0; i < columnLength; i++) {
                columns[i] = Column.fromString(cli.getParameterAsList("columns", "\\+").get(i).replace("Column.", ""));
            }
            reportQuery.setColumns(columns);
        }

        //Define query dimension attributes.
        int dimensionAttributeLength = cli.getParameterAsList("dimensions_attributes", "\\+").size();
        if (dimensionAttributeLength > 0) {
            DimensionAttribute[] dimensionsAttributes = new DimensionAttribute[dimensionAttributeLength];
            for (int i = 0; i < dimensionAttributeLength; i++) {
                dimensionsAttributes[i] = DimensionAttribute.fromString(cli.getParameterAsList("dimensions_attributes", "\\+").get(i).replace("DimensionAttribute.", ""));
            }
            reportQuery.setDimensionAttributes(dimensionsAttributes);
        }

        //Define date range.
        reportQuery.setDateRangeType(DateRangeType.CUSTOM_DATE);
        reportQuery.setStartDate(DateTimes.toDateTime(cli.getParameter("start_date") + "T00:00:00", cli.getParameter("time_zone")).getDate());
        reportQuery.setEndDate(DateTimes.toDateTime(cli.getParameter("end_date") + "T00:00:00", cli.getParameter("time_zone")).getDate());

        //Identify if has filter and apply it.        
        if (cli.getParameter("filters") != null && !cli.getParameter("filters").isEmpty()) {
            StatementBuilder statementBuilder = new StatementBuilder().where(cli.getParameter("filters"));
            reportQuery.setStatement(statementBuilder.toStatement());
        }

        reportQuery.setAdUnitView(ReportQueryAdUnitView.HIERARCHICAL);

        //Create report job using defined query.
        ReportJob reportJob = new ReportJob();
        reportJob.setReportQuery(reportQuery);

        //Get the ReportService.
        ReportServiceInterface reportServiceInterface = new AdManagerServices().get(adManagerSession, ReportServiceInterface.class);

        //Run report job.
        reportJob = reportServiceInterface.runReportJob(reportJob);

        //Create report downloader.
        ReportDownloader reportDownloader = new ReportDownloader(reportServiceInterface, reportJob.getId());

        //Wait for the report to be ready.
        reportDownloader.waitForReportReady();

        //Change to your file location.
        File file = File.createTempFile("admanager-report-", ".csv");

        try {
            //Output file settings.
            ReportDownloadOptions options = new ReportDownloadOptions();
            options.setExportFormat(ExportFormat.CSV_DUMP);
            options.setUseGzipCompression(false);

            //Gererates download URL.
            URL url = reportDownloader.getDownloadUrl(options);

            //Download file.
            Resources.asByteSource(url).copyTo(Files.asByteSink(file));

        } catch (IOException ex) {
            Logger.getLogger(GoogleAdManager.class.getName()).log(Level.SEVERE, "Error on downloading report", ex);
        }

        // Writes all source files to a single target file.
        mitt.getReaderSettings().setDelimiter(',');
        mitt.getReaderSettings().setEncode("UTF-8");
        mitt.write(file, "*");
        mitt.close();

        Logger.getLogger(GoogleAdManager.class.getName()).info("Google Ad Manager extration finalized.");
    }
}
