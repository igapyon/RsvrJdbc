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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * RsvrJdbc が提供する Statement です。
 * 
 * `RsvrStatement` はなるべく使用せずに、基本的に `RsvrPreparedStatement` を使用してください。
 * 
 * @see java.sql.PreparedStatement
 * @version 1.0.2.20220901
 */
public class RsvrStatement implements AutoCloseable {
    /**
     * 内包する Statement のインスタンス。
     */
    protected Statement stmt;

    /**
     * 引数なしコンストラクタを隠します。
     */
    protected RsvrStatement() {
    }

    /**
     * [Rsvr独自] 与えられた Statement をもとに RsvrJdbc が提供する RsvrStatement インスタンスを作成します。
     * 
     * @param stmt 通常の PreparedStatement。
     */
    public RsvrStatement(Statement stmt) {
        this.stmt = stmt;
    }

    /**
     * [Passing] AutoCloseable の close を実現します。
     */
    @Override
    public void close() throws SQLException {
        stmt.close();
    }

    /**
     * [Rsvr独自] 内部的に保持する もとの Statement インスタンスを取得します。
     * 
     * 通常はこのメソッドは使用されませんが、ワークアラウンド的な実装対応が必要な時にこのメソッドを使用することでしょう。
     * 
     * @return 内部的に保持する もとの PreparedStatement インスタンス。
     */
    public Statement getInternalStatement() {
        return stmt;
    }

    ///////////////////////////////////////////////
    // Statement 由来メソッド

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param max 最大数。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param max 最大数。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param enable 有効かどうか。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param seconds タイムアウト秒数。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void cancel() throws SQLException {
        stmt.cancel();
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param name カーソルの名前。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    /**
     * [Rsvr] java.sql.Statement に由来するメソッドですが、クエリを実行し、ResultSetの代わりに、RsvrJdbc による
     * RsvrResultSetを返却します。
     * 
     * @return ResultSetの代わりに、RsvrJdbc による RsvrResultSetを返却します。
     * @throws SQLException SQL例外が発生した
     */
    public RsvrResultSet getResultSet() throws SQLException {
        return RsvrJdbc.wrap(stmt.getResultSet());
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param direction 方向。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param rows 行数。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param current 現在位置。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean getMoreResults(int current) throws SQLException {
        return stmt.getMoreResults(current);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * プログラマーへのおせっかい注意喚起: このメソッドを使用する際は、prepareStatement(SQL,
     * Statement.RETURN_GENERATED_KEYS) された statement であることが必要です。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public RsvrResultSet getGeneratedKeys() throws SQLException {
        return RsvrJdbc.wrap(stmt.getGeneratedKeys());
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getResultSetHoldability() throws SQLException {
        return stmt.getResultSetHoldability();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isClosed() throws SQLException {
        return stmt.isClosed();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param poolable プール可能かどうか。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setPoolable(boolean poolable) throws SQLException {
        stmt.setPoolable(poolable);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isPoolable() throws SQLException {
        return stmt.isPoolable();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void closeOnCompletion() throws SQLException {
        stmt.closeOnCompletion();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実行結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isCloseOnCompletion() throws SQLException {
        return stmt.isCloseOnCompletion();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql SQL。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int[] executeBatch() throws SQLException {
        int[] result = stmt.executeBatch();
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql SQL。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean execute(String sql) throws SQLException {
        boolean result = stmt.execute(sql);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql               SQL。
     * @param autoGeneratedKeys autoGeneratedKeysの設定値。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        int result = stmt.executeUpdate(sql, autoGeneratedKeys);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql           SQL。
     * @param columnIndexes 列のインデックス。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
        int result = stmt.executeUpdate(sql, columnIndexes);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql         SQL。
     * @param columnNames 項目名。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int executeUpdate(String sql, String columnNames[]) throws SQLException {
        int result = stmt.executeUpdate(sql, columnNames);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql               SQL。
     * @param autoGeneratedKeys autoGeneratedKeysの設定値。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        boolean result = stmt.execute(sql, autoGeneratedKeys);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql           SQL。
     * @param columnIndexes 列のインデックス。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean execute(String sql, int columnIndexes[]) throws SQLException {
        boolean result = stmt.execute(sql, columnIndexes);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param sql         SQL。
     * @param columnNames 項目名。
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean execute(String sql, String columnNames[]) throws SQLException {
        boolean result = stmt.execute(sql, columnNames);
        // 子の resetParameterIndex(); は呼び出しません（呼び出せません。）
        return result;
    }

    // Statement 由来メソッド
    ///////////////////////////////////////////////
}
