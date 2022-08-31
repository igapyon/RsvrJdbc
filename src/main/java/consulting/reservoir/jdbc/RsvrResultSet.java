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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.Calendar;

/**
 * RsvrJdbc が提供する ResultSet です。
 * 
 * <h1>About RsvrJdbc</h1>
 *
 * `RsvrJdbc` は、JDBCプログラミングにおける `ちいさないらいら` を解消することを目的に作成した軽量ライブラリです。
 * JDBCプログラミング体験そのままが活用できることを原則に `RsvrJdbc` における小さな変更点を知るだけで
 * JDBCプログラミング経験者はすぐに利用開始でき、そして `ちいさないらいら` を解消できることを目指して `RsvrJdbc` は作られています。
 * 
 * <h2>Usage: 検索</h2>
 * 
 * まずは基本的な使い方から見てみましょう。 まずは検索の方法から見ていきます。以下がシンプルな検索の例です。 このソースコード断片では
 * `conn.prepareStatement()` の結果を `RsvrJdbc#wrap`
 * メソッドで受けることにより、PreparedStatement に相当する `RsvrPreparedStatement` インスタンスを入手できます。
 * 
 * 得られた `RsvrPreparedStatement` インスタンスの `executeQuery()` メソッドを呼び出すと、ResultSet
 * に相当する `RsvrResultSet` インスタンスを入手できます。
 * 
 * <pre>
try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
        "SELECT IssueId, Summary, IssueType, Priority, Description FROM MyTable ORDER BY KeyId"))) {
    try (RsvrResultSet rset = stmt.executeQuery()) {
        for (; rset.next();) {
            Long origIssueId = rset.getLong();
            String summary = rset.getString();
            String issueType = rset.getString();
            String priority = rset.getString();
            String description = rset.getString();
 * </pre>
 * 
 * `RsvrResultSet` は JDBC API で使用可能な getString や getLong を利用できますが、通常の JDBC
 * プログラミングでは指定の必要がある columnIndex 引数の指定が不要になっています。
 * 
 * 実は、通常の JDBC プログラミングで この columnIndex 引数の指定は `ちいさないらいら` です。ほとんどの場合は 1 から順番に 2,
 * 3, 4
 * と順に増やしながら引数指定します。この定型的な指定は意外に面倒なうえに、SQL途中への項目追加などの仕様変更の際におけるバグの作り込みの原因にもなりがちです。
 * 
 * `RsvrJdbc` では、この columnIndex の指定を省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？
 * 
 * でも待ってください。columnIndex の指定が省略されるとして、この値はいつリセットされるのでしょうか。大丈夫です。次の説明で
 * RsvrResultSet 内部の columnIndex のリセットタイミングを知ることができます。
 * 
 * <h2>RsvrResultSet 内部の columnIndex のリセットタイミング</h2>
 * 
 * `RsvrJdbc` では、以下のメソッド呼び出しのタイミングで RsvrResultSet 内部の columnIndex のリセットを実施します。
 * 
 * <ul>
 * <li>RsvrResultSet#absolute</li>
 * <li>RsvrResultSet#afterLast</li>
 * <li>RsvrResultSet#beforeFirst</li>
 * <li>RsvrResultSet#close</li>
 * <li>RsvrResultSet#first</li>
 * <li>RsvrResultSet#last</li>
 * <li>RsvrResultSet#next</li>
 * <li>RsvrResultSet#previous</li>
 * <li>RsvrResultSet#relative</li>
 * </ul>
 * 
 * これは、たいていの一般的な JDBCプログラミングでは参照しているレコードを進めたり戻したりする際に columnIndex
 * を1へとリセットすることが観察されたことからも、上記のような仕様としています。
 * 
 * また、RsvrResultSet#resetColumnIndex メソッドを明示的に呼び出すことによっても columnIndex
 * のリセットを行うことが可能です。
 * 
 * @see java.sql.ResultSet
 * @version 1.0.2.20220901
 */
