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
package br.com.dafiti.sapjco3;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Helio Leal
 */
public class Sapjco3 {

    private static final Logger LOG = Logger.getLogger(Sapjco3.class.getName());

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
                    .addParameter("f", "function", "RFC function name", "", true, false)
                    .addParameter("t", "table", "table name", "", true, false)
                    .addParameter("fi", "field", "(Optional)  Fields to be retrieved, if not passed, try to get all of them.", "")
                    .addParameter("i", "where", "(Optional)  Where condition.", "")
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                    .addParameter("d", "input_delimiter", "(Optional) SAPJCO3 function resultset return delimiter; '|' as default", "|")
                    .addParameter("r", "row_count", "(Optional) how many records will return at once; '0' as default and means return everything", "0")
                    .addParameter("d", "delimiter", "(Optional) SAPJCO3 function resultset return delimiter; '|' as default", "\\|");

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
                    .addCustomField("etl_load_date", new Now());

            if (cli.hasParameter("field")) {
                configuration.addField(cli.getParameterAsList("field", "\\+"));
            }

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

            int rowCount = cli.getParameterAsInteger("row_count");
            int rowSkips = 0;
            int numRows = ZRFCGetTableCount(destination, cli);

            LOG.log(Level.INFO, "Table {0} has {1} records [WHERE CONDITION: {2}].", new Object[]{cli.getParameter("table"), numRows, cli.getParameter("where")});

            //Retrieves data based on row count.
            do {
                JCoFunction function = destination.getRepository().getFunction(cli.getParameter("function"));

                //Identifies if function was found on SAP Server.
                if (function != null) {
                    //Defines RFC import parameters.
                    function.getImportParameterList().setValue("QUERY_TABLE", cli.getParameter("table"));
                    function.getImportParameterList().setValue("DELIMITER", cli.getParameter("input_delimiter"));

                    //Identifies how many rows will retrieve at once.
                    if (rowCount > 0) {
                        function.getImportParameterList().setValue("ROWCOUNT", rowCount);
                        function.getImportParameterList().setValue("ROWSKIPS", rowSkips);
                    }

                    //Defines RFC tables parameters.
                    if (cli.getParameter("where") != null) {
                        JCoTable options = function.getTableParameterList().getTable("OPTIONS");
                        options.appendRow();
                        options.setValue("TEXT", cli.getParameter("where"));
                    }

                    if (cli.getParameter("field") != null) {
                        JCoTable options = function.getTableParameterList().getTable("FIELDS");

                        for (String field : cli.getParameterAsList("field", "\\+")) {
                            options.appendRow();
                            options.setValue("FIELDNAME", field.toUpperCase());
                        }
                    }

                    //Executes function.
                    function.execute(destination);

                    //If no fields were informed, try to automatically get fields.
                    if (!cli.hasParameter("field")) {
                        final JCoTable fields = function.getTableParameterList().getTable("FIELDS");

                        //Fields are only set once.
                        boolean hasFields = configuration.getOriginalField().size() > 0;

                        if (!hasFields) {
                            for (int i = 0; i < fields.getNumRows(); i++) {
                                fields.setRow(i);
                                configuration.addField(fields.getString("FIELDNAME"));
                            }
                        }
                    }

                    final JCoTable rows = function.getTableParameterList().getTable("DATA");

                    LOG.log(Level.INFO, "This request returned {0} rows [ROWCOUNT: {1}, ROWSKIPS: {2}].", new Object[]{rows.getNumRows(), rowCount, rowSkips});

                    for (int i = 0; i < rows.getNumRows(); i++) {
                        List record = new ArrayList();

                        rows.setRow(i);
                        String row = rows.getString("WA") + "\n";
                        String[] values = row.split(cli.getParameter("delimiter"));

                        for (int y = 0; y < values.length; y++) {
                            record.add(values[y].trim());
                        }

                        mitt.write(record);
                    }

                    //Increments lines to skip.
                    rowSkips += rowCount;

                } else {
                    throw new Exception("Function not found in SAP Server");
                }
                //Identifies if row count is greater than 0 and if rowskips reached limit.
            } while (rowCount > 0 && rowSkips < numRows);

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

    /**
     *
     * @param destination JCoDestination
     * @param cli CommandLineInterface
     * @return Number of entries of a table.
     * @throws JCoException
     */
    static private int ZRFCGetTableCount(JCoDestination destination, CommandLineInterface cli) throws JCoException {
        JCoFunction function = destination.getRepository().getFunction("ZRFC_GET_TABLE_COUNT");
        function.getImportParameterList().setValue("TABLE_NAME", cli.getParameter("table"));

        //Defines RFC tables parameters.
        if (cli.getParameter("where") != null) {
            function.getImportParameterList().setValue("WHERE_CONDITION", cli.getParameter("where"));
        }

        //Executes function.
        function.execute(destination);

        return function.getExportParameterList().getInt("COUNT");
    }
}
