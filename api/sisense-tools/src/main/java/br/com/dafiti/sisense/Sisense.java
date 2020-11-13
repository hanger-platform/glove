package br.com.dafiti.sisense;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.sisense.action.ElastiCubeStartBuild;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class Sisense {

    private static final Logger LOG = Logger.getLogger(Sisense.class.getName());

    public static void main(String[] args) throws IOException, ParseException {
        LOG.info("GLOVE - Sisense tools started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Define parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("s", "server", "Sisense server", "", true, false)
                    .addParameter("e", "cube", "ElastCube", "", true, false)
                    .addParameter("a", "action", "Sisense API Action", "startBuild", true, false)
                    .addParameter("t", "type", "Build type", "Full");

            //Read the command line interface.
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines the JSON parser.
            JSONParser parser = new JSONParser();

            //Reads the credentials json file. 
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Identifies the api action.
            if (cli.getParameter("action").equalsIgnoreCase("startBuild")) {
                new ElastiCubeStartBuild(
                        (String) credentials.get("token"),
                        cli.getParameter("server"),
                        cli.getParameter("cube"),
                        cli.getParameter("type")).startBuild();
            }
        } catch (DuplicateEntityException ex) {
            LOG.log(Level.SEVERE, "Fail on Sisense API", ex);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - Sisense tools finished");
    }
}
