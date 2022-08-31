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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * RsvrJdbc が提供する ResultSetMetaData です。
 * 
 * <h1>About RsvrJdbc</h1>
 *
 * `RsvrJdbc` は、JDBCプログラミングにおける `ちいさないらいら` を解消することを目的に作成した軽量ライブラリです。
 * JDBCプログラミング体験そのままが活用できることを原則に `RsvrJdbc` における小さな変更点を知るだけで
 * JDBCプログラミング経験者はすぐに利用開始でき、そして `ちいさないらいら` を解消できることを目指して `RsvrJdbc` は作られています。
 * 
 * <h2>Usage: MetaData取得</h2>
 * 
 * `RsvrResultSetMetaData` は、columnIndex 指定の省略を提供します。columnIndex
 * は自動的に増えることはありません。columnIndex は RsvrJdbc 独自の nextColumn() メソッドの呼び出し、または
 * incrementColumnIndex() メソッドの呼び出しによりインクリメントすることが可能です。
 * 
 * <pre>
 * try (RsvrPreparedStatement stmt = RsvrJdbc
 *         .wrap(conn.prepareStatement("SELECT RecordId, Value1 FROM " + CASEID + " ORDER BY RecordId"))) {
 *     RsvrResultSetMetaData rsetMeta = stmt.getMetaData();
 *     do {
 *         System.err.println(rsetMeta.getColumnName() + " (" + rsetMeta.getColumnIndex() + ")");
 *         System.err.println("  db type: " + rsetMeta.getColumnTypeName());
 *         System.err.println("  java   : " + rsetMeta.getColumnClassName());
 *         System.err.println("  table  : " + rsetMeta.getTableName());
 *     } while (rsetMeta.nextColumn());
 * }
 * </pre>
 * 
 * `RsvrResultSetMetaData` は JDBC API で使用可能な getColumnTypeName や getTableName
 * を利用できますが、通常の JDBC プログラミングでは指定の必要がある columnIndex 引数の指定が不要になっています。
 * 
 * 実は、通常の JDBC プログラミングで この columnIndex 引数の指定は `ちいさないらいら` です。ほとんどの場合は 1 から順番に 2,
 * 3, 4 と順に増やしながら引数指定します。この定型的な指定は意外に面倒なうえに、仕様変更の際におけるバグの作り込みの原因にもなりがちです。
 * 
 * `RsvrJdbc` では、この columnIndex の指定を省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？
 * 
 * @see java.sql.ResultSet
 * @version 1.0.2.20220901
 */
public class RsvrResultSetMetaData {
    /**
     * 内包する ResultSetMetaData のインスタンス。
     */
    private ResultSetMetaData rsetMeta = null;

    /**
     * プログラマーの引数指定から columnIndex の指定を省略できるようにするために内部的に保持する変数。 RsvrResultSetMetaData
     * が提供する機能の一つを実現するために使用される。
     */
    private int internalColumnIndex = 1;

    /**
     * getColumnCount メソッドの呼び出し結果をキャッシュするための変数。
     */
    private int cachedLastColumnCount = -1;

    /**
     * 引数なしコンストラクタを隠します。
     */
    protected RsvrResultSetMetaData() {
    }

    /**
     * [Rsvr独自] 与えられた ResultSetMetaData をもとに RsvrJdbc が提供する RsvrResultSetMetaData
     * インスタンスを作成します。
     * 
     * @param meta 通常の ResultSetMetaData。
     */
    public RsvrResultSetMetaData(ResultSetMetaData meta) {
        this.rsetMeta = meta;
    }

    /**
     * [Rsvr独自][index=1] RsvrResultSetMetaData 内部の columnIndex をリセットします。
     */
    public void resetColumnIndex() {
        internalColumnIndex = 1;
    }

    /**
     * [Rsvr独自] 内部的に保持する columnIndex の値を設定します。
     * 
     * @param columnIndex 設定したい columnIndex値。
     */
    public void setColumnIndex(int columnIndex) {
        internalColumnIndex = columnIndex;
    }

    /**
     * [Rsvr独自] RsvrResultSetMetaData 内部の columnIndex を取得します。
     * 
     * @return 内包する columnIndexの値。
     */
    public int getColumnIndex() {
        return internalColumnIndex;
    }

    /**
     * [Rsvr独自][index++] 内部的に保持する columnIndex の値をインクリメントします。
     */
    public void incrementColumnIndex() {
        internalColumnIndex++;
    }

    /**
     * [Rsvr独自][index++] 次の columnIndex に進みます。カラム数を超えた場合は false を返却します。
     * 
     * @return 次の columnIndex に相当する列が存在したら true。列が存在しなければ false.
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean nextColumn() throws SQLException {
        incrementColumnIndex();
        if (getColumnIndex() <= getColumnCount()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * [Passing] java.sql.ResultSetMetaData に由来するメソッドであり、内包する ResultSetMetaData
     * の該当メソッドをそのまま呼び出して連携しますが、一度呼び出して得られた columnCountは次回呼び出し時にそのまま活用します(キャッシュします)。
     * 
     * @return 得られた値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getColumnCount() throws SQLException {
        if (cachedLastColumnCount > 0) {
            return cachedLastColumnCount;
        }
        // 得られた項目数を記憶。
        cachedLastColumnCount = rsetMeta.getColumnCount();
        return cachedLastColumnCount;
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isAutoIncrement() throws SQLException {
        return rsetMeta.isAutoIncrement(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isCaseSensitive() throws SQLException {
        return rsetMeta.isCaseSensitive(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isSearchable() throws SQLException {
        return rsetMeta.isSearchable(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isCurrency() throws SQLException {
        return rsetMeta.isCurrency(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int isNullable() throws SQLException {
        return rsetMeta.isNullable(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isSigned() throws SQLException {
        return rsetMeta.isSigned(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getColumnDisplaySize() throws SQLException {
        return rsetMeta.getColumnDisplaySize(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getColumnLabel() throws SQLException {
        return rsetMeta.getColumnLabel(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getColumnName() throws SQLException {
        return rsetMeta.getColumnName(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getSchemaName() throws SQLException {
        return rsetMeta.getSchemaName(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getPrecision() throws SQLException {
        return rsetMeta.getPrecision(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getScale() throws SQLException {
        return rsetMeta.getScale(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * h2 database ではテーブル名が返却されます。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getTableName() throws SQLException {
        return rsetMeta.getTableName(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getCatalogName() throws SQLException {
        return rsetMeta.getCatalogName(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public int getColumnType() throws SQLException {
        return rsetMeta.getColumnType(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * h2 database では、BIGINT や CHARACTER VARYING といった文字列が返却します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getColumnTypeName() throws SQLException {
        return rsetMeta.getColumnTypeName(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isReadOnly() throws SQLException {
        return rsetMeta.isReadOnly(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isWritable() throws SQLException {
        return rsetMeta.isWritable(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean isDefinitelyWritable() throws SQLException {
        return rsetMeta.isDefinitelyWritable(getColumnIndex());
    }

    /**
     * [Rsvr] RsvrJdbc により columnIndex
     * 引数の省略を実現した、ResultSetMetaDataから同名メソッドを呼び出して値を取得するメソッドです。 省略された columnIndex
     * はクラス内部で保持した値を使用します。
     * 
     * h2 database では java.lang.Long や java.lang.String などが返却されます。
     * 
     * @return 取得した値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public String getColumnClassName() throws SQLException {
        return rsetMeta.getColumnClassName(getColumnIndex());
    }
}
