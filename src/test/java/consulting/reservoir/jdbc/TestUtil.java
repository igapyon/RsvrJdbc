/*
 * Copyright 2022 Reservoir Consulting - Toshiki Iga
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulting.reservoir.jdbc;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestUtil {
    public static Connection getConnection(String testCaseName) throws IOException {
        Connection conn;
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new IOException("No class found: " + ex.toString());
        }

        File dbDir = new File("./target/rsvrjdbctest");
        dbDir.mkdirs();
        final File dbFile = new File(dbDir, "rsvrjdbctest-" + testCaseName);
        final String jdbcConnStr = "jdbc:h2:file:" + dbFile.getCanonicalPath()
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        try {
            conn = DriverManager.getConnection(jdbcConnStr, "sa", "");
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new IOException("Create db failed: " + ex.toString());
        }
        return conn;
    }

}
