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
package br.com.dafiti.email;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;

/**
 * 
 * @author Valdiney V GOMES
 *
 */
public class EmailAttachment {

	private static final Logger LOG = Logger.getLogger(EmailAttachment.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LOG.info("GLOVE - Email attachment extractor started");

		// Define the mitt.
		Mitt mitt = new Mitt();

		try {
			// Defines parameters.
			mitt.getConfiguration().addParameter("c", "credentials", "Credentials file", "", true, false)
					.addParameter("o", "output", "Output file", "", true, false)
					.addParameter("f", "field", "Fields to be retrieved from e-mail attachment, concatenated by +", "", true, false)
					.addParameter("fd", "folder", "(Optional) E-mail folder, default: INBOX", "INBOX")
					.addParameter("sd", "start_date", "(Optional) E-mail received date since as YYYYMMDD", "")
					.addParameter("st", "start_time", "(Optional) E-mail received time since, default: 00:00:00", "00:00:00")
					.addParameter("ed", "end_date", "(Optional) E-mail received date to as YYYYMMDD", "")
					.addParameter("et", "end_time", "(Optional) E-mail received time to, default: 23:59:59", "23:59:59")
					.addParameter("fr", "from", "(Optional) E-mail from condition", "")
					.addParameter("s", "subject", "(Optional) E-mail subject condition", "")
					.addParameter("p", "pattern", "(Optional)  Attachment file name pattern in a RegExp fashion, default: .csv|.xls|.xlsx|.avro|.gz|.zip", ".csv|.xls|.xlsx|.avro|.gz|.zip")
					.addParameter("a", "partition", "Partition field, concatenated by +", "")
					.addParameter("k", "key", "(Unique key field, concatenated by +", "")
					.addParameter("d", "delimiter", "(Optional) File delimiter, default ;", ";")
					.addParameter("pr", "properties", "(Optional) MITT reader propertiess", "")
					.addParameter("b", "backup", "(Optional) Original attachment backup folder", "");

			// Reads the command line interface.
			CommandLineInterface cli = mitt.getCommandLineInterface(args);

			// Defines output file.
			mitt.setOutputFile(cli.getParameter("output"));

			// Defines the delimiter.
			mitt.getReaderSettings().setDelimiter(cli.getParameter("delimiter").charAt(0));

			// Defines the reader properties.
			if (cli.getParameter("properties") != null) {
				mitt.getReaderSettings().setProperties(cli.getParameter("properties"));
			}

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

			configuration.addCustomField("etl_load_date", new Now()).addField(cli.getParameterAsList("field", "\\+"));

			// Reads the credentials file.
			JSONParser parser = new JSONParser();
			JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));
			JSONArray connection = (JSONArray) credentials.get("connection");

			Properties properties = new Properties();

			for (Object object : connection) {
				JSONObject property = (JSONObject) object;
				for (Object key : property.keySet()) {
					properties.setProperty(key.toString(), property.get(key).toString());
				}
			}

			// Defines a session.
			Store store = Session.getDefaultInstance(properties).getStore(credentials.get("protocol").toString());
			store.connect(credentials.get("email").toString(), credentials.get("password").toString());

			// Defines a folder to be read.
			Folder folder = store.getFolder(cli.getParameter("folder"));
			folder.open(Folder.READ_ONLY);

			// Defines search conditions.
			List<SearchTerm> searchTerm = new ArrayList<SearchTerm>();

			// Message from condition.
			if (cli.getParameter("from") != null) {
				searchTerm.add(new FromStringTerm(cli.getParameter("from")));
			}

			// Message from condition.
			if (cli.getParameter("subject") != null) {
				searchTerm.add(new SubjectTerm(cli.getParameter("subject")));
			}

			// Message received date condition.
			if (cli.getParameter("start_date") != null && cli.getParameter("end_date") != null) {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				searchTerm.add(new ReceivedDateTerm(ComparisonTerm.GE,
						formatter.parse(cli.getParameter("start_date") + " " + cli.getParameter("start_time"))));
				searchTerm.add(new ReceivedDateTerm(ComparisonTerm.LE,
						formatter.parse(cli.getParameter("end_date") + " " + cli.getParameter("end_time"))));
			}

			// Define the output path.
			Path outputPath = Files.createTempDirectory("mail_");

			if (cli.getParameter("backup") != null) {
				Files.createDirectories(Paths.get(cli.getParameter("backup")));
			}

			// Attachment file name RegExp pattern.
			Pattern pattern = Pattern.compile(cli.getParameter("pattern"));

			// Search messages that meet the search conditions.
			Message[] messages = folder.search(new AndTerm(searchTerm.toArray(new SearchTerm[searchTerm.size()])));

			for (int i = 0; i < messages.length; i++) {
				Message message = messages[i];

				if (message.getContentType().contains("multipart")) {
					Multipart multiPart = (Multipart) message.getContent();

					for (int parts = 0; parts < multiPart.getCount(); parts++) {
						MimeBodyPart mimeBodyPart = (MimeBodyPart) multiPart.getBodyPart(parts);

						if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
							Date date = message.getReceivedDate();
							String from = message.getFrom()[0].toString();
							String subject = message.getSubject();
							String filename = mimeBodyPart.getFileName();

							// Identifies if a attachment matches the defined filename pattern.
							Matcher matcher = pattern.matcher(filename);

							if (matcher.find()) {
								mimeBodyPart
										.saveFile(outputPath + File.separator + date + "_" + subject + "_" + filename);

								// Identifies if should do a copy.
								if (cli.getParameter("backup") != null) {
									Files.copy(
											Paths.get(outputPath + File.separator + date + "_" + subject + "_"
													+ filename),
											Paths.get(
													cli.getParameter("backup") + date + "_" + subject + "_" + filename),
											StandardCopyOption.REPLACE_EXISTING);
								}

								LOG.info("GLOVE - Downloading attachment " + filename + " of message " + subject
										+ " from " + from + " at " + date);
							} else {
								LOG.info("GLOVE - Skipping not allowed attachment " + filename + " of message "
										+ subject + " from " + from + " at " + date);
							}
						}
					}
				}
			}

			folder.close(false);
			store.close();

			LOG.info("GLOVE -  Writing output file to " + cli.getParameter("output"));

			// Write to the output file.
			mitt.write(outputPath.toFile(), "*");
			Files.delete(outputPath);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "GLOVE - Email attachment extractor fail: ", ex);
			System.exit(1);
		} finally {
			LOG.info("GLOVE - Email attachment extractor finalized.");
			mitt.close();
		}
	}
}
