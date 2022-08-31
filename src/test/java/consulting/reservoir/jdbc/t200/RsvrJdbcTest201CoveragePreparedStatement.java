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
package consulting.reservoir.jdbc.t200;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * カバレッジ
 * 
 * このパッケージでは、カバレッジ向上を主目的
 */
class RsvrJdbcTest201CoveragePreparedStatement {
    private static final String CASEID = "test201";

    @Test
    void test101() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 全消し
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("DELETE FROM " + CASEID))) {
            stmt.executeUpdate();
            // 内包ステートメントの取得。
            stmt.getInternalPreparedStatement();
        }

        // 5行追加
        insertInto(conn, "1行目", 12);
        insertInto(conn, "2行目", 34);
        insertInto(conn, "3行目", 56);
        insertInto(conn, "4行目", 78);
        insertInto(conn, "5行目", 90);

        // 検索
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " ORDER BY RecordId",
                        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE))) {
            stmt.setCursorName("MyCursor1");
            stmt.getConnection();
            assertFalse(stmt.isClosed());
            stmt.setPoolable(stmt.isPoolable());
            stmt.setFetchDirection(stmt.getFetchDirection());
            stmt.setFetchSize(stmt.getFetchSize());
            stmt.isCloseOnCompletion();

            stmt.getResultSetConcurrency();
            stmt.getResultSetType();
            stmt.getResultSetHoldability();

            stmt.setParameterIndex(3);
            assertEquals(3, stmt.getParameterIndex());
            stmt.setMaxFieldSize(5555);
            stmt.getMaxFieldSize(); // 単にカバレッジ。
            stmt.setMaxRows(5555);
            assertEquals(5555, stmt.getMaxRows());
            stmt.setEscapeProcessing(false);
            stmt.setQueryTimeout(60);
            assertEquals(60, stmt.getQueryTimeout());

            stmt.executeQuery();
            try (RsvrResultSet rset = stmt.getResultSet()) {
                for (; rset.next();) {

                }
                stmt.cancel();
            }
            try {
                stmt.getWarnings();
            } catch (Exception ex) {
                // カバレッジだけを目的
            }
            stmt.clearWarnings();

            // 最後にこれを呼ばないと一部のテストが落ちる。
            stmt.closeOnCompletion();
        }
    }

    void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + CASEID + " (" //
                        + " RecordId BIGINT AUTO_INCREMENT NOT NULL" //
                        + ",Value1 VARCHAR(8192)" //
                        + ",Value2 INT" //
                        + ",PRIMARY KEY(RecordId))"))) {
            stmt.executeUpdate();
        }
    }

    void insertInto(Connection conn, String value1, Integer value2) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1, Value2) VALUES (?,?)"))) {
            stmt.setString(value1);
            stmt.setInt(value2);
            stmt.executeUpdateSingleRow();

            assertEquals(1, stmt.getUpdateCount());
            assertFalse(stmt.getMoreResults());
        }
    }
}
