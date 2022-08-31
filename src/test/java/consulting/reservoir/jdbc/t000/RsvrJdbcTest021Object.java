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
package consulting.reservoir.jdbc.t000;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * Object の確認。
 * 
 * setObject, getObject については追加の知見が必要。
 */
class RsvrJdbcTest021Object {
    private static final String CASEID = "test021";

    @Test
    void test021() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 値その1 (insert)
        final String value1 = "こんにちは世界";
        final Long recordId = insertInto(conn, value1);
        final byte[] wrk1 = (byte[]) selectValue(conn, recordId);
        assertEquals(value1, new String(wrk1, "UTF8"));

        // 値その2 (null値)
        update(conn, recordId, null);
        assertEquals(null, selectValue(conn, recordId));
    }

    void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + CASEID + " (" //
                        + " RecordId BIGINT AUTO_INCREMENT NOT NULL" //
                        + ",Value1 VARBINARY" //
                        + ",PRIMARY KEY(RecordId))"))) {
            stmt.executeUpdate();
        }
    }

    Long insertInto(Connection conn, Object value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)", //
                        Statement.RETURN_GENERATED_KEYS))) {
            stmt.setObject(value);
            stmt.executeUpdateSingleRow();
            RsvrResultSet genKeys = stmt.getGeneratedKeys();
            assertTrue(genKeys.next());
            return genKeys.getLong();
        }
    }

    Object selectValue(Connection conn, Long recordId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " WHERE RecordId=?"))) {
            stmt.setLong(recordId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                return rset.getObject();
            }
        }
    }

    void update(Connection conn, Long recordId, Object value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("UPDATE " + CASEID + " SET Value1=? WHERE recordId=?"))) {
            stmt.setObject(value);
            stmt.setLong(recordId);
            stmt.executeUpdateSingleRow();
        }
    }
}