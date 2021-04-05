package br.com.dafiti.zoom;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

/**
 *
 * @author Valdiney V GOMES
 */
public class Zoom {

    private static final Logger LOG = Logger.getLogger(Zoom.class.getName());
    private static final String ZOOM_ENDPOINT = "http://anunciante.zoom.com.br/";

    public static void main(String[] args) throws IOException {
        LOG.info("Glove - Zoom Extractor started");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from input file", "", true, false)
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Runs the crawler to export accountstatement report. 
            Response loginForm = Jsoup.connect(ZOOM_ENDPOINT)
                    .method(Connection.Method.GET)
                    .execute();

            Response mainPage = Jsoup.connect(ZOOM_ENDPOINT + "zoomout/j_security_check")
                    .data("j_username", credentials.get("username").toString())
                    .data("j_password", credentials.get("password").toString())
                    .cookies(loginForm.cookies())
                    .execute();

            Response report = Jsoup.connect(
                    new URIBuilder(ZOOM_ENDPOINT + "accountstatement")
                            .addParameter("merchantId", credentials.get("account").toString())
                            .addParameter("dateStart",
                                    new SimpleDateFormat("dd/MM/yyyy")
                                            .format(
                                                    new SimpleDateFormat("yyyy-MM-dd")
                                                            .parse((String) cli.getParameter("start_date"))))
                            .addParameter("dateEnd",
                                    new SimpleDateFormat("dd/MM/yyyy").format(
                                            new SimpleDateFormat("yyyy-MM-dd")
                                                    .parse((String) cli.getParameter("end_date"))))
                            .addParameter("action", "export").build().toString())
                    .cookies(mainPage.cookies())
                    .execute();

            //Saves the report data. 
            Path outputPath = Files.createTempDirectory("zoom_");
            FileUtils.writeByteArrayToFile(
                    new File(outputPath.toString() + "/" + "zoom.csv"),
                    report.bodyAsBytes());

            //Defines the reader delimiter as comma. 
            mitt.getReaderSettings().setDelimiter(',');
  
            //Write the data to output file. 
            mitt.write(outputPath.toFile(), "*.csv");
            FileUtils.deleteDirectory(outputPath.toFile());
        } catch (DuplicateEntityException
                | IOException
                | URISyntaxException
                | java.text.ParseException
                | ParseException ex) {

            LOG.log(Level.SEVERE, "Zoom Extractor failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            LOG.info("Glove - Zoom Extractor finalized.");
            System.exit(0);
        }
    }
}
