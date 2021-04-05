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
package br.com.dafiti.jdbc;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
//import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class JDBC {

    public static void main(String[] args) {
        try {
            URL u = new URL("file:///home/valdiney.gomes/Classes/mysql-connector-java-8.0.23.jar");
            URLClassLoader ucl = new URLClassLoader(new URL[]{u});

            Driver d = (Driver) Class.forName("com.mysql.cj.jdbc.Driver", true, ucl).newInstance();

            DriverManager.registerDriver(new DriverShim(d));

            String xxx = "jdbc:mysql://127.0.0.1:3306/hanger"; //Nome da base de dados
            String user = "root"; //nome do usuário do MySQL
            String password = "root"; //senha do MySQL

            Connection conn = DriverManager.getConnection(xxx, user, password);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select * from job");

            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                System.out.println(metaData.getColumnName(i));
                System.out.println(metaData.getColumnTypeName(i));
                System.out.println(metaData.getColumnClassName(i));
                System.out.println(metaData.getColumnDisplaySize(i));
                System.out.println(metaData.getPrecision(i));
                System.out.println(metaData.getScale(i));
            }

            System.out.println("select user() output : ");
            while (rs.next()) {
                System.out.println(rs.getObject(1));
            }
        } catch (ClassNotFoundException
                | IllegalAccessException
                | InstantiationException
                | MalformedURLException
                | SQLException ex) {
        }
    }

    static class DriverShim implements Driver {

        private Driver driver;

        DriverShim(Driver d) {
            this.driver = d;
        }

        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return this.driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return this.driver.getParentLogger();
        }
    }
}
