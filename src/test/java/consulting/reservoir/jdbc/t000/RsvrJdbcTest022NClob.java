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

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.TestUtil;

/**
 * NClob の確認。
 */
class RsvrJdbcTest022NClob {
    private static final String CASEID = "test022";

    @Test
    void test022() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 値その1 (insert)
        String value1 = "ABCDEFG";
        NClob ncl1 = conn.createNClob();
        ncl1.setString(1, value1);
        final Long recordId = insertInto(conn, ncl1);
        assertEquals(value1, readAll(selectValue(conn, recordId).getCharacterStream()));

        // 値その2 (update)
        String value2 = "VWXYZ";
        NClob ncl2 = conn.createNClob();
        ncl2.setString(1, value2);
        update(conn, recordId, ncl2);
        assertEquals(value2, readAll(selectValue(conn, recordId).getCharacterStream()));

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

    Long insertInto(Connection conn, NClob value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)", //
                        Statement.RETURN_GENERATED_KEYS))) {
            stmt.setNClob(value);
            stmt.executeUpdateSingleRow();
            RsvrResultSet genKeys = stmt.getGeneratedKeys();
            assertTrue(genKeys.next());
            return genKeys.getLong();
        }
    }

    NClob selectValue(Connection conn, Long recordId) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT Value1 FROM " + CASEID + " WHERE RecordId=?"))) {
            stmt.setLong(recordId);
            try (RsvrResultSet rset = stmt.executeQuery()) {
                assertTrue(rset.next());
                return rset.getNClob();
            }
        }
    }

    void update(Connection conn, Long recordId, NClob value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("UPDATE " + CASEID + " SET Value1=? WHERE recordId=?"))) {
            stmt.setNClob(value);
            stmt.setLong(recordId);
            stmt.executeUpdateSingleRow();
        }
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (;;) {
            int value = reader.read();
            if (value < 0) {
                break;
            }
            buf.append((char) value);
        }
        return buf.toString();
    }
}