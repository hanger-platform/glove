package br.com.dafiti.jenkins;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import com.offbytwo.jenkins.JenkinsServer;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V Gomes
 */
public class JenkinsJobCreator {

    public static void main(String[] args) {
        Logger.getLogger(JenkinsJobCreator.class.getName()).info("GLOVE - Jenkins job creator started");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("l", "url", "Jenkins URL", "", true, false)
                    .addParameter("u", "user", "Jenkins user", "", true, false)
                    .addParameter("p", "password", "Jenkins password", "", true, false)
                    .addParameter("t", "template", "Job template", "", true, false)
                    .addParameter("j", "file", "Json file", "", true, false);

            //Reads command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Reads json parameter file. 
            JSONParser parser = new JSONParser();
            JSONArray parameters = (JSONArray) parser.parse(new FileReader(cli.getParameter("file")));

            if (!parameters.isEmpty()) {
                //Connects to Jenkins instance. 
                JenkinsServer jenkins = new JenkinsServer(new URI(cli.getParameter("url")), cli.getParameter("user"), cli.getParameter("password"));

                if (jenkins.isRunning()) {
                    //Reads job template XML. 
                    String templateXML = jenkins.getJobXml(cli.getParameter("template"));

                    if (templateXML != null) {
                        //Read each object in json parameter file. 
                        for (Object parameter : parameters) {
                            JSONObject attributes = (JSONObject) parameter;
                            String name = (String) attributes.get("name");

                            if (!name.isEmpty()) {
                                //Get a copy of job template XML. 
                                String jobXML = templateXML;

                                //Replace all variables in job XML. 
                                Set<String> keys = attributes.keySet();

                                for (String key : keys) {
                                    jobXML = jobXML.replace("${" + key + "}", (String) attributes.get(key));
                                }

                                //Creates or updates a job with the new XML. 
                                if (jenkins.getJob(name) != null) {
                                    jenkins.updateJob(name, jobXML, true);
                                    Logger.getLogger(JenkinsJobCreator.class.getName()).log(Level.INFO, "{0} updated", name);
                                } else {
                                    jenkins.createJob(name, jobXML, true);
                                    jenkins.enableJob(name, true);
                                    Logger.getLogger(JenkinsJobCreator.class.getName()).log(Level.INFO, "{0} created", name);
                                }
                            } else {
                                Logger.getLogger(JenkinsJobCreator.class.getName()).log(Level.SEVERE, "Name attribute is mandatory!");
                            }
                        }
                    }
                }
            }
        } catch (IOException
                | URISyntaxException
                | ParseException
                | DuplicateEntityException ex) {

            Logger.getLogger(JenkinsJobCreator.class.getName()).log(Level.SEVERE, "Job creator: ", ex);
        }

        Logger.getLogger(JenkinsJobCreator.class.getName()).info("GLOVE - Jenkins job creator finalized");
    }
}
