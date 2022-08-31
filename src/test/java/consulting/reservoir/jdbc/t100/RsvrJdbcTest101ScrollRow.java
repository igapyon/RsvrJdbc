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
package consulting.reservoir.jdbc.t100;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * スクロールカーソル の確認。
 * 
 * このパッケージでは、組み合わせによるテストを実施。
 */
class RsvrJdbcTest101ScrollRow {
    private static final String CASEID = "test101";

    @Test
    void test101() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 全消し
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("DELETE FROM " + CASEID))) {
            stmt.executeUpdate();
        }

        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)"))) {
            // 5行追加
            stmt.setString("1行目");
            stmt.executeUpdateSingleRow();
            stmt.clearParameters();

            stmt.setString("2行目");
            stmt.executeUpdateSingleRow();
            stmt.clearParameters();

            stmt.setString("3行目");
            stmt.executeUpdateSingleRow();
            stmt.clearParameters();

            stmt.setString("4行目");
            stmt.executeUpdateSingleRow();
            stmt.clearParameters();

            stmt.setString("5行目");
            stmt.executeUpdateSingleRow();
            stmt.clearParameters();
        }

        // 検索
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " ORDER BY RecordId",
                        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                assertEquals("1行目", rset.getString());
                assertTrue(rset.next());
                assertEquals("2行目", rset.getString());
                assertTrue(rset.next());
                assertEquals("3行目", rset.getString());
                assertTrue(rset.next());
                assertEquals("4行目", rset.getString());
                assertFalse(rset.isLast());
                assertTrue(rset.next());
                assertEquals("5行目", rset.getString());
                assertTrue(rset.isLast());
                assertFalse(rset.next());
                assertFalse(rset.isLast());

                assertTrue(rset.last());
                assertEquals("5行目", rset.getString());
                assertTrue(rset.previous());
                assertEquals("4行目", rset.getString());
                assertTrue(rset.previous());
                assertEquals("3行目", rset.getString());
                assertTrue(rset.previous());
                assertEquals("2行目", rset.getString());
                assertTrue(rset.previous());
                assertEquals("1行目", rset.getString());
                assertTrue(rset.isFirst());
                assertFalse(rset.previous());

                assertTrue(rset.absolute(3));
                assertEquals("3行目", rset.getString());

                assertTrue(rset.first());
                assertEquals("1行目", rset.getString());
                assertTrue(rset.relative(2));
                assertEquals("3行目", rset.getString());
            }
        }
    }

    void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + CASEID + " (" //
                        + " RecordId BIGINT AUTO_INCREMENT NOT NULL" //
                        + ",Value1 VARCHAR(8192)" //
                        + ",PRIMARY KEY(RecordId))"))) {
            stmt.executeUpdate();
        }
    }
}
