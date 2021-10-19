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
package br.com.dafiti.survey.monkey;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.github.opendevl.JFlat;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class SurveyMonkey {

    private static final Logger LOG = Logger.getLogger(SurveyMonkey.class.getName());
    private static final String SURVEYMONKEY_ENDPOINT = "https://api.surveymonkey.com/v3/";

    /**
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - SurveyMonkey API extractor started");

        int page = 0;
        boolean paginate = false;
        boolean process = true;
        JSONObject parameters = null;

        //Path where temporary files will stored.
        Path outputPath = null;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in a JsonPath format", "", true, false)
                    .addParameter("e", "endpoint", "Endpoint uri", "", true, false)
                    .addParameter("g", "paginate", "(Optional) Identifies if the endpoint has pagination", false)
                    .addParameter("p", "parameters", "(Optional) Endpoint parameters", "", true, true)
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

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
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrieves API credentials.
            String token = credentials.get("authorization").toString();

            //Identifies endpoint parameters. 
            String endpointParameter = "{\"per_page\":\"100\",\"start_modified_at\":\"2021-10-11\"}";// cli.getParameter("parameters");

            if (endpointParameter != null && !endpointParameter.isEmpty()) {
                try {
                    parameters = (JSONObject) parser.parse(endpointParameter);
                } catch (ParseException ex) {
                    LOG.log(Level.INFO, "Fail parsing endpoint parameters: {0}", endpointParameter);
                }
            }

            //Defines the output path.
            outputPath = Files.createTempDirectory("survey_monkey_" + UUID.randomUUID());

            paginate = true;

            do {
                //Increments page.
                page++;

                String json = call(cli.getParameter("endpoint"), token, parameters, paginate, page);

                // Deletar....
                // Survey Details
                //String json = "{\"title\":\"Getting to know our customer\",\"nickname\":\"\",\"language\":\"pt-br\",\"folder_id\":\"0\",\"category\":\"product_feedback\",\"question_count\":6,\"page_count\":7,\"response_count\":64886,\"date_created\":\"2020-09-09T15:58:00\",\"date_modified\":\"2021-10-19T15:10:00\",\"id\":\"292292993\",\"buttons_text\":{\"next_button\":\"Pr\\u00f3x.\",\"prev_button\":\"Anter.\",\"done_button\":\"Conclu\\u00eddo\",\"exit_button\":\"\"},\"is_owner\":true,\"footer\":false,\"custom_variables\":{\"E\":\"email\",\"D\":\"customer_id\"},\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\",\"analyze_url\":\"https:\\/\\/www.surveymonkey.com\\/analyze\\/Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D\",\"edit_url\":\"https:\\/\\/www.surveymonkey.com\\/create\\/?sm=Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D\",\"collect_url\":\"https:\\/\\/www.surveymonkey.com\\/collect\\/list?sm=Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D\",\"summary_url\":\"https:\\/\\/www.surveymonkey.com\\/summary\\/Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D\",\"preview\":\"https:\\/\\/www.surveymonkey.com\\/r\\/Preview\\/?sm=fJbXs2yNK4VhPG8e3pOARJ8Ghi9XcSzcZ1_2BDAnrQ2sreOe9dUHjsh8cP2r_2BdT0LO\",\"pages\":[{\"title\":\"Estamos muito felizes por ter você com a gente!\",\"description\":\"<div><strong>Estamos muito felizes por ter voc\\u00ea com a gente!<\\/strong><\\/div>\\n<div><br><\\/div>\\n<div>Para que possamos te proporcionar a melhor experi\\u00eancia, poderia nos contar um pouco sobre voc\\u00ea?<\\/div>\",\"position\":1,\"question_count\":0,\"id\":\"138922182\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/138922182\",\"questions\":[]},{\"title\":\"\",\"description\":\"\",\"position\":2,\"question_count\":1,\"id\":\"137936761\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137936761\",\"questions\":[{\"id\":\"531911578\",\"position\":1,\"visible\":true,\"family\":\"single_choice\",\"subtype\":\"vertical\",\"layout\":{\"bottom_spacing\":0,\"col_width\":null,\"col_width_format\":null,\"left_spacing\":0,\"num_chars\":50,\"num_lines\":1,\"position\":\"new_row\",\"right_spacing\":0,\"top_spacing\":0,\"width\":null,\"width_format\":null},\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"all\",\"amount\":\"0\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Como você conheceu a Dafiti?\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137936761\\/questions\\/531911578\",\"answers\":{\"other\":{\"position\":0,\"visible\":true,\"text\":\"Outro (especifique)\",\"id\":\"3514436895\",\"num_lines\":1,\"num_chars\":50,\"is_answer_choice\":true,\"apply_all_rows\":false,\"error_text\":\"Digite um coment\\u00e1rio.\"},\"choices\":[{\"position\":1,\"visible\":true,\"text\":\"Indicação de amigos ou família\",\"quiz_options\":{\"score\":0},\"id\":\"3514436890\"},{\"position\":2,\"visible\":true,\"text\":\"Anúncios\",\"quiz_options\":{\"score\":0},\"id\":\"3800507763\"},{\"position\":3,\"visible\":true,\"text\":\"Busca no Google\",\"quiz_options\":{\"score\":0},\"id\":\"3514436892\"},{\"position\":4,\"visible\":true,\"text\":\"Influenciadores\\/celebridades\",\"quiz_options\":{\"score\":0},\"id\":\"3514436893\"},{\"position\":5,\"visible\":true,\"text\":\"Imprensa\",\"quiz_options\":{\"score\":0},\"id\":\"3514436894\"}]}}]},{\"title\":\"\",\"description\":\"\",\"position\":3,\"question_count\":1,\"id\":\"148846077\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/148846077\",\"questions\":[{\"id\":\"581228436\",\"position\":1,\"visible\":true,\"family\":\"single_choice\",\"subtype\":\"vertical\",\"layout\":{\"bottom_spacing\":0,\"col_width\":null,\"col_width_format\":null,\"left_spacing\":0,\"num_chars\":50,\"num_lines\":1,\"position\":\"new_row\",\"right_spacing\":0,\"top_spacing\":0,\"width\":null,\"width_format\":null},\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"all\",\"amount\":\"0\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Onde você viu esse anúncio?\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/148846077\\/questions\\/581228436\",\"answers\":{\"other\":{\"position\":0,\"visible\":true,\"text\":\"Outro (especifique)\",\"id\":\"3829387048\",\"num_lines\":1,\"num_chars\":50,\"is_answer_choice\":true,\"apply_all_rows\":false,\"error_text\":\"Digite um coment\\u00e1rio.\"},\"choices\":[{\"position\":1,\"visible\":true,\"text\":\"Instagram\",\"quiz_options\":{\"score\":0},\"id\":\"3829387044\"},{\"position\":2,\"visible\":true,\"text\":\"Facebook\",\"quiz_options\":{\"score\":0},\"id\":\"3829387045\"},{\"position\":3,\"visible\":true,\"text\":\"Google\",\"quiz_options\":{\"score\":0},\"id\":\"3829387046\"},{\"position\":4,\"visible\":true,\"text\":\"Site de conteúdo \\/ blog\",\"quiz_options\":{\"score\":0},\"id\":\"3829387047\"}]}}]},{\"title\":\"\",\"description\":\"\",\"position\":4,\"question_count\":1,\"id\":\"148847043\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/148847043\",\"questions\":[{\"id\":\"581229964\",\"position\":1,\"visible\":true,\"family\":\"single_choice\",\"subtype\":\"vertical\",\"layout\":{\"bottom_spacing\":0,\"col_width\":null,\"col_width_format\":null,\"left_spacing\":0,\"num_chars\":50,\"num_lines\":1,\"position\":\"new_row\",\"right_spacing\":0,\"top_spacing\":0,\"width\":null,\"width_format\":null},\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"all\",\"amount\":\"0\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Por onde você viu essa influenciadora\\/celebridade falando da Dafiti?\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/148847043\\/questions\\/581229964\",\"answers\":{\"other\":{\"position\":0,\"visible\":true,\"text\":\"Outro (especifique)\",\"id\":\"3829555480\",\"num_lines\":1,\"num_chars\":50,\"is_answer_choice\":true,\"apply_all_rows\":false,\"error_text\":\"Digite um coment\\u00e1rio.\"},\"choices\":[{\"position\":1,\"visible\":true,\"text\":\"Instagram\",\"quiz_options\":{\"score\":0},\"id\":\"3829555474\"},{\"position\":2,\"visible\":true,\"text\":\"Facebook\",\"quiz_options\":{\"score\":0},\"id\":\"3829555475\"},{\"position\":3,\"visible\":true,\"text\":\"Google\",\"quiz_options\":{\"score\":0},\"id\":\"3829555476\"},{\"position\":4,\"visible\":true,\"text\":\"Site de conte\\u00fado \\/ blog\",\"quiz_options\":{\"score\":0},\"id\":\"3829555477\"},{\"position\":5,\"visible\":true,\"text\":\"TV\",\"quiz_options\":{\"score\":0},\"id\":\"3829555478\"}]}}]},{\"title\":\"\",\"description\":\"\",\"position\":5,\"question_count\":1,\"id\":\"137945429\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945429\",\"questions\":[{\"id\":\"531912486\",\"position\":1,\"visible\":true,\"family\":\"single_choice\",\"subtype\":\"vertical\",\"layout\":null,\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"all\",\"amount\":\"0\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Você já comprou roupas, calçados ou acessórios pela internet antes? Se você já chegou a comprar na Dafiti, responda pensando em antes dessa compra.\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945429\\/questions\\/531912486\",\"answers\":{\"choices\":[{\"position\":1,\"visible\":true,\"text\":\"Sim\",\"quiz_options\":{\"score\":0},\"id\":\"3514442222\"},{\"position\":2,\"visible\":true,\"text\":\"Não\",\"quiz_options\":{\"score\":0},\"id\":\"3514442223\"}]}}]},{\"title\":\"\",\"description\":\"\",\"position\":6,\"question_count\":1,\"id\":\"137945475\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945475\",\"questions\":[{\"id\":\"531913055\",\"position\":1,\"visible\":true,\"family\":\"open_ended\",\"subtype\":\"single\",\"layout\":null,\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"all\",\"amount\":\"0\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Em qual loja online você comprou moda? Se for mais de uma, por favor, separe-as por vírgulas.\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945475\\/questions\\/531913055\"}]},{\"title\":\"\",\"description\":\"\",\"position\":7,\"question_count\":1,\"id\":\"137945568\",\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945568\",\"questions\":[{\"id\":\"538962691\",\"position\":1,\"visible\":true,\"family\":\"multiple_choice\",\"subtype\":\"vertical\",\"layout\":null,\"sorting\":null,\"required\":{\"text\":\"Esta pergunta exige uma resposta.\",\"type\":\"at_least\",\"amount\":\"1\"},\"validation\":null,\"forced_ranking\":false,\"headings\":[{\"heading\":\"Qual\\/quais categorias abaixo você já comprou online?\"}],\"href\":\"https:\\/\\/api.surveymonkey.com\\/v3\\/surveys\\/292292993\\/pages\\/137945568\\/questions\\/538962691\",\"answers\":{\"choices\":[{\"position\":1,\"visible\":true,\"text\":\"Roupas\",\"quiz_options\":{\"score\":0},\"id\":\"3559031214\"},{\"position\":2,\"visible\":true,\"text\":\"Calçados\",\"quiz_options\":{\"score\":0},\"id\":\"3559031215\"},{\"position\":3,\"visible\":true,\"text\":\"Acessórios\",\"quiz_options\":{\"score\":0},\"id\":\"3559031216\"},{\"position\":4,\"visible\":true,\"text\":\"Não comprei nenhuma dessas categorias\",\"quiz_options\":{\"score\":0},\"id\":\"3559033402\"}]}}]}]}";
                // Surveys
                //Sttring json = "{"title":"Getting to know our customer","nickname":"","language":"pt-br","folder_id":"0","category":"product_feedback","question_count":6,"page_count":7,"response_count":64958,"date_created":"2020-09-09T15:58:00","date_modified":"2021-10-19T19:31:00","id":"292292993","buttons_text":{"next_button":"Pr\u00f3x.","prev_button":"Anter.","done_button":"Conclu\u00eddo","exit_button":""},"is_owner":true,"footer":false,"custom_variables":{"E":"email","D":"customer_id"},"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993","analyze_url":"https:\/\/www.surveymonkey.com\/analyze\/Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D","edit_url":"https:\/\/www.surveymonkey.com\/create\/?sm=Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D","collect_url":"https:\/\/www.surveymonkey.com\/collect\/list?sm=Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D","summary_url":"https:\/\/www.surveymonkey.com\/summary\/Vn77_2BbScGhqR1bvajcR04ardJwwxN9ocGQ_2BZnUwBee8_3D","preview":"https:\/\/www.surveymonkey.com\/r\/Preview\/?sm=fJbXs2yNK4VhPG8e3pOARJ8Ghi9XcSzcZ1_2BDAnrQ2sreOe9dUHjsh8cP2r_2BdT0LO","pages":[{"title":"Estamos muito felizes por ter você com a gente!","description":"<div><strong>Estamos muito felizes por ter voc\u00ea com a gente!<\/strong><\/div>\n<div><br><\/div>\n<div>Para que possamos te proporcionar a melhor experi\u00eancia, poderia nos contar um pouco sobre voc\u00ea?<\/div>","position":1,"question_count":0,"id":"138922182","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/138922182","questions":[]},{"title":"","description":"","position":2,"question_count":1,"id":"137936761","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137936761","questions":[{"id":"531911578","position":1,"visible":true,"family":"single_choice","subtype":"vertical","layout":{"bottom_spacing":0,"col_width":null,"col_width_format":null,"left_spacing":0,"num_chars":50,"num_lines":1,"position":"new_row","right_spacing":0,"top_spacing":0,"width":null,"width_format":null},"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"all","amount":"0"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Como você conheceu a Dafiti?"}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137936761\/questions\/531911578","answers":{"other":{"position":0,"visible":true,"text":"Outro (especifique)","id":"3514436895","num_lines":1,"num_chars":50,"is_answer_choice":true,"apply_all_rows":false,"error_text":"Digite um coment\u00e1rio."},"choices":[{"position":1,"visible":true,"text":"Indicação de amigos ou família","quiz_options":{"score":0},"id":"3514436890"},{"position":2,"visible":true,"text":"Anúncios","quiz_options":{"score":0},"id":"3800507763"},{"position":3,"visible":true,"text":"Busca no Google","quiz_options":{"score":0},"id":"3514436892"},{"position":4,"visible":true,"text":"Influenciadores\/celebridades","quiz_options":{"score":0},"id":"3514436893"},{"position":5,"visible":true,"text":"Imprensa","quiz_options":{"score":0},"id":"3514436894"}]}}]},{"title":"","description":"","position":3,"question_count":1,"id":"148846077","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/148846077","questions":[{"id":"581228436","position":1,"visible":true,"family":"single_choice","subtype":"vertical","layout":{"bottom_spacing":0,"col_width":null,"col_width_format":null,"left_spacing":0,"num_chars":50,"num_lines":1,"position":"new_row","right_spacing":0,"top_spacing":0,"width":null,"width_format":null},"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"all","amount":"0"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Onde você viu esse anúncio?"}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/148846077\/questions\/581228436","answers":{"other":{"position":0,"visible":true,"text":"Outro (especifique)","id":"3829387048","num_lines":1,"num_chars":50,"is_answer_choice":true,"apply_all_rows":false,"error_text":"Digite um coment\u00e1rio."},"choices":[{"position":1,"visible":true,"text":"Instagram","quiz_options":{"score":0},"id":"3829387044"},{"position":2,"visible":true,"text":"Facebook","quiz_options":{"score":0},"id":"3829387045"},{"position":3,"visible":true,"text":"Google","quiz_options":{"score":0},"id":"3829387046"},{"position":4,"visible":true,"text":"Site de conteúdo \/ blog","quiz_options":{"score":0},"id":"3829387047"}]}}]},{"title":"","description":"","position":4,"question_count":1,"id":"148847043","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/148847043","questions":[{"id":"581229964","position":1,"visible":true,"family":"single_choice","subtype":"vertical","layout":{"bottom_spacing":0,"col_width":null,"col_width_format":null,"left_spacing":0,"num_chars":50,"num_lines":1,"position":"new_row","right_spacing":0,"top_spacing":0,"width":null,"width_format":null},"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"all","amount":"0"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Por onde você viu essa influenciadora\/celebridade falando da Dafiti?"}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/148847043\/questions\/581229964","answers":{"other":{"position":0,"visible":true,"text":"Outro (especifique)","id":"3829555480","num_lines":1,"num_chars":50,"is_answer_choice":true,"apply_all_rows":false,"error_text":"Digite um coment\u00e1rio."},"choices":[{"position":1,"visible":true,"text":"Instagram","quiz_options":{"score":0},"id":"3829555474"},{"position":2,"visible":true,"text":"Facebook","quiz_options":{"score":0},"id":"3829555475"},{"position":3,"visible":true,"text":"Google","quiz_options":{"score":0},"id":"3829555476"},{"position":4,"visible":true,"text":"Site de conte\u00fado \/ blog","quiz_options":{"score":0},"id":"3829555477"},{"position":5,"visible":true,"text":"TV","quiz_options":{"score":0},"id":"3829555478"}]}}]},{"title":"","description":"","position":5,"question_count":1,"id":"137945429","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945429","questions":[{"id":"531912486","position":1,"visible":true,"family":"single_choice","subtype":"vertical","layout":null,"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"all","amount":"0"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Você já comprou roupas, calçados ou acessórios pela internet antes? Se você já chegou a comprar na Dafiti, responda pensando em antes dessa compra."}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945429\/questions\/531912486","answers":{"choices":[{"position":1,"visible":true,"text":"Sim","quiz_options":{"score":0},"id":"3514442222"},{"position":2,"visible":true,"text":"Não","quiz_options":{"score":0},"id":"3514442223"}]}}]},{"title":"","description":"","position":6,"question_count":1,"id":"137945475","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945475","questions":[{"id":"531913055","position":1,"visible":true,"family":"open_ended","subtype":"single","layout":null,"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"all","amount":"0"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Em qual loja online você comprou moda? Se for mais de uma, por favor, separe-as por vírgulas."}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945475\/questions\/531913055"}]},{"title":"","description":"","position":7,"question_count":1,"id":"137945568","href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945568","questions":[{"id":"538962691","position":1,"visible":true,"family":"multiple_choice","subtype":"vertical","layout":null,"sorting":null,"required":{"text":"Esta pergunta exige uma resposta.","type":"at_least","amount":"1"},"validation":null,"forced_ranking":false,"headings":[{"heading":"Qual\/quais categorias abaixo você já comprou online?"}],"href":"https:\/\/api.surveymonkey.com\/v3\/surveys\/292292993\/pages\/137945568\/questions\/538962691","answers":{"choices":[{"position":1,"visible":true,"text":"Roupas","quiz_options":{"score":0},"id":"3559031214"},{"position":2,"visible":true,"text":"Calçados","quiz_options":{"score":0},"id":"3559031215"},{"position":3,"visible":true,"text":"Acessórios","quiz_options":{"score":0},"id":"3559031216"},{"position":4,"visible":true,"text":"Não comprei nenhuma dessas categorias","quiz_options":{"score":0},"id":"3559033402"}]}}]}]}";
                // Survey responses = "";
                // Deletar acima...
                if (json != null && !json.isEmpty()) {

                    //Display page statistics.
                    if (paginate) {
                        int perPage = JsonPath.read(json, "$.per_page");
                        int currentPage = JsonPath.read(json, "$.page");
                        int total = JsonPath.read(json, "$.total");

                        LOG.log(Level.INFO, "Total: {0} | Current page: {1} ({2} per page)", new Object[]{total, currentPage, perPage});

                        //Identifies if it is last page.
                        try {
                            JsonPath.read(json, "$.links.next");
                        } catch (PathNotFoundException ex) {
                            process = false;
                        }
                    }

                    JFlat flatMe = new JFlat(json);

                    //get the 2D representation of JSON document
                    List<Object[]> json2csv = flatMe.json2Sheet().getJsonAsSheet();

                    //write the 2D representation in csv format
                    flatMe.headerSeparator("_").write2csv(outputPath.toString() + "/page_" + page + "_" + UUID.randomUUID() + ".csv", ';');

                }
            } while (paginate && process);

            mitt.write(outputPath.toFile());
            FileUtils.deleteDirectory(outputPath.toFile());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SurveyMonkey API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SurveyMonkey API extractor finalized");
    }

    public static String call(String uri, String token, JSONObject parameters, boolean paginate, int page) throws Exception {
        String json = null;
        String entity = null;

        //Connect to the API. 
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(SURVEYMONKEY_ENDPOINT + uri);

            //Sets header.
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setHeader("Authorization", token);

            //Sets default URI parameters. 
            URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());

            //Identifies if endpoint has pagination.
            if (paginate) {
                uriBuilder.addParameter("page", String.valueOf(page));
            }

            //Sets endpoint URI parameters. 
            if (parameters != null && !parameters.isEmpty()) {
                for (Object key : parameters.keySet()) {
                    uriBuilder.addParameter((String) key, (String) parameters.get(key));
                }
            }

            //Sets URI parameters. 
            httpGet.setURI(uriBuilder.build());

            //Executes a request. 
            CloseableHttpResponse response = client.execute(httpGet);

            //Gets a reponse entity. 
            entity = EntityUtils.toString(response.getEntity(), "UTF-8");
        }

        return entity;
    }
}
