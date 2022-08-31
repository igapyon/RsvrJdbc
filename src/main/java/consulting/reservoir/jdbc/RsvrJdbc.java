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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * PreparedStatement および ResultSet を引数に、RsvrJdbc クラスを導入できる、RsvrJdbc
 * で最初に利用する構成に切り替えための、最初に使用するクラスです。
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
 * <h2>null との格闘</h2>
 * 
 * JDBCプログラミングにおける `ちいさないらいら` のひとつに null との格闘があります。ご承知のように JDBC API
 * ではいくつかのデータ型においてプリミティブ型が使用されます。Javaではプリミティブ型は null を扱うことができないために、JDBC API
 * での読み書きにおいては、null の場合は別途 ResultSet#wasNull や PreparedStatement#setNull
 * を使用する必要があります。しかも PreparedStatement#setNull の第2引数 sqlType を指定することも面倒なことです。
 * 
 * `RsvrJdbc` では、この null との格闘を解決するために、プリミティブ型で提供されるメソッド引数や戻り値のいくつかを
 * プリミティブ型の値をラップするクラスの引数や戻り値で置き換えています。
 * 
 * PreparedStatement#setInt の引数は int ですが、RsvrPreparedStatement#setInt の引数は
 * Integer に置き換えられています。この箇所は `RsvrJdbc`
 * が明示的に仕様を調整している重要な箇所の一つです。一方で、この変更によって、null との格闘のいらいらの解消を実現できるのです。
 * 
 * RsvrPreparedStatement#setInt は、内部処理で引数に null が与えられたら値に null をセットするための setNull
 * メソッドを呼び出し、それ以外の場合は通常の setInt を呼び出して値を設定するように分岐しています。
 * 
 * 同様に ResultSet#getInt の戻り値は int ですが、RsvrResultSet#getInt の戻り値は Integer
 * に置き換えられています。RsvrResultSet#getInt は内部処理で getInt の呼び出し後に wasNull を呼び出してその結果が
 * true の場合は null を戻し、それ以外の場合は getInt で得られた値を Integer で戻すように分岐しています。
 * 
 * なお、この null に対応するための機能のために、RsvrResultSet#getXXX メソッドの戻り値が今まではプリミティブ型で null
 * を意識する必要がなかったものが、`RsvrJdbc` では null
 * を意識する必要があるよう変わる点に注意が必要です。RsvrResultSet#getXXX で戻された値は (プリミティブ型ではなく)
 * ラッパークラスで受けた上で、適切に null であるか確認するようコーディングすることを推奨します。
 * 
 * このように `RsvrJdbc` では、この null にまつわる面倒ごとを省略できてしまうのです。どうでしょう、`ちいさないらいら`
 * のひとつが解決できましたか？
 * 
 * <h2>java.sql.Date、java.sql.Timestamp との格闘</h2>
 * 
 * JDBCプログラミングにおける `ちいさないらいら` のひとつに 日付型、日時型 との格闘があります。 ご承知のように JDBC API では
 * `java.sql.Date` や `java.sql.Timestamp` という JDBC用のデータ型を持っています。一方で 一般的な Java
 * プログラミングでは `java.util.Date` や新しい日付型、日時型を使用していることでしょう。 この型の違いを埋める作業が Java JDBC
 * プログラミングの `ちいさないらいら`
 * であることを私は信じています。身近なところとしては、IDEのimport自動編成機能などにおいてちょっとした面倒ごとが発生しがちであることも理由のひとつです。
 * 
 * `RsvrJdbc` ではこの不満を解消するために、`RsvrPreparedStatement#setJavaUtilDate` および
 * `ResultSet#getJavaUtilDate` という java.util.Date
 * を入出力するためのメソッドを提供します。なお現状では新しい日付型、日時型については未対応です。 これら `RsvrJdbc`
 * が提供するクラスのメソッドを使用することにより、 `java.sql.Date` や `java.sql.Timestamp` という
 * JDBC用のデータ型を見る必要がなくなりました。
 * 
 * ※私が使用する範囲の RDB では 内部的に java.sql.Timestamp にマッピングすることによりすべてうまくいきました。しかし RDB
 * の種類によってこれは期待しない挙動を行う可能性があります。その場合には setJavaUtilDate、getJavaUtilDate
 * を使用せずに、java.sql.Timestampを引数や戻り値で使用する他のメソッドを使用するなどワークアラウンドをおこなってください。
 * 
 * `RsvrJdbc` では、この java.sql.Date、 java.sql.Timestamp
 * にまつわる面倒ごとを省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？
 * 
 * <h1>JPA や ORM で 既にうまくいっている人には不要</h1>
 * 
 * JPA や Hibernate や MyBatis といった ORM をご利用されていて、それらでうまくいっている人には、`RsvrJdbc`
 * は不要なものです。 `RsvrJdbc` は、世の中のいろいろな Java データベースアクセス手法を試したがいろいろあって絶望し、JDBC API
 * によるプログラムに仕方なく戻ってきた人に最適なライブラリとして書かれたものだからです。
 * 
 * そして `RsvrJdbc` は `SQL` を自分で記述する必要があります。さらに `RsvrJdbc` には ORM の機能はありません。(外付けの
 * ORM 機能が `RsvrJdbc` 用に提供される可能性は否定できませんが、`RsvrJdbc` は ORM
 * 機能は搭載しませんし、また少なくとも現在はそういった外付け ORM 機能は提供されていません)
 * 
 * <h2>RsvrJdbc はライブラリとして軽量なだけではなく 実行時の速度も (相対的に) 高速</h2>
 * 
 * `RsvrJdbc` は軽量なライブラリとして実装されており、見通しが良いばかりでなく、実行時の速度も (相対的に)
 * 高速、あるいはコーディングの工夫により速度改善の工夫を実施しやすいライブラリです。これは `RsvrJdbc` が生 JDBC API
 * プログラミングとほぼほぼ等価かつ透過であるために手に入るメリットです。`SQL` を自分で記述する前提になっていることも
 * 性能のチューニングの実施という観点からは有利であるとも考えることは可能でしょう。
 * 
 * <h1>RsvrJdbc の導入方法</h1>
 * 
 * `RsvrJdbc` の導入方法にはいくつかの方法があります。
 * 
 * 最初の簡単な方法は `consulting.reservoir.jdbc`
 * パッケージのファイルすべてを自分のソースコードにコピーしてしまうことです。`RsvrJdbc`
 * は軽量なライブラリであるため、いくつかのファイルを取り回すだけで組み込みが可能です。
 * 
 * （未実装）2つ目の方法は Maven Repository を参照してライブラリとして組み込む方法です。これは一般的な
 * Java開発におけるライブラリ参照方法です。
 * 
 * ## RsvrJdbc 導入が成功していることの確認方法
 * 
 * RsvrJdbc 導入が成功していることをソースコードから確認するには、`import java.sql.PreparedStatement;`
 * の記述がソースコードから無くなり、代わりに `RsvrPreparedStatement`
 * のインポートが記述されていることが確認でき、そしてコンパイルやビルドが成功していれば導入は成功していると考えられます。
 * 
 * <h1>RsvrJdbc の制限</h1>
 * 
 * <h2>スレッドセーフではありません。</h2>
 * 
 * `RsvrJdbc` はスレッドセーフではありません。
 * 
 * <h2>columnLabel を使用するメソッドは 基本的にサポート外</h2>
 * 
 * RsvrResultSet の columnLabel (項目名による値アクセス) をもちいた API 呼び出しについて `RsvrJdbc`
 * はで極力関与することなく 元のResultSet の columnLabel
 * をもちいたメソッド呼び出しに単純割当するように作られています。columnLabel 側の実装には `RsvrJdbc`
 * で提供される追加の機能がありませんので、基本的に columnLabel 側のメソッドは使用せずに columnIndex
 * を使用する側のメソッドの仕様を推奨します。
 * 
 * <h1>RsvrJdbc でわからないことがあったら</h1>
 * 
 * `RsvrJdbc` で不明点があった時にもっとも簡単な解決方法の一つは、`RsvrJdbc` のソースコードを参照することです。`RsvrJdbc`
 * は軽量ライブラリであり、ソースコードの参照は比較的簡単に実現できることでしょう。`RsvrJdbc` のソースコードに現れるソースコードの多くは、JDBC
 * API を直接使用している際に登場するソースコード記述でよく見かけるものであるため読解の助けになることでしょう。
 * 
 * <h2>ソースコードを読むことを推奨</h2>
 * 
 * 当面は `RsvrJdbc` に関するインターネット上などの情報は少なめでしょうから、基本的に `RsvrJdbc`
 * のソースコードを読みつつ使用することを推奨します。平易な構造になっているので、それほど苦労せずに読解できることと期待します。
 * 
 * <h1>感想 / 背景</h1>
 * 
 * 私は Java を用いたソフトウェア開発についての比較的長い経験を持っています。その中で、JDBC API
 * を直接使用した開発、世の中のデータベースアクセス支援ツールの使用、データベースアクセス支援ツールそのものの開発とさまざまな経験をしてきましたが、どれも一長一短で
 * 結局安心して利用できるデータベースアクセス方法は JDBC API の直接使用との結論に至りました。しかしJDBC API の直接使用は
 * JDBCプログラミングにおける `ちいさないらいら` と向き合うことを強いることでもあります。
 * 
 * なんとか JDBC API 直接使用の良さを失うことなく `ちいさないらいら` だけを解決する方法がないものかと考えた挙句に生まれたのが
 * `RsvrJdbc` です。`RsvrJdbc` は JDBC API 直接使用の使い勝手を損なうことなく、`ちいさないらいら`
 * を解決することができます。そして軽量ライブラリであり、ソースコードの見通しもよく、安心して開発に使用することが可能です。そしてもちろん JDBC API
 * の使用経験が `RsvrJdbc` でも大いに役立ちます。
 * 
 * 同じような JDBCプログラミング経験を持つ方の役に立つことを祈ります。
 * 
 * @version 1.0.2.20220901
 */
