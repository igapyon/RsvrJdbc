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
import java.sql.Connection;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.util.Calendar;

/**
 * RsvrJdbc が提供する PreparedStatement です。
 * 
 * <h1>About RsvrJdbc</h1>
 *
 * `RsvrJdbc` は、JDBCプログラミングにおける `ちいさないらいら` を解消することを目的に作成した軽量ライブラリです。
 * JDBCプログラミング体験そのままが活用できることを原則に `RsvrJdbc` における小さな変更点を知るだけで
 * JDBCプログラミング経験者はすぐに利用開始でき、そして `ちいさないらいら` を解消できることを目指して `RsvrJdbc` は作られています。
 * 
 * <h2>Usage: 更新 (追加と削除も同様)</h2>
 * 
 * 引き続き基本的な使い方を見てみましょう。以下がシンプルな更新の例です。 conn.prepareStatement() の結果を
 * RsvrJdbc.wrap() で受けることにより、PreparedStatement に相当する `RsvrPreparedStatement`
 * を入手できます。
 * 
 * <pre>
try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE MyTable SET IssueKey=?, Summary=?, KeyId=?, ProjectId=? WHERE IssueId = ?"))) {
    stmtMod.setString(source.getIssueKey());
    stmtMod.setString(source.getSummary());
    stmtMod.setLong(source.getKeyId());
    stmtMod.setLong(source.getProjectId());
    stmtMod.setLong(source.getId());
    stmtMod.executeUpdateSingleRow();
 * </pre>
 * 
 * `RsvrPreparedStatement` は setString や setLong が利用できますが、通常の JDBC
 * プログラミングでは指定する必要のある parameterIndex 引数の指定が不要になっています。
 * 
 * 通常の JDBC プログラミングで この parameterIndex 引数は `ちいさないらいら` です。ほとんどの場合は 1 から順番に 2, 3,
 * 4
 * と順に増やしながら引数指定するのですが、この定型的な指定が意外に面倒なうえに、SQL途中への項目追加などの仕様変更の際におけるバグの作り込みの原因にもなりがちです。
 * 
 * `RsvrJdbc` では、この parameterIndex の指定を省略できてしまうのです。どうでしょう、`ちいさないらいら`
 * のひとつが解決できましたか？
 * 
 * また、`RsvrPreparedStatement#executeUpdateSingleRow` という見慣れないメソッドがあります。これは
 * `RsvrJdbc` で独自に追加されたメソッドです。比較的多くの UPDATE 文では 1行のみレコードを更新することが知られています。そして
 * 1行のみ更新されたかどうかの確認は、executeUpdate を呼び出した後の戻り値が 1
 * であることを確認することにより担保できます。`RsvrJdbc` においても、そのような一般的な使用方法も可能ですが、ここで提供する
 * executeUpdateSingleRow を使用することで実行結果が1行であったことを担保できるようになり、コーディングの簡素化を実現できます。
 * 
 * `RsvrPreparedStatement#executeUpdateSingleRow` では、内部で executeUpdate
 * を呼び出して戻り値が1であることを確認します。仮に 1 ではなかった時には SQLException を throw
 * するように作られています。これにより、いつもの `ちいさないらいら` の UPDATE
 * 文の実行結果が1件であることの確認ソースコードの記述不要を実現しているのです。
 * 
 * `RsvrJdbc` では、この executeUpdate の実行結果件数の確認を省略できてしまうのです。どうでしょう、`ちいさないらいら`
 * のひとつが解決できましたか？
 * 
 * でも待ってください。parameterIndex の指定が省略されるとして、この値はいつリセットされるのでしょうか。次に
 * RsvrPreparedStatement 内部の parameterIndex のリセットタイミングを見てみましょう。
 * 
 * <h2>RsvrPreparedStatement 内部の parameterIndex のリセットタイミング</h2>
 * 
 * 以下のメソッド呼び出しのタイミングで RsvrPreparedStatement 内部の parameterIndex のリセットはおこなわれます。
 * 
 * <ul>
 * <li>RsvrPreparedStatement#clearParameters</li>
 * <li>RsvrPreparedStatement#close</li>
 * <li>RsvrPreparedStatement#execute</li>
 * <li>RsvrPreparedStatement#executeLargeUpdate</li>
 * <li>RsvrPreparedStatement#executeQuery</li>
 * <li>RsvrPreparedStatement#executeUpdate</li>
 * </ul>
 * 
 * これは、たいていの一般的な JDBCプログラミングではステートメントの実行時や clearParameters メソッドを呼び出す際に
 * parameterIndex を1へとリセットすることが観察されたことからも、上記のような仕様としています。
 * 
 * また、RsvrPreparedStatement#resetParameterIndex メソッドを明示的に呼び出すことによっても
 * parameterIndex のリセットを行うことが可能です。
 * 
 * @see java.sql.PreparedStatement
 * @version 1.0.2.20220901
 */
