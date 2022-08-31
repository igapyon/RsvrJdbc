# About RsvrJdbc (version 1.0.2.20220901)

`RsvrJdbc` は、JDBCプログラミングにおける `ちいさないらいら` を解消することを目的に作成した軽量ライブラリです。
JDBCプログラミング体験をそのまま引き続き活用できることを原則に、`RsvrJdbc` における小さな変更点を知るだけで JDBCプログラミング経験者がすぐに利用開始できて、そして `ちいさないらいら` を解消できることを目指して `RsvrJdbc` は作られています。

## Usage: 検索

まずは `RsvrJdbc` の基本的な使い方から見てみましょう。
最初に検索の方法から見ていきます。以下が `RsvrJdbc` におけるシンプルな検索のソースコード例です。
このソースコード断片では `conn.prepareStatement()` の結果を `RsvrJdbc#wrap` メソッドで受けることにより、PreparedStatement に相当する `RsvrPreparedStatement` インスタンスを入手しています。

得られた `RsvrPreparedStatement` インスタンスの `executeQuery()` メソッドを呼び出すと、ResultSet に相当する `RsvrResultSet` インスタンスを入手できます。

```java
try (RsvrPreparedStatement stmt = RsvrJdbc.wrap(conn.prepareStatement(
        "SELECT IssueId, Summary, IssueType, Priority, Description FROM MyTable ORDER BY KeyId" ))) {
    try (RsvrResultSet rset = stmt.executeQuery()) {
        for (; rset.next();) {
            Long origIssueId = rset.getLong();
            String summary = rset.getString();
            String issueType = rset.getString();
            String priority = rset.getString();
            String description = rset.getString();
```

ここで `RsvrResultSet` のメソッドの引数に注目しましょう。
`RsvrResultSet` は JDBC API で使用可能な getString や getLong を利用することができますが、通常の JDBC プログラミングでは指定の必要がある columnIndex 引数の指定が不要になっています。

実は、通常の JDBC プログラミングで この columnIndex 引数の指定は `ちいさないらいら` です。ほとんどの場合は 1 から順番に 2, 3, 4 と順に増やしながら引数指定します。この定型的な指定は単純に面倒なうえに、SQL途中への項目追加などの仕様変更の際におけるバグの作り込み原因にもなりがちです。

`RsvrJdbc` では、この columnIndex の指定を省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？ 

でも待ってください。columnIndex の指定が省略されるとして、この値はいつリセットされるのでしょうか。大丈夫です。この次の説明で `RsvrResultSet` 内部の columnIndex のリセットタイミングを知ることができます。

## RsvrResultSet 内部の columnIndex のリセットタイミング

`RsvrJdbc` では、以下のメソッド呼び出しのタイミングで `RsvrResultSet` 内部の columnIndex のリセットを実施します。

- RsvrResultSet#absolute
- RsvrResultSet#afterLast
- RsvrResultSet#beforeFirst
- RsvrResultSet#close
- RsvrResultSet#first
- RsvrResultSet#last
- RsvrResultSet#next
- RsvrResultSet#previous
- RsvrResultSet#relative

この仕様決定は、たいていの一般的な JDBCプログラミングにおけるプログラミングパターンから導出されました。つまり、大抵の JDBC プログラミングでは、参照レコードを進めたり戻したりする際に columnIndex を1へとリセットすることが観察されたことから、上記の仕様が導出されました。

もちろん、RsvrResultSet#resetColumnIndex メソッドという columnIndex のリセットを目的としたメソッドを明示的に呼び出すことによっても columnIndex のリセットを行うことが可能です。

## Usage: 更新 (追加と削除も同様)

引き続き `RsvrJdbc` の基本的な使い方を見てみましょう。以下が更新のシンプルなソースコード例です。
conn.prepareStatement() の結果を RsvrJdbc#wrap で受けることにより、PreparedStatement に相当する `RsvrPreparedStatement` をインスタンスを入手できます。

