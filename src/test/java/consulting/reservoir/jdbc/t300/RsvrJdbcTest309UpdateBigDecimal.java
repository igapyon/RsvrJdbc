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
package consulting.reservoir.jdbc.t300;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * 検索結果の行の更新 の確認。
 */
class RsvrJdbcTest309UpdateBigDecimal {
    private static final String CASEID = "test309";

    @Test
    void test309() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 全消し
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("DELETE FROM " + CASEID))) {
            stmt.executeUpdate();
        }

        insertInto(conn, new BigDecimal("1234567.89"));

        Long lastRecordId = null;
        // 検索して更新
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT RecordId, Value1 FROM " + CASEID + " ORDER BY RecordId",
                        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                lastRecordId = rset.getLong();
                assertEquals(new BigDecimal("1234567.89"), rset.getBigDecimal());

                rset.setColumnIndex(2);
                rset.updateBigDecimal(new BigDecimal("87654321.09"));
                rset.updateRow();
            }
        }

        // 検索して null で更新
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT RecordId, Value1 FROM " + CASEID + " WHERE RecordId=?",
                        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE))) {
            stmt.setLong(lastRecordId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                assertNotNull(rset.getLong());
                assertEquals(new BigDecimal("87654321.09"), rset.getBigDecimal());

                rset.setColumnIndex(2);
                rset.updateBigDecimal(null);
                rset.updateRow();
            }
        }

        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " WHERE RecordId=?"))) {
            stmt.setLong(lastRecordId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                assertEquals(null, rset.getBigDecimal());
            }
        }
    }

    void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + CASEID + " (" //
                        + " RecordId BIGINT AUTO_INCREMENT NOT NULL" //
                        + ",Value1 NUMERIC(20,2)" //
                        + ",PRIMARY KEY(RecordId))"))) {
            stmt.executeUpdate();
        }
    }

    void insertInto(Connection conn, BigDecimal value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)"))) {
            stmt.setBigDecimal(value);
            stmt.executeUpdateSingleRow();
        }
    }
}