public class RsvrPreparedStatement extends RsvrStatement {
    /**
     * プログラマーの引数指定から parameterIndex の指定を省略できるようにするために内部的に保持する変数。
     * RsvrPreparedStatement が提供する機能の一つを実現するために使用される。
     */
    private int internalParameterIndex = 1;

    /**
     * 引数なしコンストラクタを隠します。
     */
    protected RsvrPreparedStatement() {
    }

    /**
     * [Rsvr独自] 与えられた PreparedStatement をもとに RsvrJdbc が提供する RsvrPreparedStatement
     * インスタンスを作成します。
     * 
     * @param stmt 通常の PreparedStatement。
     */
    public RsvrPreparedStatement(PreparedStatement stmt) {
        this.stmt = stmt;
    }

    /**
     * [Rsvr独自] 内部的に保持する もとの PreparedStatement インスタンスを取得します。
     * 
     * 通常はこのメソッドは使用されませんが、ワークアラウンド的な実装対応が必要な時にこのメソッドを使用することでしょう。
     * 
     * @return 内部的に保持する もとの PreparedStatement インスタンス。
     */
    public PreparedStatement getInternalPreparedStatement() {
        return (PreparedStatement) stmt;
    }

    /**
     * [Rsvr独自][index++] 内部的に保持する parameterIndex の値をインクリメントします。
     * 
     * 通常はこのメソッドは直接呼び出すシーンはないと想定します。
     */
    public void incrementParameterIndex() {
        internalParameterIndex++;
    }

    /**
     * [Rsvr独自][index=1] RsvrPreparedStatement 内部の parameterIndex のリセットします。
     * 
     * なお、このメソッドを明示的に呼び出す以外に、 以下のメソッド呼び出しのタイミングで RsvrPreparedStatement 内部の
     * parameterIndex のリセットはおこなわれます。（以下のメソッドがこのメソッドを呼び出します）
     * 
     * <ul>
     * <li>RsvrPreparedStatement#clearParameters</li>
     * <li>RsvrPreparedStatement#close</li>
     * <li>RsvrPreparedStatement#execute</li>
     * <li>RsvrPreparedStatement#executeLargeUpdate</li>
     * <li>RsvrPreparedStatement#executeQuery</li>
     * <li>RsvrPreparedStatement#executeUpdate</li>
     * </ul>
     * 
     * これは、たいていの一般的な JDBCプログラミングではステートメントの実行時や clearParameters メソッドを呼び出す際に
     * parameterIndex を1へとリセットすることが観察されたことからも、上記のような仕様としています。
     */
    public void resetParameterIndex() {
        internalParameterIndex = 1;
    }

    /**
     * [Rsvr独自] 内部的に保持する parameterIndex の値を設定します。
     * 
     * @param parameterIndex 設定したい parameterIndex値。
     */
    public void setParameterIndex(int parameterIndex) {
        internalParameterIndex = parameterIndex;
    }

    /**
     * [Rsvr独自] RsvrPreparedStatement 内部の parameterIndex を取得します。
     * 
     * 通常はこのメソッドは直接呼び出すシーンはないと想定します。
     * 
     * @return 内包する parameterIndexの値。
     */
    public int getParameterIndex() {
        return internalParameterIndex;
    }

    ///////////////////////////////////////////////
    // Statement 由来メソッド

    /**
     * [Passing][index=1] AutoCloseable の close を実現します。
     * 
     * （念の為）内包する parameterIndex の値をリセットします。
     */
    @Override
    public void close() throws SQLException {
        getInternalPreparedStatement().close();
        resetParameterIndex();
    }

