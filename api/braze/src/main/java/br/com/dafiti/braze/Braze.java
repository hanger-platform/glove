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
package br.com.dafiti.braze;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class Braze {

    /**
     * Braze File transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        Logger.getLogger(Braze.class.getName()).info("GLOVE - Braze Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("t", "type", "(Optional) Extract type. Options: detail_list and detail; detail_list is default.", "detail_list")
                    .addParameter("s", "service", "Identifies the service name", "", true, false)
                    .addParameter("el", "endpoint_list", "(Optional) Identifies the endpoint that contains a list to extract data from", "")
                    .addParameter("ed", "endpoint_detail", "Identifies the endpoint that contains the details of each list item", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from the file", "", true, false)
                    .addParameter("sl", "sleep", "(Optional) Sleep time in seconds at one request and another; 0 is default", "0")
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Identifies the approprieted report to extract.
            switch (cli.getParameter("type").toLowerCase()) {
                case "detail_list":
                    if ((cli.getParameter("endpoint_list") != null) && (!cli.getParameter("endpoint_list").isEmpty())) {
                        new DetailList(cli.getParameter("endpoint_list"),
                                cli.getParameter("endpoint_detail"),
                                cli.getParameter("output"),
                                cli.getParameter("service"),
                                cli.getParameterAsList("key", "\\+"),
                                cli.getParameterAsList("partition", "\\+"),
                                cli.getParameterAsList("field", "\\+"),
                                cli.getParameterAsInteger("sleep"),
                                credentials).extract();
                    } else {
                        Logger.getLogger(Braze.class.getName()).log(
                                Level.SEVERE, "GLOVE - endpoint_list is empty. For type List, it is necessary to define the endpoint_list."
                        );
                    }

                    break;

                case "detail":
                    new Detail(cli.getParameter("endpoint_detail"),
                            cli.getParameter("output"),
                            cli.getParameter("service"),
                            cli.getParameterAsList("key", "\\+"),
                            cli.getParameterAsList("partition", "\\+"),
                            cli.getParameterAsList("field", "\\+"),
                            cli.getParameterAsInteger("sleep"),
                            credentials).extract();
                    break;

                default:
                    Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, "GLOVE - Service {0} not yet implemented", cli.getParameter("service"));
            }

        } catch (DuplicateEntityException
                | FileNotFoundException
                | ParseException ex) {

            Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, "GLOVE - Braze fail: ", ex);
            System.exit(1);
        } catch (IOException ex) {

            Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, "GLOVE - Braze fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(Braze.class.getName()).info("GLOVE - Braze Extractor finalized");
    }
}
