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

        String xxx = "database+size+rownumber::rownumber()+FileLastModified::FileLastModified()+FileSize::FileSize()";

        mitt.setOutputFile("/tmp/braze/output/xxx.csv");
        mitt.getConfiguration().addField(Arrays.asList(xxx.split("\\+")));
        
        mitt.getReaderSettings().setDelimiter(',');
        
        mitt.write(new File("/home/valdiney.gomes/Debug"), "*.*");
    }
}
