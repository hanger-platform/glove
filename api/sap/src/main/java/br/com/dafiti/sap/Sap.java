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
package br.com.dafiti.sap;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.model.Field;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoField;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoRecordFieldIterator;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Helio Leal
 */
public class Sap {

    private static final Logger LOG = Logger.getLogger(Sap.class.getName());

    /**
     * SAPJCO3 data transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - SAPJCO3 extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved", "", true, false)
                    .addParameter("f", "function", "RFC function name", "", true, false)
                    .addParameter("i", "import", "(Optional)  Json Object - Function importation parameters.", "")
                    .addParameter("t", "tables", "(Optional)  Json Array - Function tables parameters.", "")
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

            if (!com.sap.conn.jco.ext.Environment.isDestinationDataProviderRegistered()) {

                Properties connectProperties = new Properties();
                connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, credentials.get("host").toString());
                connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, credentials.get("sysnr").toString());
                connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT, credentials.get("client").toString());
                connectProperties.setProperty(DestinationDataProvider.JCO_USER, credentials.get("user").toString());
                connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD, credentials.get("passwd").toString());
                connectProperties.setProperty(DestinationDataProvider.JCO_LANG, credentials.get("lang").toString());

                CustomDestinationDataProvider customDestinationDataProvider = new CustomDestinationDataProvider();

                Environment.registerDestinationDataProvider(customDestinationDataProvider);
                customDestinationDataProvider.changePropertiesForABAP_AS(connectProperties);
            }

            JCoDestination destination = JCoDestinationManager.getDestination("ABAP_AS");

            JCoFunction function = destination.getRepository().getFunction(cli.getParameter("function"));

            //Retrieves function importation parameters.
            JSONObject importParameters = (JSONObject) parser.parse(cli.getParameter("import"));

            if (importParameters != null && !importParameters.isEmpty()) {
                for (Object key : importParameters.keySet()) {
                    function.getImportParameterList().setValue((String) key, importParameters.get(key));
                }
            }

            //Retrieves function tables parameters.            
            JSONArray tablesParameters = (JSONArray) parser.parse(cli.getParameter("tables"));

            if (tablesParameters != null && !tablesParameters.isEmpty()) {
                for (Object tableParameter : tablesParameters) {
                    JSONObject parameter = (JSONObject) tableParameter;

                    JCoTable options = function.getTableParameterList().getTable(parameter.get("TABLE").toString());
                    JSONArray parameterValues = (JSONArray) parameter.get("VALUES");

                    for (Object values : parameterValues) {
                        JSONObject value = (JSONObject) values;

                        for (Object key : value.keySet()) {
                            options.appendRow();
                            options.setValue((String) key, value.get(key));
                        }
                    }
                }
            }

            //Identifies if function was found on SAP Server.
            if (function != null) {
                //Execute ABAP function.
                function.execute(destination);

                List record = new ArrayList();
                final JCoTable rows = function.getTableParameterList().getTable("DATA");

                for (int i = 0; i < rows.getNumRows(); i++) {
                    rows.setRow(i);
                    String[] values = rows.getString("WA").split(",");

                    for (int y = 0; y < values.length; y++) {
                        record.add(values[y]);
                    }

                    mitt.write(record);
                }
            } else {
                throw new Exception("Function not found in SAP Server");
            }

        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SAPJCO3 extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SAPJCO3 extractor finalized");
    }

    static class CustomDestinationDataProvider implements DestinationDataProvider {

        private DestinationDataEventListener destinationDataEventListener;

        private Properties ABAP_AS_properties;

        public Properties getDestinationProperties(String destinationName) {
            if (destinationName.equals("ABAP_AS") && ABAP_AS_properties != null) {
                return ABAP_AS_properties;
            }

            return null;
        }

        public void setDestinationDataEventListener(DestinationDataEventListener eventListener) {
            this.destinationDataEventListener = eventListener;
        }

        public boolean supportsEvents() {
            return true;
        }

        void changePropertiesForABAP_AS(Properties properties) {
            if (properties == null) {
                ABAP_AS_properties = null;
                destinationDataEventListener.deleted("ABAP_AS");
            } else {
                if (ABAP_AS_properties == null || !ABAP_AS_properties.equals(properties)) {
                    ABAP_AS_properties = properties;
                    destinationDataEventListener.updated("ABAP_AS");
                }
            }
        }
    }
}
