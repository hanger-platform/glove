/*
 * Copyright (c) 2022 Dafiti Group
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
package br.com.dafiti.quicksight;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import br.com.dafiti.quicksight.resources.FactoryDescribable;
import br.com.dafiti.quicksight.resources.Describable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Helio Leal
 */
public class Quicksight {

    private static final Logger LOG = Logger.getLogger(Quicksight.class.getName());

    public static void main(String[] args) {
        LOG.info("GLOVE - Quicksight extractor started");

        // Define the mitt.
        Mitt mitt = new Mitt();

        try {

            // Defines parameters.
            mitt.getConfiguration()
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("r", "resource", "AWS quicksight resource", "", true, false)
                    .addParameter("z", "region", "AWS region", "", true, false)
                    .addParameter("a", "account", "AWS account id", "", true, false)
                    .addParameter("n", "namespace", "AWS quicksight namespace", "", true, false)
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
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

            Describable describable = FactoryDescribable.getDescribable(
                    cli.getParameter("resource"),
                    cli.getParameter("region"),
                    cli.getParameter("account"),
                    cli.getParameter("namespace"));

            describable.setFields(configuration);
            describable.extract(mitt);

        } catch (DuplicateEntityException ex) {
            LOG.log(Level.SEVERE, "GLOVE - Quicksight extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - Quicksight extractor finalized");
    }
}
