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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
        //configure the appender
        ConsoleAppender console = new ConsoleAppender();
        String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.DEBUG);
        console.activateOptions();
        //add appender to any Logger (here is root)
        Logger.getRootLogger().addAppender(console);

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
                    .addParameter("r", "row_skips", "(Optional) starts getting data at what record index; '0' as default", "0")
                    .addParameter("d", "delimiter", "(Optional) SAPJCO3 function resultset return delimiter; '|' as default", "\\|")
                    .addParameter("l", "id", "(Optional) identifies if table has id with lraw type", "")
                    .addParameter("de", "debug", "(Optional) debug mode; 'false' as default", "false");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines the log level.
            if (cli.getParameterAsBoolean("debug")) {
                LOG.setLevel(Level.DEBUG);
            } else {
                LOG.setLevel(Level.INFO);
            }

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
            int rowSkips = cli.getParameterAsInteger("row_skips");
            int numRows = ZRFCGetTableCount(destination, cli);

            LOG.log(Level.INFO, "Table " + cli.getParameter("table") + " has " + numRows + " records [WHERE CONDITION: " + cli.getParameter("where") + "].");

            //Retrieves data based on row count.
            do {
                LOG.debug("Before " + cli.getParameter("function") + " execute");
                JCoFunction function = destination.getRepository().getFunction(cli.getParameter("function"));

                //Identifies if function was found on SAP Server.
                if (function != null) {
                    //Defines RFC import parameters.
                    function.getImportParameterList().setValue("QUERY_TABLE", cli.getParameter("table"));
                    function.getImportParameterList().setValue("DELIMITER", cli.getParameter("input_delimiter"));

                    if (cli.hasParameter("id")) {
                        function.getImportParameterList().setValue("ID", cli.getParameterAsInteger("id"));
                    }

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

                    LOG.debug("After " + cli.getParameter("function") + " execute");

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

                    LOG.log(Level.INFO, "This request returned " + rows.getNumRows() + " rows [ROWCOUNT: " + rowCount + ", ROWSKIPS: " + rowSkips + "].");

                    LOG.debug("Before mitt write.");

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

                    LOG.debug("After mitt write.");

                    //Increments lines to skip.
                    rowSkips += rowCount;

                } else {
                    throw new Exception("Function not found in SAP Server");
                }
                //Identifies if row count is greater than 0 and if rowskips reached limit.
            } while (rowCount > 0 && rowSkips < numRows);

        } catch (Exception ex) {
            LOG.log(Level.ERROR, "GLOVE - SAPJCO3 extractor fail: ", ex);
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
        LOG.debug("Before ZRFC_GET_TABLE_COUNT execute.");

        JCoFunction function = destination.getRepository().getFunction("ZRFC_GET_TABLE_COUNT");
        function.getImportParameterList().setValue("TABLE_NAME", cli.getParameter("table"));

        //Defines RFC tables parameters.
        if (cli.getParameter("where") != null) {
            function.getImportParameterList().setValue("WHERE_CONDITION", cli.getParameter("where"));
        }

        //Executes function.
        function.execute(destination);

        LOG.debug("After ZRFC_GET_TABLE_COUNT execute.");

        return function.getExportParameterList().getInt("COUNT");
    }
}