```java
try (RsvrPreparedStatement stmtMod = RsvrJdbc.wrap(conn.prepareStatement("UPDATE MyTable SET IssueKey=?, Summary=?, KeyId=?, ProjectId=? WHERE IssueId = ?"))) {
    stmtMod.setString(source.getIssueKey());
    stmtMod.setString(source.getSummary());
    stmtMod.setLong(source.getKeyId());
    stmtMod.setLong(source.getProjectId());
    stmtMod.setLong(source.getId());
    stmtMod.executeUpdateSingleRow();
```

ここで `RsvrPreparedStatement` のメソッドの引数に注目しましょう。
`RsvrPreparedStatement` は JDBC API で使用可能な setString や setLong を利用することができますが、通常の JDBC プログラミングでは指定する必要のある parameterIndex 引数の指定が不要になっています。

通常の JDBC プログラミングで この parameterIndex 引数は `ちいさないらいら` です。ほとんどの場合は 1 から順番に 2, 3, 4 と順に増やしながら引数指定します。この定型的な指定は単純で面倒なうえに、SQL途中への項目追加などの仕様変更の際におけるバグの作り込みの原因にもなりがちです。

`RsvrJdbc` では、この parameterIndex の指定を省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？ 

また、`RsvrPreparedStatement#executeUpdateSingleRow` という見慣れないメソッドがあります。これは `RsvrJdbc` で独自に追加されたメソッドです。比較的多くの UPDATE 文では 1行のみレコードを更新することが知られています。そして 1行のみ更新されたかどうかの確認は、executeUpdate を呼び出した後の戻り値が 1 であることを確認することにより担保できます。`RsvrJdbc` においても、そのような一般的な使用方法も可能ですが、ここで提供する executeUpdateSingleRow を使用することで実行結果が1行であったことを担保できるようになり、コーディングの簡素化を実現できます。

`RsvrPreparedStatement#executeUpdateSingleRow` では、内部で executeUpdate を呼び出して戻り値が1であることを確認します。仮に 1 ではなかった時には SQLException を throw するように作られています。これにより、いつもの `ちいさないらいら` の UPDATE 文の実行結果が1件であることの確認ソースコードの記述不要を実現しているのです。

`RsvrJdbc` では、この executeUpdate の実行結果件数の確認を省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？ 

でも待ってください。parameterIndex の指定が省略されるとして、この値はいつリセットされるのでしょうか。大丈夫です。この次の説明で RsvrPreparedStatement 内部の parameterIndex のリセットタイミングを知ることができます。

## RsvrPreparedStatement 内部の parameterIndex のリセットタイミング

以下のメソッド呼び出しのタイミングで RsvrPreparedStatement 内部の parameterIndex のリセットを実施します。

- RsvrPreparedStatement#clearParameters
- RsvrPreparedStatement#close
- RsvrPreparedStatement#execute
- RsvrPreparedStatement#executeLargeUpdate
- RsvrPreparedStatement#executeQuery
- RsvrPreparedStatement#executeUpdate

この仕様決定は、たいていの一般的な JDBCプログラミングにおけるプログラミングパターンから導出されました。つまり、大抵の JDBC プログラミングでは ステートメントの実行時や clearParameters メソッドを呼び出す際に parameterIndex を1へとリセットすることが観察されたことからも、上記のような仕様が導出されました。

もちろん、RsvrPreparedStatement#resetParameterIndex メソッドという parameterIndex のリセットを目的としたメソッドを明示的に呼び出すことによっても parameterIndex のリセットを行うことが可能です。

## null との格闘

JDBCプログラミングにおける `ちいさないらいら` のひとつに null との格闘があります。ご承知のように JDBC API ではいくつかのデータ型においてプリミティブ型が使用されます。Javaではプリミティブ型は null を扱うことができないために、JDBC API での読み書きにおいては、null の場合は別途 ResultSet#wasNull や PreparedStatement#setNull を使用する必要があります。さらに加えて PreparedStatement#setNull の第2引数 sqlType を指定することも面倒なことです。

`RsvrJdbc` では、この null との格闘を解決するために、プリミティブ型で提供されるメソッド引数や戻り値のいくつかを プリミティブ型の値をラップするクラスの引数や戻り値で置き換えています。