    /**
     * [Passing] java.sql.Statement に由来するメソッドであり、内包する Statement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    @Override
    public Connection getConnection() throws SQLException {
        return getInternalPreparedStatement().getConnection();
    }

    // Statement 由来メソッド
    ///////////////////////////////////////////////

    ///////////////////////////////////////////////
    // PreparedStatement 由来メソッド

    /**
     * [Passing][index=1] java.sql.PreparedStatement
     * に由来するメソッドですが、クエリを実行し、ResultSetの代わりに、RsvrJdbc による RsvrResultSetを返却します。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @return ResultSetの代わりに、RsvrResultSetを返却します。
     * @throws SQLException SQL例外が発生した場合。
     */
    public RsvrResultSet executeQuery() throws SQLException {
        ResultSet result = getInternalPreparedStatement().executeQuery();
        resetParameterIndex();
        return RsvrJdbc.wrap(result);
    }

    /**
     * [Passing][index=1] java.sql.PreparedStatement に由来するメソッドであり、内包する
     * PreparedStatement の該当メソッドをそのまま呼び出して連携します。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public int executeUpdate() throws SQLException {
        int result = getInternalPreparedStatement().executeUpdate();
        resetParameterIndex();
        return result;
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param sqlType SQLタイプ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNull(int sqlType) throws SQLException {
        getInternalPreparedStatement().setNull(getParameterIndex(), sqlType);
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBoolean(Boolean x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.BOOLEAN);
        } else {
            getInternalPreparedStatement().setBoolean(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setByte(Byte x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.TINYINT);
        } else {
            getInternalPreparedStatement().setByte(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setShort(Short x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.SMALLINT);
        } else {
            getInternalPreparedStatement().setShort(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setInt(Integer x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.INTEGER);
        } else {
            getInternalPreparedStatement().setInt(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setLong(Long x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.BIGINT);
        } else {
            getInternalPreparedStatement().setLong(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setFloat(Float x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.FLOAT);
        } else {
            getInternalPreparedStatement().setFloat(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現したステートメントに値を設定するメソッドです。設定する値はプリミティブ型ではなくラッパー型を指定することができ、null
     * も設定値として指定することが可能です。省略された parameterIndex はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値をプリミティブ型のラッパークラスのインタンスで渡す。null の指定も可能。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setDouble(Double x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.DOUBLE);
        } else {
            getInternalPreparedStatement().setDouble(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBigDecimal(BigDecimal x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.DECIMAL);
        } else {
            getInternalPreparedStatement().setBigDecimal(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setString(String x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setString(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBytes(byte x[]) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.ARRAY);
        } else {
            getInternalPreparedStatement().setBytes(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setDate(java.sql.Date x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.DATE);
        } else {
            getInternalPreparedStatement().setDate(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setTime(java.sql.Time x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.TIME);
        } else {
            getInternalPreparedStatement().setTime(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setTimestamp(java.sql.Timestamp x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.TIMESTAMP);
        } else {
            getInternalPreparedStatement().setTimestamp(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setAsciiStream(java.io.InputStream x, int length) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setAsciiStream(getParameterIndex(), x, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBinaryStream(java.io.InputStream x, int length) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBinaryStream(getParameterIndex(), x, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] java.sql.PreparedStatement に由来するメソッドであり、内包する
     * PreparedStatement の該当メソッドをそのまま呼び出して連携します。
     * 
     * 併せて 内包する parameterIndex の値をリセットします。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void clearParameters() throws SQLException {
        getInternalPreparedStatement().clearParameters();
        resetParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象の
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setObject(Object x, int targetSqlType) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.JAVA_OBJECT);
        } else {
            getInternalPreparedStatement().setObject(getParameterIndex(), x, targetSqlType);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setObject(Object x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.JAVA_OBJECT);
        } else {
            getInternalPreparedStatement().setObject(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Passing][index=1] java.sql.PreparedStatement に由来するメソッドであり、内包する
     * PreparedStatement の該当メソッドをそのまま呼び出して連携します。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public boolean execute() throws SQLException {
        boolean result = getInternalPreparedStatement().execute();
        resetParameterIndex();
        return result;
    }

    /**
     * [Passing] java.sql.PreparedStatement に由来するメソッドであり、内包する PreparedStatement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void addBatch() throws SQLException {
        getInternalPreparedStatement().addBatch();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setCharacterStream(java.io.Reader reader, int length) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setCharacterStream(getParameterIndex(), reader, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setRef(Ref x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.REF);
        } else {
            getInternalPreparedStatement().setRef(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBlob(Blob x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBlob(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setClob(Clob x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setClob(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setArray(Array x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.ARRAY);
        } else {
            getInternalPreparedStatement().setArray(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr] java.sql.PreparedStatement に由来するメソッドであり、内包する PreparedStatement
     * の該当メソッドをそのまま呼び出しますが、RsvrResultSetMetaData でラップして返却します。
     * 
     * @return RsvrResultSetMetaData でラップした ResultSetMetaData.
     * @throws SQLException SQL例外が発生した場合。
     */
    public RsvrResultSetMetaData getMetaData() throws SQLException {
        return RsvrJdbc.wrap(getInternalPreparedStatement().getMetaData());
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x   設定したい値。
     * @param cal カレンダー。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setDate(java.sql.Date x, Calendar cal) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.DATE);
        } else {
            getInternalPreparedStatement().setDate(getParameterIndex(), x, cal);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x   設定したい値。
     * @param cal カレンダー。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setTime(java.sql.Time x, Calendar cal) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.TIME);
        } else {
            getInternalPreparedStatement().setTime(getParameterIndex(), x, cal);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x   設定したい値。
     * @param cal カレンダー。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setTimestamp(java.sql.Timestamp x, Calendar cal) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.TIMESTAMP);
        } else {
            getInternalPreparedStatement().setTimestamp(getParameterIndex(), x, cal);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param sqlType  SQLタイプ。
     * @param typeName タイプ名。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNull(int sqlType, String typeName) throws SQLException {
        getInternalPreparedStatement().setNull(getParameterIndex(), sqlType, typeName);
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setURL(java.net.URL x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setURL(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Passing] java.sql.PreparedStatement に由来するメソッドであり、内包する PreparedStatement
     * の該当メソッドをそのまま呼び出して連携します。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return getInternalPreparedStatement().getParameterMetaData();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setRowId(RowId x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setRowId(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param value 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNString(String value) throws SQLException {
        if (value == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNString(getParameterIndex(), value);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param value  設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNCharacterStream(Reader value, long length) throws SQLException {
        if (value == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNCharacterStream(getParameterIndex(), value, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param value 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNClob(NClob value) throws SQLException {
        if (value == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNClob(getParameterIndex(), value);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setClob(Reader reader, long length) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setClob(getParameterIndex(), reader, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param inputStream 設定したい値
     * @param length      長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBlob(InputStream inputStream, long length) throws SQLException {
        if (inputStream == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBlob(getParameterIndex(), inputStream, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNClob(Reader reader, long length) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNClob(getParameterIndex(), reader, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param xmlObject XMLオブジェクト。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setSQLXML(SQLXML xmlObject) throws SQLException {
        if (xmlObject == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setSQLXML(getParameterIndex(), xmlObject);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ
     * @param scaleOrLength スケールまたは長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setObject(Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.JAVA_OBJECT);
        } else {
            getInternalPreparedStatement().setObject(getParameterIndex(), x, targetSqlType, scaleOrLength);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setAsciiStream(java.io.InputStream x, long length) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setAsciiStream(getParameterIndex(), x, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x      設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBinaryStream(java.io.InputStream x, long length) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBinaryStream(getParameterIndex(), x, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @param length 長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setCharacterStream(java.io.Reader reader, long length) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setCharacterStream(getParameterIndex(), reader, length);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setAsciiStream(java.io.InputStream x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setAsciiStream(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBinaryStream(java.io.InputStream x) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBinaryStream(getParameterIndex(), x);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setCharacterStream(java.io.Reader reader) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setCharacterStream(getParameterIndex(), reader);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param value 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNCharacterStream(Reader value) throws SQLException {
        if (value == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNCharacterStream(getParameterIndex(), value);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setClob(Reader reader) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setClob(getParameterIndex(), reader);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param inputStream 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setBlob(InputStream inputStream) throws SQLException {
        if (inputStream == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARBINARY);
        } else {
            getInternalPreparedStatement().setBlob(getParameterIndex(), inputStream);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param reader 設定したい値。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setNClob(Reader reader) throws SQLException {
        if (reader == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.VARCHAR);
        } else {
            getInternalPreparedStatement().setNClob(getParameterIndex(), reader);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ
     * @param scaleOrLength スケールまたは長さ。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setObject(Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.JAVA_OBJECT);
        } else {
            getInternalPreparedStatement().setObject(getParameterIndex(), x, targetSqlType, scaleOrLength);
        }
        incrementParameterIndex();
    }

    /**
     * [Rsvr][index++] RsvrJdbc により parameterIndex
     * 引数の省略を実現した、ステートメントに値を設定するメソッドです。省略された parameterIndex
     * はクラス内部で保持した値を使用たうえで呼び出し後にインクリメントします。
     * 
     * 内包する parameterIndex の値をインクリメントします。
     * 
     * @param x             設定したい値。
     * @param targetSqlType 対象のSQLタイプ
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setObject(Object x, SQLType targetSqlType) throws SQLException {
        if (x == null) {
            getInternalPreparedStatement().setNull(getParameterIndex(), java.sql.Types.JAVA_OBJECT);
        } else {
            getInternalPreparedStatement().setObject(getParameterIndex(), x, targetSqlType);
        }
        incrementParameterIndex();
    }

    /**
     * [Passing][index=1] java.sql.PreparedStatement に由来するメソッドであり、内包する
     * PreparedStatement の該当メソッドをそのまま呼び出して連携します。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @return 実施結果
     * @throws SQLException SQL例外が発生した場合。
     */
    public long executeLargeUpdate() throws SQLException {
        long result = getInternalPreparedStatement().executeLargeUpdate();
        resetParameterIndex();
        return result;
    }

    // PreparedStatement 由来メソッド
    ///////////////////////////////////////////////

    ///////////////////////////////////////////////
    // RsvrPreparedStatement 独自メソッド

    /**
     * [Rsvr独自][index=1] `RsvrJdbc` で独自に追加された、ステートメントの実行結果が1件であることが保証できる
     * executeUpdate 相当のメソッドです。
     * 
     * 比較的多くの UPDATE 文では 1行のみレコードを更新することが知られています。そして 1行のみ更新されたかどうかの確認は、executeUpdate
     * を呼び出した後の戻り値が 1 であることを確認することにより担保できます。`RsvrJdbc`
     * においても、そのような一般的な使用方法も可能ですが、ここで提供する executeUpdateSingleRow
     * を使用することで実行結果が1行であったことを担保できるようになり、コーディングの簡素化を実現できます。
     * 
     * `RsvrPreparedStatement#executeUpdateSingleRow` では、内部で executeUpdate
     * を呼び出して戻り値が1であることを確認します。仮に 1 ではなかった時には SQLException を throw
     * するように作られています。これにより、いつもの `ちいさないらいら` の UPDATE
     * 文の実行結果が1件であることの確認ソースコードの記述不要を実現しているのです。
     * 
     * 内包する parameterIndex の値をリセットします。
     * 
     * @throws SQLException SQL例外が発生した場合。
     */
    public void executeUpdateSingleRow() throws SQLException {
        int rowCount = this.executeUpdate();
        if (rowCount == 0) {
            throw new SQLException("RsvrPreparedStatement#executeUpdateSingleRow: Unexpected: No row processed.");
        }
        if (rowCount >= 2) {
            throw new SQLException(
                    "RsvrPreparedStatement#executeUpdateSingleRow: Unexpected: Multiple rows processed.");
        }
    }

    /**
     * [Rsvr独自][index++] パラメータに java.util.Date 値を設定します。
     * 
     * 開発者のソースコードから java.sql.Timestamp 型を見えなくするためのメソッドです。
     * 
     * ※私が確認した範囲の RDB では 内部的に java.sql.Timestamp にマッピングすることによりすべてうまくいきました。しかし RDB
     * の種類によってこれは期待しない挙動を行う可能性があります。その場合には setJavaUtilDate、getJavaUtilDate
     * を使用せずに、java.sql.Timestampを引数や戻り値で使用する他のメソッドを使用するなどワークアラウンドをおこなってください。
     * 
     * @param javaUtilDate java.util.Date 型による設定したい日時データ。内部的に
     *                     java.sql.Timestampに変換した上でJDBCに設定する。
     * @throws SQLException SQL例外が発生した場合。
     */
    public void setJavaUtilDate(java.util.Date javaUtilDate) throws SQLException {
        if (javaUtilDate == null) {
            setTimestamp(null);
        } else {
            setTimestamp(new java.sql.Timestamp(javaUtilDate.getTime()));
        }
    }
}
