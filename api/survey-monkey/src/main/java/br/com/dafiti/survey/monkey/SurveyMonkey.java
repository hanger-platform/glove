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
package br.com.dafiti.survey.monkey;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.github.opendevl.JFlat;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class SurveyMonkey {

    private static final Logger LOG = Logger.getLogger(SurveyMonkey.class.getName());
    private static final String SURVEYMONKEY_ENDPOINT = "https://api.surveymonkey.com/v3/";

    /**
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - SurveyMonkey API extractor started");

        int page = 0;
        boolean paginate = false;
        boolean process = true;
        JSONObject parameters = null;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in a JsonPath format", "", true, false)
                    .addParameter("e", "endpoint", "Endpoint uri", "", true, false)
                    .addParameter("pa", "paginate", "(Optional) Identifies if the endpoint has pagination", "false", true, true)
                    .addParameter("p", "parameters", "(Optional) Endpoint parameters", "", true, true)
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
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

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrieves API credentials.
            String token = credentials.get("authorization").toString();

            //Identifies endpoint parameters. 
            String endpointParameter = cli.getParameter("parameters");

            if (endpointParameter != null && !endpointParameter.isEmpty()) {
                try {
                    parameters = (JSONObject) parser.parse(endpointParameter);
                } catch (ParseException ex) {
                    LOG.log(Level.INFO, "Fail parsing endpoint parameters: {0}", endpointParameter);
                }
            }

            //Identifies if endpoint is paginated.
            paginate = cli.getParameterAsBoolean("paginate");

            // Gets original fields.
            List originalFields = mitt.getConfiguration().getOriginalFieldName();

            do {
                //Increments page.
                page++;

                String json = null;

                //Accept standard cookies
                RequestConfig requestConfig = RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build();

                //Connect to the API. 
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(SURVEYMONKEY_ENDPOINT + cli.getParameter("endpoint"));

                    //Sets header.
                    httpGet.setHeader("Content-Type", "application/json");
                    httpGet.setHeader("Authorization", token);

                    //Sets http configurations.
                    httpGet.setConfig(requestConfig);

                    //Sets default URI parameters. 
                    URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());

                    //Identifies if endpoint has pagination.
                    if (paginate) {
                        uriBuilder.addParameter("page", String.valueOf(page));
                    }

                    //Sets endpoint URI parameters. 
                    if (parameters != null && !parameters.isEmpty()) {
                        for (Object key : parameters.keySet()) {
                            uriBuilder.addParameter((String) key, (String) parameters.get(key));
                        }
                    }

                    //Sets URI parameters. 
                    httpGet.setURI(uriBuilder.build());

                    //Executes a request. 
                    CloseableHttpResponse response = client.execute(httpGet);

                    //Gets a reponse entity. 
                    json = EntityUtils.toString(response.getEntity(), "UTF-8");
                }

                if (json != null && !json.isEmpty()) {

                    //Display page statistics.
                    if (paginate) {
                        int perPage = JsonPath.read(json, "$.per_page");
                        int currentPage = JsonPath.read(json, "$.page");
                        int total = JsonPath.read(json, "$.total");

                        LOG.log(Level.INFO, "Total: {0} | Current page: {1} ({2} per page)", new Object[]{total, currentPage, perPage});

                        //Identifies if it is last page.
                        try {
                            JsonPath.read(json, "$.links.next");
                        } catch (PathNotFoundException ex) {
                            process = false;
                        }
                    }

                    //Defines jFlat to flatten json.
                    JFlat jFlat = new JFlat(json);

                    //get the 2D representation of JSON document.
                    List<Object[]> values = jFlat.json2Sheet().headerSeparator(".").getJsonAsSheet();

                    //Starts at 1 to ignore header.
                    for (int line = 1; line < values.size(); line++) {
                        List record = new ArrayList();

                        //Fetchs columns
                        for (int column = 0; column < values.get(0).length; column++) {

                            //Identifies header position.
                            if (originalFields.contains(values.get(0)[column])) {
                                if (values.get(line)[column] != null) {
                                    record.add(((JsonPrimitive) values.get(line)[column]).getAsString());
                                } else {
                                    record.add("");
                                }
                            }
                        }

                        mitt.write(record);
                    }
                }
            } while (paginate && process);

            //   mitt.write(outputPath.toFile());
            //FileUtils.deleteDirectory(outputPath.toFile());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SurveyMonkey API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SurveyMonkey API extractor finalized");
    }
}