例えば、PreparedStatement#setInt の引数は int ですが、RsvrPreparedStatement#setInt の引数では Integer に置き換えられています。この箇所は `RsvrJdbc` が明示的に仕様を調整している重要な変更箇所の一つです。また、この変更によって、null との格闘のいらいらの解消が実現できるのです。

RsvrPreparedStatement#setInt はメソッド内の処理で、引数に null が与えられたら値に null をセットするための setNull メソッドを呼び出し、それ以外の場合は通常の setInt を呼び出して値を設定するように分岐しています。

同様に ResultSet#getInt の戻り値は int ですが、RsvrResultSet#getInt の戻り値は Integer に置き換えられています。RsvrResultSet#getInt は内部処理で getInt の呼び出し後に wasNull を呼び出してその結果が true の場合は null を戻し、それ以外の場合は getInt で得られた値を ラッパークラスに相当する java.lang.Integer で戻すように分岐しています。

なお、この null に対応するための機能のために、RsvrResultSet#getXXX メソッドの戻り値が今まではプリミティブ型で null を意識する必要がなかったものが、`RsvrJdbc` では null を意識する必要があるよう変わる点に注意が必要です。RsvrResultSet#getXXX で戻された値は (プリミティブ型ではなく) ラッパークラスで受けた上で、適切に null であるか確認するようコーディングすることを強く推奨します。

このように `RsvrJdbc` では、この null にまつわる面倒ごとを省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？ 

## java.sql.Date、java.sql.Timestamp との格闘

JDBCプログラミングにおける `ちいさないらいら` のひとつに 日付型、日時型 との格闘があります。
ご承知のように JDBC API では `java.sql.Date` や `java.sql.Timestamp` という JDBC用のデータ型を扱います。一方で 一般的な Java プログラミングでは `java.util.Date` や新しい日付型、日時型を使用していることでしょう。
この型の違いを埋める作業が Java JDBC プログラミングの `ちいさないらいら` のひとつであると私は信じています。身近なところとしては、IDEのimport自動編成機能などにおいて型の選択が必要になるという小さい面倒ごとが発生しがちであることも理由のひとつです。

`RsvrJdbc` ではこの不満を解消するために、`RsvrPreparedStatement#setJavaUtilDate` および `ResultSet#getJavaUtilDate` という java.util.Date を入出力するためのメソッドを提供します。
※なお現状では新しい日付型、日時型については未対応です。
これら `RsvrJdbc` が提供するクラスのメソッドを使用することにより、 `java.sql.Date` や `java.sql.Timestamp` という JDBC用のデータ型を見る必要がなくなりました。

※私が使用する範囲の RDB では 内部的に java.sql.Timestamp にマッピングすることによりすべてうまくいきました。しかし RDB の種類によってこれは期待しない挙動を行う可能性があります。その場合には setJavaUtilDate、getJavaUtilDate を使用せずに、java.sql.Timestampを引数や戻り値で使用する他のメソッドを使用するなどワークアラウンドをおこなってください。

`RsvrJdbc` では、この java.sql.Date、 java.sql.Timestamp にまつわる面倒ごとを省略できてしまうのです。どうでしょう、`ちいさないらいら` のひとつが解決できましたか？ 

# JPA や ORM で 既にうまくいっている人には不要

JPA や Hibernate や MyBatis といった　ORM をご利用されていて、それらでうまくいっている人には、`RsvrJdbc` は不要なものです。
世の中のいろいろな Java データベースアクセス手法を試したがいろいろあって絶望し、JDBC API によるプログラムに仕方なく戻ってきた人にとって、`RsvrJdbc` は最適なライブラリとして書かれたものだからです。

そして `RsvrJdbc` は `SQL` を自分で記述する必要があります。さらに `RsvrJdbc` には ORM の機能はありません。(外付けの ORM 機能が `RsvrJdbc` 用に提供される可能性は否定できませんが、`RsvrJdbc` は ORM 機能は搭載しませんし、また少なくとも現在はそういった外付け ORM 機能は提供されていません)

