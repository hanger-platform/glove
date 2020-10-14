/*
 * Copyright (c) 2019 Dafiti Group
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
package br.com.dafiti.mitt;

import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.output.Output;
import br.com.dafiti.mitt.output.OutputProcessor;
import br.com.dafiti.mitt.settings.ReaderSettings;
import br.com.dafiti.mitt.settings.WriterSettings;
import com.jcabi.manifests.Manifests;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @author Valdiney V GOMES
 */
public class Mitt {

    private boolean debug = false;
    private Output output;
    private OutputProcessor outputProcessor;
    private Configuration configuration;
    private ReaderSettings readerSettings;
    private WriterSettings writerSettings;
    private CommandLineInterface commandLineInterface;

    //TODO - It should be improved (Workaround). 
    public Mitt() {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        try {
            Model model = reader.read(new FileReader("pom.xml"));
            List<Dependency> dependencies = model.getDependencies();

            for (Dependency dependency : dependencies) {
                if (dependency.getArtifactId().equalsIgnoreCase("mitt")) {
                    Logger.getLogger(Mitt.class.getName()).log(Level.INFO, "MITT v{0}", dependency.getVersion());
                    break;
                }
            }
        } catch (IOException
                | XmlPullParserException ex) {
        }
    }

    /**
     *
     * @param output
     */
    public void setOutputFile(String output) {
        this.getWriterSettings()
                .setOutputFile(new File(output));
    }

    /**
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     *
     * @return
     */
    private Output getOutput() {
        if (output == null) {
            output = new Output(
                    this.getConfiguration(),
                    this.getReaderSettings(),
                    this.getWriterSettings());
        }

        return output;
    }

    /**
     *
     * @return
     */
    private OutputProcessor getOutputProcessor() {
        if (outputProcessor == null) {
            outputProcessor = this.getOutput().getOutputProcessor();
        }

        return outputProcessor;
    }

    /**
     *
     * @param arguments
     * @return
     */
    public CommandLineInterface getCommandLineInterface(String[] arguments) {
        if (commandLineInterface == null) {
            commandLineInterface = new CommandLineInterface(
                    this.getConfiguration(),
                    arguments);
        }

        return commandLineInterface;
    }

    /**
     *
     * @return
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            configuration = new Configuration(debug);
        }

        return configuration;
    }

    /**
     *
     * @return
     */
    public ReaderSettings getReaderSettings() {
        if (readerSettings == null) {
            readerSettings = new ReaderSettings();
        }

        return readerSettings;
    }

    /**
     *
     * @return
     */
    public WriterSettings getWriterSettings() {
        if (writerSettings == null) {
            writerSettings = new WriterSettings();
        }

        return writerSettings;
    }

    /**
     *
     * @param record
     */
    public void write(List record) {
        this.getOutputProcessor().write(record);
    }

    /**
     *
     * @param record
     */
    public void write(String[] record) {
        this.getOutputProcessor().write(record);
    }

    /**
     *
     * @param file
     */
    public void write(File file) {
        this.write(file, "*");
    }

    /**
     *
     * @param file
     * @param wildcard
     */
    public void write(
            File file,
            String wildcard) {

        FileFilter fileFilter;
        File[] files = null;

        if (file.isDirectory()) {
            fileFilter = new WildcardFileFilter(wildcard);
            files = file.listFiles(fileFilter);
        }

        if (files != null) {
            this.getOutput().write(files);
        }
    }

    /**
     *
     */
    public void close() {
        if (this.getWriterSettings().getOutputFile() != null) {
            if (outputProcessor != null) {
                outputProcessor.close();
            }
        }
    }
}
