/*
 * Copyright (c) 2020 Dafiti Group
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
package br.com.dafiti.criteo;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import com.criteo.marketing.ApiClient;
import com.criteo.marketing.ApiException;
import com.criteo.marketing.ApiResponse;
import com.criteo.marketing.Configuration;
import com.criteo.marketing.api.AnalyticsApi;
import com.criteo.marketing.model.StatisticsReportQueryMessage;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Valdiney V GOMES
 * @author Fernando Saga
 */
public class Criteo {

    private static final Logger LOG = Logger.getLogger(Criteo.class.getName());

    public static void main(String[] args) {
        LOG.info("Glove - Criteo Analytics Extractor started");

        int retries = 0;
        boolean retry = false;

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from input file", "", true, false)
                    .addParameter("d", "dimensions", "Report dimensions, divided by + if has more than one field", "", true, false)
                    .addParameter("m", "metrics", "Report metrics, divided by + if has more than one field", "", true, false)
                    .addParameter("a", "account", "Advertiser account ID", "", true, false)
                    .addParameter("cu", "currency", "(Optional) Currency code.  Default is BRL", "BRL")
                    .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                    .addParameter("d", "debug", "(Optional) Identifies if debug mode is enabled", false)
                    .addParameter("r", "retry", "(Optional) How many retries. Default is 3", "3");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Defines report dimensions. 
            ArrayList QueryDimensions = new ArrayList<StatisticsReportQueryMessage.DimensionsEnum>();
            cli.getParameterAsList("dimensions", "\\+").forEach(dimension -> {
                QueryDimensions.add(StatisticsReportQueryMessage.DimensionsEnum.valueOf(dimension.toUpperCase()));
            });

            //Define report metrics. 
            ArrayList QueryMetrics = new ArrayList<String>();
            QueryMetrics.addAll(cli.getParameterAsList("metrics", "\\+"));

            //Define report interval. 
            LocalDate startDate = LocalDate
                    .parse(
                            cli.getParameter("start_date"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    );

            LocalDate endDate = LocalDate
                    .parse(
                            cli.getParameter("end_date"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    );

            //Defines query parameters.
            StatisticsReportQueryMessage srqm = new StatisticsReportQueryMessage();
            srqm.advertiserIds(cli.getParameter("account"));
            srqm.startDate(OffsetDateTime.of(startDate.atStartOfDay(), ZoneOffset.UTC));
            srqm.endDate(OffsetDateTime.of(endDate.atStartOfDay(), ZoneOffset.UTC));
            srqm.timezone("GMT");
            srqm.format("Csv");
            srqm.currency(cli.getParameter("currency"));
            srqm.dimensions(QueryDimensions);
            srqm.metrics(QueryMetrics);

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Defines the client API. 
            ApiClient client = Configuration.getDefaultApiClient();

            client.setUsername(credentials.get("client_id").toString());
            client.setPassword(credentials.get("client_secret").toString());
            client.setDebugging(cli.hasParameter("debug"));
            client.setDateFormat(DateFormat.getDateInstance(DateFormat.SHORT));

            do {
                try {
                    //Requests report data.
                    AnalyticsApi analyticsApi = new AnalyticsApi(client);
                    ApiResponse<byte[]> stats = analyticsApi.getAdsetReportWithHttpInfo(srqm);

                    Path outputPath = Files.createTempDirectory("criteo_");
                    FileUtils.writeByteArrayToFile(
                            new File(outputPath.toString() + "/" + "criteo-analytics.csv"),
                            stats.getData());

                    //Write the data to output file. 
                    mitt.write(outputPath.toFile(), "*.csv");
                    FileUtils.deleteDirectory(outputPath.toFile());

                    retry = false;

                } catch (ApiException e) {
                    retries++;
                    retry = true;

                    if (retries > Integer.parseInt(cli.getParameter("retry"))) {
                        throw new Exception("ApiException: " + e);
                    } else {
                        Thread.sleep(retries * 20000);
                        LOG.log(Level.INFO, "Authentication error, retry {0}", retries);
                    }
                }
            } while (retry);

        } catch (Exception ex) {

            LOG.log(Level.SEVERE, "Criteo Analytics Extractor failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            LOG.info("Glove - Criteo Analytics Extractor finalized.");
            System.exit(0);
        }
    }
}
