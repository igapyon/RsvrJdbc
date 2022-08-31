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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.rowset.serial.SerialClob;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * Clob の確認。
 */
class RsvrJdbcTest018Clob {
    private static final String CASEID = "test018";

    @Test
    void test018() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 値その1 (insert)
        char[] value1 = "ABCDEFG".toCharArray();
        final Long recordId = insertInto(conn, new SerialClob(value1));
        assertArrayEquals(value1, readAll(selectValue(conn, recordId).getCharacterStream()));

        // 値その2 (update)
        char[] value2 = "VWXYZ".toCharArray();
        update(conn, recordId, new SerialClob(value2));
        assertArrayEquals(value2, readAll(selectValue(conn, recordId).getCharacterStream()));

        // 値その3 (null値)
        update(conn, recordId, null);
        assertEquals(null, selectValue(conn, recordId));
    }

    void createTable(Connection conn) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + CASEID + " (" //
                        + " RecordId BIGINT AUTO_INCREMENT NOT NULL" //
                        + ",Value1 CLOB" //
                        + ",PRIMARY KEY(RecordId))"))) {
            stmt.executeUpdate();
        }
    }

    Long insertInto(Connection conn, Clob value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)", //
                        Statement.RETURN_GENERATED_KEYS))) {
            stmt.setClob(value);
            stmt.executeUpdateSingleRow();
            RsvrResultSet genKeys = stmt.getGeneratedKeys();
            assertTrue(genKeys.next());
            return genKeys.getLong();
        }
    }

    Clob selectValue(Connection conn, Long recordId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " WHERE RecordId=?"))) {
            stmt.setLong(recordId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                return rset.getClob();
            }
        }
    }

    void update(Connection conn, Long recordId, Clob value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("UPDATE " + CASEID + " SET Value1=? WHERE recordId=?"))) {
            stmt.setClob(value);
            stmt.setLong(recordId);
            stmt.executeUpdateSingleRow();
        }
    }

    private static char[] readAll(Reader reader) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (;;) {
            int value = reader.read();
            if (value < 0) {
                break;
            }
            buf.append((char) value);
        }
        return buf.toString().toCharArray();
    }
}