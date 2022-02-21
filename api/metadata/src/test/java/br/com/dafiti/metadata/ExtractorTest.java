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
package br.com.dafiti.metadata;

import br.com.dafiti.metadata.schema.Spectrum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import org.codehaus.plexus.util.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Helio Leal
 */
public class ExtractorTest {

    @Rule
    public final TemporaryFolder temporaryFolder = TemporaryFolder.builder().assureDeletion().build();
    public Extractor extractor;

    @Before
    public void setUp() throws Exception {
        File file = temporaryFolder.newFile("sample.csv");
        File reservedWords = temporaryFolder.newFile("reservedWords.txt");
        String output = temporaryFolder.newFolder("output").getAbsolutePath();

        MockFiles(file, reservedWords);

        this.extractor = new Extractor(
                file,
                reservedWords,
                ';',
                '\"',
                '\"',
                "",
                "",
                output + "/",
                "spectrum",
                10);
    }

    @Test
    public void testIfSampleIsFilled() throws IOException {
        this.extractor.fillDataSample();

        // Test if header is correctly filled   
        MatcherAssert.assertThat(
                this.extractor.getField().getList(),
                is(Arrays.asList("partition_field", "custom_primary_key", "id_sales_order", "customer_first_name", "allowoverwrite", "customer_email"))
        );

        // Test if content is correctly filled
        List<String[]> expectedContent = Arrays.asList(
                new String[]{"2022-02-13", "138563627", "138563627", "LEGACY", "LEGACY", "email_xxx@icloud.com"},
                new String[]{"2022-02-13", "136365829", "138563629", "LEGACY", "LEGACY", "email_yyy@gmail.com"}
        );

        MatcherAssert.assertThat(expectedContent.get(0), is(this.extractor.getFieldContent().get(0)));
        MatcherAssert.assertThat(expectedContent.get(1), is(this.extractor.getFieldContent().get(1)));
    }

    @Test
    public void testFilesExistenceForDialectSpectrum() throws IOException {
        this.extractor.fillDataSample();
        this.extractor.inferMetadata(new Spectrum());
        this.extractor.writeFiles();

        assertEquals(true, FileUtils.fileExists(temporaryFolder.getRoot() + "/output/sample_fields.csv"));
        assertEquals(true, FileUtils.fileExists(temporaryFolder.getRoot() + "/output/sample_columns.csv"));
        assertEquals(true, FileUtils.fileExists(temporaryFolder.getRoot() + "/output/sample.json"));
        assertEquals(true, FileUtils.fileExists(temporaryFolder.getRoot() + "/output/sample_metadata.csv"));
    }

    @Test
    public void testFilesContentForDialectSpectrum() throws IOException {
        this.extractor.fillDataSample();
        this.extractor.inferMetadata(new Spectrum());
        this.extractor.writeFiles();

        assertEquals(
                "partition_field varchar(20),custom_primary_key bigint,id_sales_order bigint,customer_first_name varchar(12),allowoverwrite_rw varchar(12),customer_email varchar(40)",
                FileUtils.fileRead(temporaryFolder.getRoot() + "/output/sample_fields.csv"));

        assertEquals(
                "partition_field\n"
                + "custom_primary_key\n"
                + "id_sales_order\n"
                + "customer_first_name\n"
                + "allowoverwrite\n"
                + "customer_email",
                FileUtils.fileRead(temporaryFolder.getRoot() + "/output/sample_columns.csv"));

        assertEquals(
                "[{\"name\":\"partition_field\",\"type\":[\"null\",\"string\"],\"default\":null},\n"
                + "{\"name\":\"custom_primary_key\",\"type\":[\"null\",\"long\"],\"default\":null},\n"
                + "{\"name\":\"id_sales_order\",\"type\":[\"null\",\"long\"],\"default\":null},\n"
                + "{\"name\":\"customer_first_name\",\"type\":[\"null\",\"string\"],\"default\":null},\n"
                + "{\"name\":\"allowoverwrite_rw\",\"type\":[\"null\",\"string\"],\"default\":null},\n"
                + "{\"name\":\"customer_email\",\"type\":[\"null\",\"string\"],\"default\":null}]",
                FileUtils.fileRead(temporaryFolder.getRoot() + "/output/sample.json"));

        assertEquals(
                "[{\"field\":\"partition_field\",\"type\":\"string\",\"length\":20},\n"
                + "{\"field\":\"custom_primary_key\",\"type\":\"integer\"},\n"
                + "{\"field\":\"id_sales_order\",\"type\":\"integer\"},\n"
                + "{\"field\":\"customer_first_name\",\"type\":\"string\",\"length\":12},\n"
                + "{\"field\":\"allowoverwrite_rw\",\"type\":\"string\",\"length\":12},\n"
                + "{\"field\":\"customer_email\",\"type\":\"string\",\"length\":40}]",
                FileUtils.fileRead(temporaryFolder.getRoot() + "/output/sample_metadata.csv"));
    }

    private void MockFiles(File file, File reservedWords) throws IOException {
        String csvInput
                = "partition_field;custom_primary_key;id_sales_order;customer_first_name;allowoverwrite;customer_email\n"
                + "2022-02-13;138563627;138563627;LEGACY;LEGACY;email_xxx@icloud.com\n"
                + "2022-02-13;136365829;138563629;LEGACY;LEGACY;email_yyy@gmail.com";

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(csvInput);
        bufferedWriter.close();

        String reservedInput
                = "aes128\n"
                + "aes256\n"
                + "all\n"
                + "allowoverwrite\n"
                + "analyse\n"
                + "analyze\n"
                + "and\n"
                + "any\n"
                + "array";

        FileWriter fileWriterReserved = new FileWriter(reservedWords);
        BufferedWriter bufferedWriterReserved = new BufferedWriter(fileWriterReserved);
        bufferedWriterReserved.write(reservedInput);
        bufferedWriterReserved.close();
    }
}