public class RsvrResultSet implements AutoCloseable {
    /**
     * 内包する ResultSet のインスタンス。
     */
    private ResultSet rset = null;

    /**
     * プログラマーの引数指定から columnIndex の指定を省略できるようにするために内部的に保持する変数。
     * 
     * RsvrResultSet が提供する機能の一つを実現するために使用される。
     */
    private int internalColumnIndex = 1;

    /**
     * コンストラクタ。
     * 
     * @param rset 内包する予定の ResultSet。
     */
    public RsvrResultSet(ResultSet rset) {
        this.rset = rset;
    }

    /**
     * [Passing][index=1] AutoCloseable の close を実現します。
     * 
     * （念の為）内包する columnIndex の値をリセットします。
     */
    @Override
    public void close() throws SQLException {
        rset.close();
        resetColumnIndex();
    }

    /**
     * [Rsvr独自] 内部的に保持する もとの ResultSet インスタンスを取得します。
     * 
     * 通常はこのメソッドは使用されませんが、ワークアラウンド的な実装対応が必要な時にこのメソッドを使用することでしょう。
     * 
     * @return 内部的に保持する もとの ResultSet インスタンス。
     */
    public ResultSet getInternalResultSet() {
        return rset;
    }

    /**
     * [Rsvr独自][index++] 内部的に保持する columnIndex の値をインクリメントします。
     */
    public void incrementColumnIndex() {
        internalColumnIndex++;
    }

    /**
     * [Rsvr独自][index=1] RsvrResultSet 内部の columnIndex をリセットします。
     * 
     * なお、このメソッドを呼ばなくても、以下のメソッド呼び出しのタイミングで RsvrResultSet 内部の columnIndex
     * のリセットを実施します。（以下のメソッドがこのメソッドを呼び出します）
     * 
     * <ul>
     * <li>RsvrResultSet#absolute</li>
     * <li>RsvrResultSet#afterLast</li>
     * <li>RsvrResultSet#beforeFirst</li>
     * <li>RsvrResultSet#close</li>
     * <li>RsvrResultSet#first</li>
     * <li>RsvrResultSet#last</li>
     * <li>RsvrResultSet#next</li>
     * <li>RsvrResultSet#previous</li>
     * <li>RsvrResultSet#relative</li>
     * </ul>
     * 
     * これは、たいていの一般的な JDBCプログラミングではカーソル移動時に columnIndex
     * を1へとリセットすることが観察されたことからも、上記のような仕様としています。
     */
    public void resetColumnIndex() {
        internalColumnIndex = 1;
    }

    /**
     * [Rsvr独自] 内部的に保持する columnIndex の値を設定します。
     * 
     * @param newColumnIndex 設定したい columnIndex値。
     */
    public void setColumnIndex(int newColumnIndex) {
        internalColumnIndex = newColumnIndex;
    }

