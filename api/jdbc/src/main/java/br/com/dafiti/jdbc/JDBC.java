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
package br.com.dafiti.jdbc;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;

import com.github.vertical_blank.sqlformatter.SqlFormatter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;

/**
 *
 * @author Valdiney V GOMES
 */
public class JDBC {

	private static final Logger LOG = Logger.getLogger(JDBC.class.getName());

	public static void main(String[] args) {
		LOG.info("GLOVE - JDBC extractor started");

		int row = 0;
		boolean all = false;
		String query = null;
		Mitt mitt = new Mitt();
		ResultSet resultset = null;
		StringBuilder sql = new StringBuilder();
		List<String> fields = new ArrayList<String>();

		try {
			// Defines parameters.
			mitt.getConfiguration().addParameter("c", "credentials", "Credentials file", "", true, false)
					.addParameter("o", "output", "Output file", "", true, false)
					.addParameter("d", "delimiter", "(Optional)  Output delimiter, default as ;", ";")
					.addParameter("f", "field", "(Optional) Fields to be retrieved, concatenated by +", "")
					.addParameter("c", "catalog", "(Optional) Catalog name", "")
					.addParameter("s", "schema", "(Optional) Schema name", "")
					.addParameter("t", "table", "(Optional) Table name", "")
					.addParameter("sq", "sql",	"(Optional) SQL SELECT statement. Its possible to read the SELECT statement from a file using: file://<.sql file path>", "")
					.addParameter("ft", "filter", "(Optional) SQL filter condition", "")
					.addParameter("a", "partition", "(Optional) Partition field, concatenated by +", "")
					.addParameter("k", "key", "(Optional) Unique key field, concatenated by +", "")
					.addParameter("p", "parameter",	"(Optional) Credentials and SQL SELECT statement replacement variable, in a bash fashion",	"")
					.addParameter("h", "no_header", "(Optional) Identifies if output file should have a header", false);

			// Reads the command line interface.
			CommandLineInterface cli = mitt.getCommandLineInterface(args);

			// Defines output file.
			mitt.setOutputFile(cli.getParameter("output"));
			mitt.getWriterSettings().setDelimiter(cli.getParameter("delimiter").charAt(0));
			mitt.getWriterSettings().setHeaderEnabled(!cli.hasParameter("no_header"));

			// Defines fields.
			Configuration configuration = mitt.getConfiguration();

			if (cli.hasParameter("partition")) {
				configuration.addCustomField("partition_field",
						new Concat((List) cli.getParameterAsList("partition", "\\+")));
			}
			if (cli.hasParameter("key")) {
				configuration.addCustomField("custom_primary_key",
						new Concat((List) cli.getParameterAsList("key", "\\+")));
			}

			configuration.addCustomField("etl_load_date", new Now());

			// Identifies if should replace credentials and SQL parameters.
			Map parameters = new HashMap();

			if (cli.getParameter("parameter") != null) {
				parameters = new Gson().fromJson(cli.getParameter("parameter"), HashMap.class);
			}

			// Defines variable replace rule.
			StringSubstitutor replacer = new StringSubstitutor(parameters);

			// Reads the credentials.
			JsonObject credentials = JsonParser.parseString(replacer.replace(
					FileUtils.readFileToString(new File(cli.getParameter("credentials")), StandardCharsets.UTF_8)))
					.getAsJsonObject();

			LOG.info("GLOVE - Connecting to the database ...");

			// Defines which JDBC class should be loaded.
			URLClassLoader urlClassLoader = new URLClassLoader(
					new URL[] { new URL("file://" + credentials.get("JDBCDriverFile").getAsString()) });

			// Registers a driver.
			DriverManager.registerDriver(new DriverShim((Driver) Class
					.forName(credentials.get("JDBCDriverClass").getAsString(), true, urlClassLoader).newInstance()));

			// Defines a connection.
			Connection connection = DriverManager.getConnection(credentials.get("JDBCUrl").getAsString(),
					credentials.get("username").getAsString(), credentials.get("password").getAsString());

			LOG.info("GLOVE - Quering data on database...");

			// Identifies the fields.
			if (cli.getParameter("field") == null) {
				all = true;
				LOG.info("GLOVE - MITT Engine disabled");
			} else {
				configuration.addField(cli.getParameterAsList("field", "\\+"));
			}

			// Creates a statement.
			Statement statement = connection.createStatement();

			// Identifies if it is a default dump or a customized SQL.
			if (cli.getParameter("sql") == null) {
				// Get the table columns.
				ResultSet columns = connection.getMetaData().getColumns(
						cli.getParameter("catalog") == null ? cli.getParameter("schema") : cli.getParameter("catalog"),
						cli.getParameter("schema"), cli.getParameter("table"), null);

				// Identifies if a column exists in the table or is a transformation.
				while (columns.next()) {
					String field = columns.getString("COLUMN_NAME");

					if (configuration.getOriginalFieldName().contains(columns.getString("COLUMN_NAME")) || all) {
						fields.add(field);

						if (all) {
							configuration.addField(field);
						}
					}
				}

				// Builds the SQL SELECT statement.
				sql.append("SELECT " + String.join(", ", fields) + " FROM ");

				if (cli.getParameter("catalog") != null) {
					sql.append(cli.getParameter("catalog")).append(".");
				}

				if (cli.getParameter("schema") != null) {
					sql.append(cli.getParameter("schema")).append(".");
				}

				sql.append(cli.getParameter("table"));

				if (cli.getParameter("filter") != null) {
					sql.append(" WHERE ").append(cli.getParameter("filter"));
				}

				query = replacer.replace(sql.toString());

				LOG.info("GLOVE - SQL SELECT statement: " + SqlFormatter.format(query));

				// Runs the SQL SELECT statement.
				resultset = statement.executeQuery(sql.toString());
			} else {
				// Loads the customized SQL SELECT statement from parameter or from a file.
				if (cli.getParameter("sql").startsWith("file://") && cli.getParameter("sql").endsWith(".sql")) {

					sql.append(FileUtils.readFileToString(new File(cli.getParameter("sql").replace("file://", "")),
							StandardCharsets.UTF_8));
				} else {
					sql.append(cli.getParameter("sql"));
				}

				query = replacer.replace(sql.toString());

				LOG.info("GLOVE - Customized SQL SELECT statement: " + SqlFormatter.format(query));

				// Runs the SQL SELECT statement.
				resultset = statement.executeQuery(query);

				// Identifies the resultSet metadata.
				ResultSetMetaData metaData = resultset.getMetaData();

				// Identifies if a column exists in the table or is a transformation.
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					String field = metaData.getColumnName(i);

					if (configuration.getOriginalFieldName().contains(field) || all) {
						fields.add(field);

						if (all) {
							configuration.addField(field);
						}
					}
				}
			}

			LOG.info("GLOVE - Fetching data from the server...");

			while (resultset.next()) {
				row = resultset.getRow();
				List<String> record = new ArrayList<String>();

				// Reads the resultSet.
				for (String field : fields) {
					record.add(resultset.getString(resultset.findColumn(field)));
				}

				// Logs the progress.
				if (row % 100000 == 0) {
					LOG.info("GLOVE - " + row + " rows written... ");
				}

				mitt.write(record);
			}

			LOG.info("GLOVE - " + row + " rows written to output file " + cli.getParameter("output"));
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "GLOVE - JDBC extractor fail: ", ex);
			System.exit(1);
		} finally {
			LOG.info("GLOVE - JDBC extractor finalized.");
			mitt.close();
		}
	}

	/**
	 * Driver shim to get around the class loader issue with the DriverManager.
	 * 
	 * @author Jeremy Long
	 *
	 */
	static class DriverShim implements Driver {

		private Driver driver;

		DriverShim(Driver d) {
			this.driver = d;
		}

		public boolean acceptsURL(String u) throws SQLException {
			return this.driver.acceptsURL(u);
		}

		public Connection connect(String u, Properties p) throws SQLException {
			return this.driver.connect(u, p);
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return this.driver.getPropertyInfo(url, info);
		}

		@Override
		public int getMajorVersion() {
			return this.driver.getMajorVersion();
		}

		@Override
		public int getMinorVersion() {
			return this.driver.getMinorVersion();
		}

		@Override
		public boolean jdbcCompliant() {
			return this.driver.jdbcCompliant();
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return this.driver.getParentLogger();
		}
	}
}
