# `RsvrJdbc` の機能

## 機能一覧

`RsvrJdbc` には以下のような機能が含まれます。

| 種別   | 機能名                | 説明 |
| ---   | ---                  | --- |
| 機能   | ResultSet の columnIndex 指定を省略する機能 | RsvrResultSet に columnIndex を内包させることにより、プログラマーは columnIndex 指定から解放されます。 |
| 機能   | RsvrResultSet が内包する columnIndex の値をリセットする機能 | RsvrResultSet に内包された columnIndex の内容はリセットされるタイミングが必要です。absolute, afterLast, beforeFirst, close, first, last, next, previous, relative 呼び出し時に columnIndex をリセットします。 |
| 機能   | PreparedStatement の parameterIndex 指定を省略する機能 | RsvrPreparedStatement に parameterIndex を内包させることにより、プログラマーは parameterIndex 指定から解放されます。 |
| 機能   | PreparedStatement の executeUpdate の実行結果が 1 件であることを確認する機能 | executeUpdateSingleRow という実行結果が1件であったことを確認するメソッドを新たに追加。 |
| 機能   | PreparedStatement が内包する parameterIndex の値をリセットする機能 | RsvrPreparedStatement に内包された parameterIndex の内容はリセットされるタイミングが必要です。clearParameters, close, execute, executeLargeUpdate, executeQuery, executeUpdate 呼び出し時に parameterIndex をリセットします。 |
| 機能   | PreparedStatement の setXXXXX に null 対応を追加 | 通常の JDBC プログラミングでは 例えば setInt に null を与えたい場合は別のメソッド setNull を呼び出す必要があるが、これを不要とする機能。 |
| 機能   | ResultSet の getXXXXX に null 対応を追加 | 通常の JDBC プログラミングでは getInt に null が得られたかどうかを確認したい場合は別のメソッド getNull を呼び出す必要があるが、これを不要とする機能。 |
| 機能   | PreparedStatement の日時設定、および ResultSet の日時取得に java.util.Date 対応を追加 | 専用のメソッド setJavaUtilDate、getJavaUtilDate を追加することにより、使い慣れた java.util.Date を使って日時の設定・取得を可能とした。 |
| 機能   | ResultSetMetaData の columnIndex 指定を省略する機能 | RsvrResultSetMetaData に columnIndex を内包させることにより、プログラマーは columnIndex 指定から解放されます。 |

## 制限

- `RsvrJdbc` が提供する機能はスレッドセーフではありません。
- columnLabel を使用するメソッドは 基本的にサポート外です。メソッド呼び出しをそのまま もとの PreparedStatement に割り当てするような構造になっています。

