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
import java.io.File;
import java.util.Arrays;

/**
 *
 * @author Valdiney V GOMES
 */
public class MittBatchTest {

    public static void main(String[] args) throws DuplicateEntityException {
        Mitt mitt = new Mitt();
        mitt.getReaderSettings().setDelimiter(',');

        String xxx = "date::regexp(::filename(),**[0-9]{4}-[0-9]{2}-[0-9]{2}**)+file::filename()+{app_name}+{adgroup_name}+{adid}+[amount_sessions]+[amount_transactions]+{android_id}+{app_id}+{app_version}+[brand]+br.com.dafiti+{campaign_name}+[cart_currency_code]+[category]+[category_id]+{city}+{click_time}+[colour]+{country}+{creative_name}+{currency}+[currency_code]+[customer_token]+{device_name}+[device_manufacturer]+[device_model]+{device_type}+[discount]+[display_size]+[duration]+{environment}+{event_name}+{fb_adgroup_id}+{fb_adgroup_name}+{fb_adset_id}+{fb_adset_name}+{fb_campaign_id}+{fb_campaign_name}+{gclid}+{gender}+{gps_adid}+{idfa}+{idfv}+{installed_at}+{ip_address}+[keywords]+{language}+{last_time_spent}+{mac_md5}+{network_name}+[new_customer]+[oi9ldf]+{os_name}+{os_version}+[product]+[quantity]+{referrer}+{reftag}+{region}+{revenue}+{session_count}+[shop_country]+[size]+[sku]+{store}+{created_at}+{time_spent}+{timezone}+[total_cart]+[total_transaction]+[total_wishlist]+{tracking_enabled}+[transaction_currency]+[transaction_id]+[price]+[tree]+{user_agent}+[user_id]+[value]+[wishlist_currency_code]+[a4s_andr_device_id]+{activity_kind}+{install_begin_time}+{install_finish_time}+{rejection_reason}+{uninstalled_at}+{reinstalled_at}";

        mitt.setOutputFile("/tmp/mitt/");
        mitt.getConfiguration().addField(Arrays.asList(xxx.split("\\+")));

        mitt.write(new File("/home/valdiney/Downloads/"), "*.csv");
    }
}