## RsvrJdbc はライブラリとして軽量なだけではなく 実行時の速度も (相対的に) 高速

`RsvrJdbc` は軽量なライブラリとして実装されており、見通しが良いばかりでなく、実行時の速度も (相対的に) 高速、あるいはコーディングの工夫により速度改善の工夫を実施しやすいライブラリです。これは `RsvrJdbc` が生 JDBC API プログラミングとほぼほぼ等価かつ透過であるために手に入るメリットです。`SQL` を自分で記述する前提になっていることも 性能のチューニングの実施という観点からは有利であるとも考えることは可能でしょう。

# RsvrJdbc の導入方法

`RsvrJdbc` の導入方法にはいくつかの方法があります。

最初の簡単な方法は `consulting.reservoir.jdbc` パッケージに含まれるファイルすべてを自分のソースコードにコピーしてしまうことです。`RsvrJdbc` は軽量なライブラリであり他に依存ライブラリを持たないため、このようにいくつかのファイルを取り回すだけで組み込みが可能です。

（未実装）2つ目の方法は Maven Repository を参照してライブラリとして組み込む方法です。これは一般的な Java開発におけるライブラリ参照方法です。

## RsvrJdbc 導入が成功していることの確認方法

RsvrJdbc 導入が成功していることをソースコードから確認するには、`import java.sql.PreparedStatement;` の記述がソースコードから無くなり、代わりに `RsvrPreparedStatement` のインポートが記述されていることが確認でき、そしてコンパイルやビルドが成功していれば導入は成功していると考えられます。

# RsvrJdbc の制限

## スレッドセーフではありません。

`RsvrJdbc` はスレッドセーフではありません。

## columnLabel を使用するメソッドは 基本的にサポート外

RsvrResultSet の columnLabel (項目名による値アクセス) をもちいた API 呼び出しについて `RsvrJdbc` はで極力関与することなく 元のResultSet の columnLabel をもちいたメソッド呼び出しに単純割当するように作られています。columnLabel 側の実装には `RsvrJdbc` で提供される追加の機能がありませんので、基本的に columnLabel 側のメソッドは使用せずに columnIndex を使用する側のメソッドの使用を推奨します。

# RsvrJdbc でわからないことがあったら

`RsvrJdbc` で不明点があった時にもっとも簡単な解決方法の一つは、`RsvrJdbc` のソースコードを参照することです。`RsvrJdbc` は軽量ライブラリであり、ソースコードの参照は比較的簡単に実現できることでしょう。`RsvrJdbc` のソースコードに現れるソースコードの多くは、JDBC API を直接使用している際に登場するソースコード記述でよく見かける程度のものであるため読解の助けになることでしょう。

## ソースコードを読むことを推奨

当面は `RsvrJdbc` に関するインターネット上などの情報は少なめでしょうから、基本的に `RsvrJdbc` のソースコードを読みつつ使用することを推奨します。平易な構造になっているので、それほど苦労せずに読解できることと期待します。

# 感想 / 背景

私は Java を用いたソフトウェア開発についての比較的長い経験を持っています。その中で、JDBC API を直接使用した開発、世の中のデータベースアクセス支援ツールの使用、データベースアクセス支援ツールそのものの開発とさまざまな経験をしてきましたが、どれも一長一短で 結局安心して利用できるデータベースアクセス方法は JDBC API の直接使用との結論に至りました。しかしJDBC API の直接使用は JDBCプログラミングにおける `ちいさないらいら` と向き合うことを強いることでもあります。

なんとか JDBC API 直接使用の良さを失うことなく `ちいさないらいら` だけを解決する方法がないものかと考えた挙句に生まれたのが `RsvrJdbc` です。`RsvrJdbc` は JDBC API 直接使用の使い勝手を損なうことなく、`ちいさないらいら` を解決することができます。そして軽量ライブラリであり、ソースコードの見通しもよく、安心して開発に使用することが可能です。そしてもちろん JDBC API の使用経験が `RsvrJdbc` でも大いに役立ちます。

同じような JDBCプログラミング経験を持つ方の役に立つことを祈ります。
