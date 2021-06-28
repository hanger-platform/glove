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
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Valdiney V GOMES
 */
public class MittTest {

    public static void main(String[] args) throws DuplicateEntityException {
        Mitt mitt = new Mitt();
        mitt.setOutputFile("/tmp/mitt/mitt.csv");
        mitt.setDebug(true);

        //Fields.
        String field = "id+nome>>renamed+data+json";
        mitt.getConfiguration().addField(Arrays.asList(field.split("\\+")));

        mitt.getConfiguration().addCustomField("farmfingerprint::farmfingerprint([[nome]])");
        mitt.getConfiguration().addCustomField("fixed::concat([[id,nome,::eval(**nome.replace('A','xxx')**)]])");
        mitt.getConfiguration().addCustomField("eval::eval(**nome.replace(/[^0-9.]/g,'xxx')**)");
        mitt.getConfiguration().addCustomField("regexp::regexp(nome,[9])");
        mitt.getConfiguration().addCustomField("checksum::checksum()");
        mitt.getConfiguration().addCustomField("complex::concat([[id,nome,::now(),::Dateformat(data,YYYYMM),::eval(**nome.replace('A','xxx')**)]])");
        mitt.getConfiguration().addCustomField("rownumber::RowNumber()");
        mitt.getConfiguration().addCustomField("rownumberfield::RowNumber([[data]])");
        mitt.getConfiguration().addCustomField("at::at(0)");

        //Parameters. 
        mitt.getConfiguration().addParameter("a", "primeiro", "Primeiro parâmetro", "xxx");
        mitt.getConfiguration().addParameter("b", "segundo", "Segundo parâmetro");
        mitt.getConfiguration().addParameter("c", "help", "Help", false);

        List<String> parameter = new ArrayList();
        parameter.add("--primeiro=1");
        parameter.add("--segundo=a+b+c");

        CommandLineInterface cli = mitt.getCommandLineInterface(parameter.toArray(new String[0]));
        cli.getParameter("primeiro");
        cli.getParameterAsList("segundo", "\\+");

        for (int i = 0; i < 10; i++) {
            List<Object> data = new ArrayList();
            data.add(i);
            data.add("A" + i);
            data.add("2019-01-01 00:00:00");
            data.add("[{\"conversions\":0,\"revenue\":0.0,\"conversions1_by_send_time\":0,\"conversions_by_send_time\":0,\"conversions2_by_send_time\":0,\"conversions1\":0,\"messages\":{\"ios_push\":[{\"conversions\":0,\"variation_api_id\":\"ec6d3b1a-ad57-4f21-bdbc-76522e891133\",\"conversions1_by_send_time\":0,\"conversions_by_send_time\":0,\"variation_name\":\"Variant 1\",\"conversions2_by_send_time\":0,\"direct_opens\":0,\"sent\":0,\"revenue\":0.0,\"total_opens\":0,\"body_clicks\":0,\"conversions1\":0,\"conversions2\":0,\"conversions3\":0,\"bounces\":0,\"conversions3_by_send_time\":0,\"unique_recipients\":0},{\"conversions\":0,\"variation_api_id\":\"93aaf479-3f90-404c-a469-7a659a3a7991\",\"conversions1_by_send_time\":0,\"conversions_by_send_time\":0,\"variation_name\":\"Control Group\",\"conversions2_by_send_time\":0,\"revenue\":0.0,\"conversions1\":0,\"conversions2\":0,\"conversions3\":0,\"conversions3_by_send_time\":0,\"enrolled\":0,\"unique_recipients\":0}]},\"conversions2\":0,\"conversions3\":0,\"time\":\"2021-03-18\",\"conversions3_by_send_time\":0,\"unique_recipients\":0}]");

            mitt.write(data);
        }

        mitt.close();
    }
}
