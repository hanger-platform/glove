/*
 * Copyright (c) 2020 Dafiti Group
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
package br.com.dafiti.mitt.transformation.embedded;

import br.com.dafiti.mitt.transformation.Parser;
import br.com.dafiti.mitt.transformation.Transformable;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class Checksum implements Transformable {

    private HashFunction hash;
    private final String algorithm;

    public Checksum() {
        this.algorithm = "MD5";
    }

    public Checksum(String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public void init() {
        this.hash = Hashing.farmHashFingerprint64();
    }

    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {

        String value;

        if (algorithm.equalsIgnoreCase("SHA1")) {
            value = DigestUtils.sha1Hex(String.valueOf(record)).toUpperCase();
        } else if (algorithm.equalsIgnoreCase("FARM_FINGERPRINT")) {
            value = String.valueOf(hash.hashBytes(String.valueOf(record).getBytes()).asLong());
        } else {
            value = DigestUtils.md5Hex(String.valueOf(record)).toUpperCase();
        }

        return value;
    }
}
