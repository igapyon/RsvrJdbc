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
package consulting.reservoir.jdbc.t400;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import consulting.reservoir.jdbc.RsvrJdbc;
import consulting.reservoir.jdbc.RsvrPreparedStatement;
import consulting.reservoir.jdbc.RsvrResultSet;
import consulting.reservoir.jdbc.RsvrResultSetMetaData;
import consulting.reservoir.jdbc.TestUtil;

/**
 * ResultSetMetaData関連のテスト
 */
class RsvrJdbcTest402ResultSetMetaData {
    private static final String CASEID = "test402";

    @Test
    void test402() throws Exception {
        Connection conn = TestUtil.getConnection(CASEID);
        createTable(conn);

        // 全消し
        try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement("DELETE FROM " + CASEID))) {
            stmt.executeUpdate();
        }

        // 行がなくとも動作する

        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("SELECT RecordId, Value1 FROM " + CASEID + " ORDER BY RecordId"))) {
            try (RsvrResultSet rset = stmt.executeQuery()) {
                RsvrResultSetMetaData rsetMeta = rset.getMetaData();
                do {
                    System.err.println(rsetMeta.getColumnName() + " (" + rsetMeta.getColumnIndex() + ")");
                    System.err.println("  db type: " + rsetMeta.getColumnTypeName());
                    System.err.println("  java   : " + rsetMeta.getColumnClassName());
                    System.err.println("  table  : " + rsetMeta.getTableName());
                } while (rsetMeta.nextColumn());
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

    void insertInto(Connection conn, String value) throws SQLException {
        try (RsvrPreparedStatement stmt = RsvrJdbc
                .wrap(conn.prepareStatement("INSERT INTO " + CASEID + " (Value1) VALUES (?)"))) {
            stmt.setString(value);
            stmt.executeUpdateSingleRow();
        }
    }
}
