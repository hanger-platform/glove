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
package br.com.dafiti.similarweb;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Fernando SAGA
 */
public class SimilarWeb {

    private static final Logger LOG = Logger.getLogger(SimilarWeb.class.getName());
    private static final String SIMILARWEB_ENDPOINT = "https://api.similarweb.com/";

    /**
     * SimilarWeb API data transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - SimilarWeb API extractor started");

        JSONObject parameters = null;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in a JsonPath format", "", true, false)
                    .addParameter("e", "endpoint", "Endpoint path", "", true, false)
                    .addParameter("b", "object", "Json object", "", true, false)
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

            //Retrives API credentials.
            String apiKey = credentials.get("api_key").toString();

            //Identifies endpoint parameters. 
            String endpointParameter = cli.getParameter("parameters");

            if (endpointParameter != null && !endpointParameter.isEmpty()) {
                try {
                    parameters = (JSONObject) parser.parse(endpointParameter);
                } catch (ParseException ex) {
                    LOG.log(Level.INFO, "Fail parsing endpoint parameters: {0}", endpointParameter);
                }
            }

            //Connect to the API. 
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(SIMILARWEB_ENDPOINT + cli.getParameter("endpoint"));

                //Sets default URI parameters. 
                URIBuilder uriBuilder = new URIBuilder(httpGet.getURI())
                        .addParameter("api_key", apiKey);

                //Sets endpoint URI parameters. 
                if (parameters != null && !parameters.isEmpty()) {
                    for (Object k : parameters.keySet()) {
                        uriBuilder.addParameter((String) k, (String) parameters.get(k));
                    }
                }

                //Sets URI parameters. 
                httpGet.setURI(uriBuilder.build());

                //Executes a request.
                CloseableHttpResponse response = client.execute(httpGet);

                //Gets a reponse entity. 
                String entity = EntityUtils.toString(response.getEntity(), "UTF-8");

                if (!entity.isEmpty()) {
                    JSONObject json = (JSONObject) new JSONParser().parse(entity);

                    //Identifies if there are payload to process. 
                    if (!json.isEmpty()) {
                        String status;

                        try {
                            status = JsonPath.read(json, "$.meta.status");
                        } catch (PathNotFoundException ex) {
                            status = "SUCCESS";
                        }

                        //Identifies the response status.
                        if ("SUCCESS".equalsIgnoreCase(status)
                                && response.getStatusLine().getStatusCode() == 200) {
                            Object object;

                            //Identifies which object should be picked up from the payload.
                            if ("*".equals(cli.getParameter("object"))) {
                                object = json;
                            } else {
                                object = json.get(cli.getParameter("object"));
                            }

                            //Identifies if the payload is an array or an object.
                            if (object instanceof JSONArray) {
                                if (!((JSONArray) object).isEmpty()) {
                                    ((JSONArray) object).forEach(item -> {
                                        List record = new ArrayList();

                                        mitt.getConfiguration()
                                                .getOriginalFieldName()
                                                .forEach(field -> {
                                                    try {
                                                        record.add(JsonPath.read(item, "$." + field));
                                                    } catch (PathNotFoundException ex) {
                                                        record.add("");
                                                    }
                                                });

                                        mitt.write(record);
                                    });
                                }
                            } else if (object instanceof JSONObject) {
                                if (!((JSONObject) object).isEmpty()) {
                                    List record = new ArrayList();

                                    mitt.getConfiguration()
                                            .getOriginalFieldName()
                                            .forEach(field -> {
                                                try {
                                                    record.add(JsonPath.read(object, "$." + field));
                                                } catch (PathNotFoundException ex) {
                                                    record.add("");
                                                }
                                            });

                                    mitt.write(record);
                                }
                            }
                        } else {
                            throw new Exception((String) JsonPath.read(json, "$.meta.error_message"));
                        }
                    }
                } else {
                    throw new Exception("Empty response entity for request " + httpGet.getURI());
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SimilarWeb API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SimilarWeb API extractor finalized");
    }
}
