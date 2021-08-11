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
package br.com.dafiti.inmet;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;

/**
 *
 * @author Valdiney V GOMES
 */
public class Inmet {
	private static final Logger LOG = Logger.getLogger(Inmet.class.getName());
	private static final String INMET_ENDPOINT = "https://apitempo.inmet.gov.br/";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		LOG.info("GLOVE - INMET API extractor started");

		// Define the mitt.
		Mitt mitt = new Mitt();

		try {
			// Defines parameters.
			mitt.getConfiguration()
					.addParameter("o", "output", "Output file", "", true, false)
					.addParameter("f", "field", "Fields to be retrieved in a JsonPath fashion", "", true, false)
					.addParameter("e", "endpoint", "Endpoint name", "", true, false)
					.addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
					.addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

			// Reads the command line interface.
			CommandLineInterface cli = mitt.getCommandLineInterface(args);

			// Defines output file.
			mitt.setOutputFile(cli.getParameter("output"));

			// Defines fields.
			Configuration configuration = mitt.getConfiguration();

			// Adds technical fields.
			if (cli.hasParameter("partition")) {
				configuration.addCustomField("partition_field",
						new Concat((List) cli.getParameterAsList("partition", "\\+")));
			}

			if (cli.hasParameter("key")) {
				configuration.addCustomField("custom_primary_key",
						new Concat((List) cli.getParameterAsList("key", "\\+")));
			}

			configuration.addCustomField("etl_load_date", new Now());

			// Adds user defined fields.
			configuration.addField(cli.getParameterAsList("field", "\\+"));

			try (CloseableHttpClient client = HttpClients.createDefault()) {
				HttpGet httpGet = new HttpGet(INMET_ENDPOINT + cli.getParameter("endpoint"));
	
				// Executes a request.
				CloseableHttpResponse response = client.execute(httpGet);
	
				StatusLine statusLine = response.getStatusLine();     
				
				if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
					// Gets a response entity.
					String entity = EntityUtils.toString(response.getEntity(), "UTF-8");
	
					if (!entity.isEmpty()) {
						JSONArray json = (JSONArray) new JSONParser().parse(entity);
	
						json.forEach(item -> {
							List<Object> record = new ArrayList<>();
	
							mitt.getConfiguration().getOriginalFieldName().forEach(field -> {
								try {
									record.add(JsonPath.read(item, "$." + field));
								} catch (PathNotFoundException ex) {
									record.add("");
								}
							});
	
							mitt.write(record);
						});
						
						LOG.info("GLOVE - INMET API endpoint " + cli.getParameter("endpoint") + " records write to output file " + cli.getParameter("output"));
					}
				}else {
					LOG.log(Level.SEVERE, "GLOVE - INMET API extractor request fail: HTTP Status ", statusLine.getStatusCode());
					System.exit(1);	
				}
			}
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "GLOVE - INMET API extractor fail: ", ex);
			System.exit(1);
		} finally {
			mitt.close();
		}

		LOG.info("GLOVE - INMET API extractor finalized");
	}
}
