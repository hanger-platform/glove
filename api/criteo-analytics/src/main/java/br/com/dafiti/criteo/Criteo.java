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

/**
 *
 * @author Valdiney V GOMES
 */
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import com.criteo.marketing.ApiClient;
import com.criteo.marketing.ApiException;
import com.criteo.marketing.ApiResponse;
import com.criteo.marketing.Configuration;
import com.criteo.marketing.api.AuthenticationApi;
import com.criteo.marketing.api.StatisticsApi;
import com.criteo.marketing.model.StatsQueryMessageEx;
import com.criteo.marketing.model.StatsQueryMessageEx.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.json.simple.parser.ParseException;

public class Criteo {

    private static final Logger LOG = Logger.getLogger(Criteo.class.getName());

    public static void main(String[] args) {
        LOG.info("Glove - Criteo Analytics Extractor started");

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
                    .addParameter("d", "debug", "(Optional) Identifies if debug mode is enabled", false);

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
            ArrayList QueryDimensions = new ArrayList<DimensionsEnum>();

            cli.getParameterAsList("dimensions", "\\+").forEach(dimension -> {
                QueryDimensions.add(DimensionsEnum.valueOf(dimension.toUpperCase()));
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
            StatsQueryMessageEx statsQuery = new StatsQueryMessageEx();
            statsQuery.setReportType(ReportTypeEnum.CAMPAIGNPERFORMANCE);
            statsQuery.setAdvertiserIds(cli.getParameter("account"));
            statsQuery.setStartDate(OffsetDateTime.of(startDate.atStartOfDay(), ZoneOffset.UTC));
            statsQuery.setEndDate(OffsetDateTime.of(endDate.atStartOfDay(), ZoneOffset.UTC));
            statsQuery.setTimezone(TimezoneEnum.GMT);
            statsQuery.setFormat(FormatEnum.CSV);
            statsQuery.setCurrency(cli.getParameter("currency"));
            statsQuery.setDimensions(QueryDimensions);
            statsQuery.setMetrics(QueryMetrics);

            //Defines the client API. 
            ApiClient client = Configuration.getDefaultApiClient();
            client.setDebugging(cli.hasParameter("debug"));

            client.setDateFormat(DateFormat.getDateInstance(DateFormat.SHORT));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Requests the access token. 
            String accessToken = new AuthenticationApi(client).oAuth2TokenPost(
                    credentials.get("client_id").toString(),
                    credentials.get("client_secret").toString(),
                    "client_credentials"
            ).getAccessToken();

            //Requests report data. 
            ApiResponse<byte[]> stats = new StatisticsApi(client).getStatsWithHttpInfo("Bearer " + accessToken, statsQuery);

            Path outputPath = Files.createTempDirectory("criteo_");
            FileUtils.writeByteArrayToFile(
                    new File(outputPath.toString() + "/" + "criteo-analytics.csv"),
                    stats.getData());

            //Write the data to output file. 
            mitt.write(outputPath.toFile(), "*.csv");
            FileUtils.deleteDirectory(outputPath.toFile());
        } catch (DuplicateEntityException
                | IOException
                | ParseException
                | ApiException ex) {

            LOG.log(Level.SEVERE, "Criteo Analytics Extractor failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
            
            LOG.info("Glove - Criteo Analytics Extractor finalized.");
            System.exit(0);
        }
    }
}
