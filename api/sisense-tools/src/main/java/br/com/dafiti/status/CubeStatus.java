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
package br.com.dafiti.status;

/**
 *
 * @author Valdiney V GOMES
 */
public enum CubeStatus {
    _1("stopped"),
    _2("running"),
    _4("faulted"),
    _8("being deleted"),
    _16("currently restarting"),
    _32("wrong version"),
    _64("the ElastiCube is down because it is 32 bit data on a 64 bit codebase"),
    _128("the ElastiCube is down because it is 64 bit data on a 32 bit codebase"),
    _256("locked"),
    _400("bad request, check if the provided cube parameter is exactly equals to sisense elasticube name (Case sensitive)"),
    _514("the ElastiCube or its child is currently in a build process"),
    _1024("the ElastiCube is starting, but not yet running"),
    _2048("the ElastiCube is in a build process"),
    _4096("trying to import a BigData ElastiCube on a non-BigData server"),
    _8192("rying to import a non-BigData ElastiCube on a BigData server"),
    _16384("Building is finished, now post indexing is running"),
    _32768("the ElastiCube is being stopped but its executable is still running"),
    _65536("this ElastiCube is in the process of cancelling an in-progress build");

    public final String status;

    private CubeStatus(String label) {
        this.status = label;
    }
}
