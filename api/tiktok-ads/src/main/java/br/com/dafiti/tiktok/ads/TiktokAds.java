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
package br.com.dafiti.tiktok.ads;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.io.FileReader;
import okhttp3.*;
import org.apache.http.client.utils.URIBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Fernando Saga
 */
public class TiktokAds {

    private static final Logger LOG = Logger.getLogger(TiktokAds.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * TikTok Ads Extractor
     *
     * @param args cli parameters provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - Tiktok Ads Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in a JsonPath fashion", "", true, false)
                    .addParameter("pt", "path", "Path", "", true, false)
                    .addParameter("pr", "parameters", "Endpoint parameters", "", true, false)
                    .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
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
            String access_token = credentials.get("access_token").toString();
            String path = cli.getParameter("path");

            //Total number of pages.
            long totalPage = 0;
            
            //Current page number.
            int page = 1;
            
            //Number of records per page.
            long pageSize = 10;
            
            //Total number of records.
            long totalNumber = 0;

            do {
                //Identifies endpoint parameters. 
                String endpointParameter = cli.getParameter("parameters");

                JSONObject json = (JSONObject) parser.parse(endpointParameter);
                //Set page parameter for loop iteration.
                json.remove("page");
                json.put("page", page);

                endpointParameter = json.toJSONString();

                String response = get(endpointParameter, access_token, path);

                if (!response.isEmpty()) {
                    JSONObject object = new JSONObject();

                    try {
                        object = (JSONObject) new JSONParser().parse(response);
                    } catch (ParseException ex) {
                        throw new Exception("Could not parse entity. " + response);
                    }

                    if (!object.isEmpty()) {
                        //API return status code.
                        long code = JsonPath.read(object, "$.code");

                        if (code == 0) { //OK
                            totalPage = JsonPath.read(object, "$.data.page_info.total_page");
                            pageSize = JsonPath.read(object, "$.data.page_info.page_size");
                            totalNumber = JsonPath.read(object, "$.data.page_info.total_number");

                            if (page == 1) {
                                LOG.log(Level.INFO, "Total found: {0} record(s)", totalNumber);
                            }

                            LOG.log(Level.INFO, "Page: {0} / {1} ({2} per page)", new Object[]{page, totalPage, pageSize});

                            if (totalPage > 0) {
                                //List is the default position of the object that contains the data.
                                Object list = JsonPath.read(object, "$.data.list");

                                //Gets original fields.
                                List<String> originalFields = mitt.getConfiguration().getOriginalFieldName();

                                ((JSONArray) list).forEach(item -> {
                                    List record = new ArrayList();

                                    for (String field : originalFields) {
                                        Object value = JsonPath.read(item, "$." + field);

                                        if (value != null) {
                                            record.add(value);
                                        } else {
                                            record.add("");
                                        }
                                    }

                                    mitt.write(record);
                                });
                            }
                        } else {
                            throw new Exception("API Response: " + response);
                        }
                    }
                }

                page++;

            } while (totalPage >= page);

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - Tiktok Ads Extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - Tiktok Ads Extractor finalized");
    }

    /**
     * Build request URL
     *
     * @param path Request path
     * @return Request URL
     */
    private static String buildUrl(String path) throws URISyntaxException {
        URI uri = new URI("https", "ads.tiktok.com", path, "", "");
        return uri.toString();
    }

    /**
     * Send GET request
     *
     * @param jsonStr:Args in JSON format
     * @return Response in JSON format
     */
    private static String get(String jsonStr, String access_token, String path) throws IOException, URISyntaxException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        URIBuilder ub = new URIBuilder(buildUrl(path));
        Map< String, Object> map = mapper.readValue(jsonStr, Map.class);
        map.forEach((k, v) -> {
            try {
                ub.addParameter(k, v instanceof String ? (String) v : mapper.writeValueAsString(v));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        URL url = ub.build().toURL();

        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Access-Token", access_token)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