public class RsvrJdbc {
    /**
     * Javadoc から見えなくするためのコンストラクタ。
     */
    private RsvrJdbc() {
    }

    /**
     * [Rsvr] 与えられた PreparedStatement を入力にラッピングして、RsvrJdbc が提供する
     * RsvrPreparedStatement に切り替えます。
     * 
     * このメソッドの呼び出しが、RsvrJdbc 使用開始の第一歩です。
     * 
     * @param stmt 通常の PreparedStatement
     * @return RsvrJdbc が提供する PreparedStatement に相当する RsvrPreparedStatement
     *         のインスタンス。これを利用することにより RsvrJdbc の機能が利用可能になる。
     */
    public static RsvrPreparedStatement wrap(PreparedStatement stmt) {
        return new RsvrPreparedStatement(stmt);
    }

    /**
     * [Rsvr] 与えられた ResultSet を入力にラッピングして、RsvrJdbc が提供する RsvrResultSet に切り替えます。
     * 
     * 通常は、これを直接使用することは稀で、大抵は RsvrPreparedStatement のメソッドからインスタンス生成されるルートを通ります。
     * 
     * @param rset 通常の ResultSet
     * @return RsvrJdbc が提供する ResultSet に相当する RsvrResultSet のインスタンス。これを利用することにより
     *         RsvrJdbc の機能が利用可能になる。
     */
    public static RsvrResultSet wrap(ResultSet rset) {
        return new RsvrResultSet(rset);
    }

    /**
     * [Rsvr] 与えられた ResultSetMetaData を入力にラッピングして、RsvrJdbc が提供する
     * RsvrResultSetMetaData に切り替えます。
     * 
     * 通常は、これを直接使用することは稀で、大抵は RsvrPreparedStatement や ResvrResultSet
     * のメソッドからインスタンス生成されるルートを通ります。
     * 
     * @param meta 通常の ResultSetMetaData
     * @return RsvrJdbc が提供する ResultSetMetaData に相当する RsvrResultSetMetaData
     *         のインスタンス。これを利用することにより RsvrJdbc の機能が利用可能になる。
     */
    public static RsvrResultSetMetaData wrap(ResultSetMetaData meta) {
        return new RsvrResultSetMetaData(meta);
    }
}