    /**
     * [Rsvr独自] RsvrResultSet 内部の columnIndex を取得します。
     * 
     * 通常はこのメソッドは直接呼び出すシーンはないと想定します。
     * 
     * @return 次回の getXXXXX の第一引数として利用される columnIndex の値。
     */
    public int getColumnIndex() {
        return internalColumnIndex;
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean next() throws SQLException {
        boolean result = rset.next();
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 直前の呼び出しがnullだったかどうか。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean wasNull() throws SQLException {
        return rset.wasNull();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getString() throws SQLException {
        String result = rset.getString(getColumnIndex());
        incrementColumnIndex();
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Boolean getBoolean() throws SQLException {
        boolean result = rset.getBoolean(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Byte getByte() throws SQLException {
        byte result = rset.getByte(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;

    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Short getShort() throws SQLException {
        short result = rset.getShort(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Integer getInt() throws SQLException {
        int result = rset.getInt(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Long getLong() throws SQLException {
        long result = rset.getLong(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Float getFloat() throws SQLException {
        float result = rset.getFloat(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した結果セットから値を取得するメソッドです。取得する値はプリミティブ型ではなくラッパー型となっており、null
     * も取得することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * ResultSet#getXXXXX の戻り値はプリミティブ型ですが、このメソッドはプリミティブ型のラッパークラスのインスタンスを返却する点に注意。
     * 
     * @return 取得した値をプリミティブ型のラッパークラスのインタンスで戻す。データベース値が null だった場合は null を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Double getDouble() throws SQLException {
        double result = rset.getDouble(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した
     */
    public byte[] getBytes() throws SQLException {
        byte[] result = rset.getBytes(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Date getDate() throws SQLException {
        java.sql.Date result = rset.getDate(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Time getTime() throws SQLException {
        java.sql.Time result = rset.getTime(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Timestamp getTimestamp() throws SQLException {
        java.sql.Timestamp result = rset.getTimestamp(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.InputStream getAsciiStream() throws SQLException {
        java.io.InputStream result = rset.getAsciiStream(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.InputStream getBinaryStream() throws SQLException {
        java.io.InputStream result = rset.getBinaryStream(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getString(String columnLabel) throws SQLException {
        return rset.getString(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean getBoolean(String columnLabel) throws SQLException {
        return rset.getBoolean(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public byte getByte(String columnLabel) throws SQLException {
        return rset.getByte(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public short getShort(String columnLabel) throws SQLException {
        return rset.getShort(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getInt(String columnLabel) throws SQLException {
        return rset.getInt(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public long getLong(String columnLabel) throws SQLException {
        return rset.getLong(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public float getFloat(String columnLabel) throws SQLException {
        return rset.getFloat(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public double getDouble(String columnLabel) throws SQLException {
        return rset.getDouble(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public byte[] getBytes(String columnLabel) throws SQLException {
        return rset.getBytes(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Date getDate(String columnLabel) throws SQLException {
        return rset.getDate(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Time getTime(String columnLabel) throws SQLException {
        return rset.getTime(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Timestamp getTimestamp(String columnLabel) throws SQLException {
        return rset.getTimestamp(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.InputStream getAsciiStream(String columnLabel) throws SQLException {
        return rset.getAsciiStream(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.InputStream getBinaryStream(String columnLabel) throws SQLException {
        return rset.getBinaryStream(columnLabel);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 警告情報。
     * @throws SQLException SQL例外が発生した場合。
     */
    public SQLWarning getWarnings() throws SQLException {
        return rset.getWarnings();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void clearWarnings() throws SQLException {
        rset.clearWarnings();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return カーソル名。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getCursorName() throws SQLException {
        return rset.getCursorName();
    }

    /**
     * [Rsvr] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出しますが、RsvrResultSetMetaData でラップして返却します。
     * 
     * @return RsvrResultSetMetaData でラップした ResultSetMetaData.
     * @throws SQLException SQL例外が発生した場合。
     */
    public RsvrResultSetMetaData getMetaData() throws SQLException {
        return RsvrJdbc.wrap(rset.getMetaData());
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Object getObject() throws SQLException {
        Object result = rset.getObject(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Object getObject(String columnLabel) throws SQLException {
        return rset.getObject(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int findColumn(String columnLabel) throws SQLException {
        return rset.findColumn(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.Reader getCharacterStream() throws SQLException {
        java.io.Reader result = rset.getCharacterStream(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.Reader getCharacterStream(String columnLabel) throws SQLException {
        return rset.getCharacterStream(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public BigDecimal getBigDecimal() throws SQLException {
        BigDecimal result = rset.getBigDecimal(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return rset.getBigDecimal(columnLabel);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isBeforeFirst() throws SQLException {
        return rset.isBeforeFirst();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isAfterLast() throws SQLException {
        return rset.isAfterLast();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isFirst() throws SQLException {
        return rset.isFirst();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isLast() throws SQLException {
        return rset.isLast();
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void beforeFirst() throws SQLException {
        rset.beforeFirst();
        resetColumnIndex();
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void afterLast() throws SQLException {
        rset.afterLast();
        resetColumnIndex();
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean first() throws SQLException {
        boolean result = rset.first();
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean last() throws SQLException {
        boolean result = rset.last();
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getRow() throws SQLException {
        return rset.getRow();
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param row 行番号。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean absolute(int row) throws SQLException {
        boolean result = rset.absolute(row);
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param rows 行数。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean relative(int rows) throws SQLException {
        boolean result = rset.relative(rows);
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing][index=1] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean previous() throws SQLException {
        boolean result = rset.previous();
        resetColumnIndex();
        return result;
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param direction 方向。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setFetchDirection(int direction) throws SQLException {
        rset.setFetchDirection(direction);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getFetchDirection() throws SQLException {
        return rset.getFetchDirection();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param rows 行数。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setFetchSize(int rows) throws SQLException {
        rset.setFetchSize(rows);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getFetchSize() throws SQLException {
        return rset.getFetchSize();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getType() throws SQLException {
        return rset.getType();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getConcurrency() throws SQLException {
        return rset.getConcurrency();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean rowUpdated() throws SQLException {
        return rset.rowUpdated();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean rowInserted() throws SQLException {
        return rset.rowInserted();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean rowDeleted() throws SQLException {
        return rset.rowDeleted();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNull() throws SQLException {
        rset.updateNull(getColumnIndex());
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBoolean(Boolean x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateBoolean(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateByte(Byte x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateByte(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateShort(Short x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateShort(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateInt(Integer x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateInt(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateLong(Long x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateLong(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateFloat(Float x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateFloat(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateDouble(Double x) throws SQLException {
        if (x == null) {
            rset.updateNull(getColumnIndex());
        } else {
            rset.updateDouble(getColumnIndex(), x);
        }
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBigDecimal(BigDecimal x) throws SQLException {
        rset.updateBigDecimal(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateString(String x) throws SQLException {
        rset.updateString(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBytes(byte x[]) throws SQLException {
        rset.updateBytes(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateDate(java.sql.Date x) throws SQLException {
        rset.updateDate(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateTime(java.sql.Time x) throws SQLException {
        rset.updateTime(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateTimestamp(java.sql.Timestamp x) throws SQLException {
        rset.updateTimestamp(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(java.io.InputStream x, int length) throws SQLException {
        rset.updateAsciiStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(java.io.InputStream x, int length) throws SQLException {
        rset.updateBinaryStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(java.io.Reader x, int length) throws SQLException {
        rset.updateCharacterStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param scaleOrLength スケールまたは長さ
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(Object x, int scaleOrLength) throws SQLException {
        rset.updateObject(getColumnIndex(), x, scaleOrLength);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(Object x) throws SQLException {
        rset.updateObject(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNull(String columnLabel) throws SQLException {
        rset.updateNull(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        rset.updateBoolean(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateByte(String columnLabel, byte x) throws SQLException {
        rset.updateByte(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateShort(String columnLabel, short x) throws SQLException {
        rset.updateShort(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateInt(String columnLabel, int x) throws SQLException {
        rset.updateInt(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateLong(String columnLabel, long x) throws SQLException {
        rset.updateLong(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateFloat(String columnLabel, float x) throws SQLException {
        rset.updateFloat(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateDouble(String columnLabel, double x) throws SQLException {
        rset.updateDouble(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        rset.updateBigDecimal(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateString(String columnLabel, String x) throws SQLException {
        rset.updateString(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBytes(String columnLabel, byte x[]) throws SQLException {
        rset.updateBytes(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateDate(String columnLabel, java.sql.Date x) throws SQLException {
        rset.updateDate(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateTime(String columnLabel, java.sql.Time x) throws SQLException {
        rset.updateTime(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateTimestamp(String columnLabel, java.sql.Timestamp x) throws SQLException {
        rset.updateTimestamp(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) throws SQLException {
        rset.updateAsciiStream(columnLabel, x, length);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) throws SQLException {
        rset.updateBinaryStream(columnLabel, x, length);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(String columnLabel, java.io.Reader reader, int length) throws SQLException {
        rset.updateCharacterStream(columnLabel, reader, length);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel   columnNameによる列指定。
     * @param x             設定したい値。
     * @param scaleOrLength スケールまたは長さ
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        rset.updateObject(columnLabel, x, scaleOrLength);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(String columnLabel, Object x) throws SQLException {
        rset.updateObject(columnLabel, x);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void insertRow() throws SQLException {
        rset.insertRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * 項目値の設定が終わったあとに呼び出す行を更新するためのメソッド。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateRow() throws SQLException {
        rset.updateRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void deleteRow() throws SQLException {
        rset.deleteRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void refreshRow() throws SQLException {
        rset.refreshRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void cancelRowUpdates() throws SQLException {
        rset.cancelRowUpdates();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void moveToInsertRow() throws SQLException {
        rset.moveToInsertRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void moveToCurrentRow() throws SQLException {
        rset.moveToCurrentRow();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Statement getStatement() throws SQLException {
        return rset.getStatement();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param map 引き渡したい値を
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Object getObject(java.util.Map<String, Class<?>> map) throws SQLException {
        Object result = rset.getObject(getColumnIndex(), map);
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Ref getRef() throws SQLException {
        Ref result = rset.getRef(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Blob getBlob() throws SQLException {
        Blob result = rset.getBlob(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Clob getClob() throws SQLException {
        Clob result = rset.getClob(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Array getArray() throws SQLException {
        Array result = rset.getArray(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param map         引き渡したい値をMapにしたもの
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Object getObject(String columnLabel, java.util.Map<String, Class<?>> map) throws SQLException {
        return rset.getObject(columnLabel, map);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Ref getRef(String columnLabel) throws SQLException {
        return rset.getRef(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Blob getBlob(String columnLabel) throws SQLException {
        return rset.getBlob(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Clob getClob(String columnLabel) throws SQLException {
        return rset.getClob(columnLabel);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public Array getArray(String columnLabel) throws SQLException {
        return rset.getArray(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param cal 設定したい値。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Date getDate(Calendar cal) throws SQLException {
        java.sql.Date result = rset.getDate(getColumnIndex(), cal);
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param cal         設定したい値を捕捉する。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return rset.getDate(columnLabel, cal);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param cal 設定したい値を補足する。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Time getTime(Calendar cal) throws SQLException {
        java.sql.Time result = rset.getTime(getColumnIndex(), cal);
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param cal         設定したい値を補足する。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return rset.getTime(columnLabel, cal);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param cal 設定したい値を補足する。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Timestamp getTimestamp(Calendar cal) throws SQLException {
        java.sql.Timestamp result = rset.getTimestamp(getColumnIndex(), cal);
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param cal         設定したい値を補足する。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.sql.Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return rset.getTimestamp(columnLabel, cal);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.net.URL getURL() throws SQLException {
        java.net.URL result = rset.getURL(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.net.URL getURL(String columnLabel) throws SQLException {
        return rset.getURL(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateRef(java.sql.Ref x) throws SQLException {
        rset.updateRef(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateRef(String columnLabel, java.sql.Ref x) throws SQLException {
        rset.updateRef(columnLabel, x);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(java.sql.Blob x) throws SQLException {
        rset.updateBlob(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(String columnLabel, java.sql.Blob x) throws SQLException {
        rset.updateBlob(columnLabel, x);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(java.sql.Clob x) throws SQLException {
        rset.updateClob(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(String columnLabel, java.sql.Clob x) throws SQLException {
        rset.updateClob(columnLabel, x);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateArray(java.sql.Array x) throws SQLException {
        rset.updateArray(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateArray(String columnLabel, java.sql.Array x) throws SQLException {
        rset.updateArray(columnLabel, x);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public RowId getRowId() throws SQLException {
        RowId result = rset.getRowId(getColumnIndex());
        incrementColumnIndex();
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public RowId getRowId(String columnLabel) throws SQLException {
        return rset.getRowId(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateRowId(RowId x) throws SQLException {
        rset.updateRowId(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        rset.updateRowId(columnLabel, x);
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getHoldability() throws SQLException {
        return rset.getHoldability();
    }

    /**
     * [Passing] java.sql.ResultSet に由来するメソッドであり、内包する ResultSet
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isClosed() throws SQLException {
        return rset.isClosed();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param nString 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNString(String nString) throws SQLException {
        rset.updateNString(getColumnIndex(), nString);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param nString     設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNString(String columnLabel, String nString) throws SQLException {
        rset.updateNString(columnLabel, nString);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param nClob 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(NClob nClob) throws SQLException {
        rset.updateNClob(getColumnIndex(), nClob);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param nClob       設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        rset.updateNClob(columnLabel, nClob);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public NClob getNClob() throws SQLException {
        NClob result = rset.getNClob(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public NClob getNClob(String columnLabel) throws SQLException {
        return rset.getNClob(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public SQLXML getSQLXML() throws SQLException {
        SQLXML result = rset.getSQLXML(getColumnIndex());
        incrementColumnIndex();
        if (rset.wasNull()) {
            return null;
        }
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return rset.getSQLXML(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param xmlObject XMLオブジェクト。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateSQLXML(SQLXML xmlObject) throws SQLException {
        rset.updateSQLXML(getColumnIndex(), xmlObject);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param xmlObject   XMLオブジェクト。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        rset.updateSQLXML(columnLabel, xmlObject);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getNString() throws SQLException {
        String result = rset.getNString(getColumnIndex());
        incrementColumnIndex();
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getNString(String columnLabel) throws SQLException {
        return rset.getNString(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.Reader getNCharacterStream() throws SQLException {
        java.io.Reader result = rset.getNCharacterStream(getColumnIndex());
        incrementColumnIndex();
        return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.io.Reader getNCharacterStream(String columnLabel) throws SQLException {
        return rset.getNCharacterStream(columnLabel);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNCharacterStream(java.io.Reader x, long length) throws SQLException {
        rset.updateNCharacterStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNCharacterStream(String columnLabel, java.io.Reader reader, long length) throws SQLException {
        rset.updateNCharacterStream(columnLabel, reader, length);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(java.io.InputStream x, long length) throws SQLException {
        rset.updateAsciiStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(java.io.InputStream x, long length) throws SQLException {
        rset.updateBinaryStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(java.io.Reader x, long length) throws SQLException {
        rset.updateCharacterStream(getColumnIndex(), x, length);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {
        rset.updateAsciiStream(columnLabel, x, length);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {
        rset.updateBinaryStream(columnLabel, x, length);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(String columnLabel, java.io.Reader reader, long length) throws SQLException {
        rset.updateCharacterStream(columnLabel, reader, length);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param inputStream 設定したい
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(InputStream inputStream, long length) throws SQLException {
        rset.updateBlob(getColumnIndex(), inputStream, length);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param inputStream 設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        rset.updateBlob(columnLabel, inputStream, length);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(Reader reader, long length) throws SQLException {
        rset.updateClob(getColumnIndex(), reader, length);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        rset.updateClob(columnLabel, reader, length);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(Reader reader, long length) throws SQLException {
        rset.updateNClob(getColumnIndex(), reader, length);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        rset.updateNClob(columnLabel, reader, length);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNCharacterStream(java.io.Reader x) throws SQLException {
        rset.updateNCharacterStream(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {
        rset.updateNCharacterStream(columnLabel, reader);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(java.io.InputStream x) throws SQLException {
        rset.updateAsciiStream(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(java.io.InputStream x) throws SQLException {
        rset.updateBinaryStream(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(java.io.Reader x) throws SQLException {
        rset.updateCharacterStream(getColumnIndex(), x);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateAsciiStream(String columnLabel, java.io.InputStream x) throws SQLException {
        rset.updateAsciiStream(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param x           設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBinaryStream(String columnLabel, java.io.InputStream x) throws SQLException {
        rset.updateBinaryStream(columnLabel, x);
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {
        rset.updateCharacterStream(getColumnIndex(), reader);
        incrementColumnIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param inputStream 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(InputStream inputStream) throws SQLException {
        rset.updateBlob(getColumnIndex(), inputStream);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param inputStream 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        rset.updateBlob(columnLabel, inputStream);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(Reader reader) throws SQLException {
        rset.updateClob(getColumnIndex(), reader);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        rset.updateClob(columnLabel, reader);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(Reader reader) throws SQLException {
        rset.updateNClob(getColumnIndex(), reader);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel columnNameによる列指定。
     * @param reader      設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        rset.updateNClob(columnLabel, reader);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex 引数の省略を実現した、結果セットから値を取得するメソッドです。省略された
     * columnIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param <T>  T
     * @param type 型。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public <T> T getObject(Class<T> type) throws SQLException {
        // TODO FIXME このパターンのみ、メソッドではなく直接変数の値にアクセスしています。
        // TODO FIXME 同様の理由で wasNullチェックは実装されていません。
        return rset.getObject(internalColumnIndex++, type);
        // incrementColumnIndex();
        // return result;
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param <T>         T
     * @param columnLabel columnNameによる列指定。
     * @param type        型。
     * @return 結果。
     * @throws SQLException SQL例外が発生した場合。
     */
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return rset.getObject(columnLabel, type);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象の
     * @param scaleOrLength スケールまたは長さ
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        rset.updateObject(getColumnIndex(), x, targetSqlType, scaleOrLength);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel   columnNameによる列指定。
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ。
     * @param scaleOrLength スケールまたは長さ
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength)
            throws SQLException {
        rset.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
    }

    /**
     * [Rsvr][index++] RsvrJdbc により columnIndex
     * 引数の省略を実現した、検索結果の項目の値を設定するメソッドです。省略された columnIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する columnIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(Object x, SQLType targetSqlType) throws SQLException {
        rset.updateObject(getColumnIndex(), x, targetSqlType);
        incrementColumnIndex();
    }

    /**
     * [Passing][columnLabel] java.sql.ResultSet に由来する columnLabel を使用するメソッドであり、内包する
     * ResultSet の該当メソッドをそのまま呼び出して連携します。
     * 
     * @param columnLabel   columnNameによる列指定。
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
        rset.updateObject(columnLabel, x, targetSqlType);
    }

    ///////////////////////////////////////////////
    // RsvrResultSet 独自メソッド

    /**
     * [Rsvr独自][index++] java.util.Date 型で日時を取得します。(java.sql.Date、java.sql.Timestamp
     * を使いたくない場合に役立ちます。)
     * 
     * 開発者のソースコードから java.sql.Timestamp 型を見えなくするためのメソッドです。
     * 
     * ※私が確認した範囲の RDB では 内部的に java.sql.Timestamp にマッピングすることによりすべてうまくいきました。しかし RDB
     * の種類によってこれは期待しない挙動を行う可能性があります。その場合には setJavaUtilDate、getJavaUtilDate
     * を使用せずに、java.sql.Timestampを引数や戻り値で使用する他のメソッドを使用するなどワークアラウンドをおこなってください。
     * 
     * @return java.util.Date 型による取り出した日時データ。java.sql.Timestampからダウンキャストした値を返却。
     * @throws SQLException SQL例外が発生した場合。
     */
    public java.util.Date getJavaUtilDate() throws SQLException {
        // java.util.Date 形式で日時を取得します。内部的には java.sql.Timestampを取得したうえで
        // java.util.Dateにダウンキャストしています。
        return getTimestamp();
    }
}
