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
package br.com.dafiti.stylight;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class Stylight {

    private static final Logger LOG = Logger.getLogger(Stylight.class.getName());
    private static final String STYLIGHT_TOKEN_URL = "https://auth.partner.stylight.net/partner/authorization";

    public static void main(String[] args) {
        LOG.info("Glove - Stylight Extractor started");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from input file", "", true, false)
                    .addParameter("s", "endpoint", "Endpoint URL", "", true, false)
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one field")
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
            String username = URLEncoder.encode(credentials.get("username").toString(), "UTF-8");
            String password = URLEncoder.encode(credentials.get("password").toString(), "UTF-8");

            //Retrieves API Token.
            HttpResponse<String> token = Unirest.post(STYLIGHT_TOKEN_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("username=" + username + "&password=" + password)
                    .asString();

            if (!token.getBody().isEmpty()) {

                //Executes API call.
                HttpResponse<String> response = Unirest.get(cli.getParameter("endpoint"))
                        .header("Accept", "text/csv")
                        .header("Authorization", "Bearer " + token.getBody())
                        .asString();

                //Defines a temporary path. 
                Path outputPath = Files.createTempDirectory("stylight_");

                //Defines a temporary output file. 
                File targetFile = new File(outputPath.toString() + "/" + UUID.randomUUID() + ".tmp");

                //Writes API response to temporary output file.
                FileUtils.copyInputStreamToFile(response.getRawBody(), targetFile);

                //Writes the final file.
                mitt.getReaderSettings().setDelimiter(',');
                mitt.write(outputPath.toFile());

                //Remove temporary path. 
                Files.delete(outputPath);
            }

        } catch (IOException
                | DuplicateEntityException
                | UnirestException
                | ParseException ex) {

            LOG.log(Level.SEVERE, "Stylight Extractor failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            LOG.info("Glove - Stylight Extractor finalized.");
            System.exit(0);
        }
    }
}
