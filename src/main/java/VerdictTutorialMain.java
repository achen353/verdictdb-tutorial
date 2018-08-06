import com.google.common.base.Stopwatch;

import java.sql.*;
import java.util.concurrent.TimeUnit;

/** Created by Dong Young Yoon on 8/3/18. */
public class VerdictTutorialMain {

  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("USAGE: VerdictTutorialMain <host> <port> <database> <command>");
      System.out.println("Supported command: create, run");
      return;
    }

    String host = args[0];
    String port = args[1];
    String database = args[2];
    String command = args[3];

    Connection conn = null;
    Connection mysqlConn = null;

    try {
      Class.forName("com.mysql.jdbc.Driver");
      conn =
          DriverManager.getConnection(
              String.format(
                  "jdbc:verdict:mysql://%s:%s/%s?" + "autoReconnect=true&useSSL=false",
                  host, port, database),
              "root",
              "");
      mysqlConn =
          DriverManager.getConnection(
              String.format("jdbc:mysql://%s:%s/%s", host, port, database), "root", "");
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    try {
      switch (command.toLowerCase()) {
        case "create":
          createScrambleTable(conn, mysqlConn, database);
          break;
        case "run":
          runQuery(conn, mysqlConn, database);
          break;
        default:
          System.out.println("Unsupported command: " + command);
          System.out.println("Supported command: create, run");
          return;
      }
      conn.close();
      mysqlConn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    System.exit(0);
  }

  private static void runQuery(Connection verdictConn, Connection mysqlConn, String database)
      throws SQLException {
    mysqlConn.createStatement().execute("SET GLOBAL query_cache_size=0");
    Stopwatch watch = new Stopwatch();
    watch.start();
    ResultSet rs1 =
        mysqlConn.createStatement()
            .executeQuery(String.format("SELECT avg(l_extendedprice) FROM %s.lineitem", database));
    watch.stop();
    if (rs1.next()) {
      System.out.println("Wihtout VerdictDB: average(l_extendedprice) = " + rs1.getDouble(1));
    }
    long time = watch.elapsedTime(TimeUnit.SECONDS);
    System.out.println("Time Taken = " + time + " s");
    rs1.close();

    watch.start();
    ResultSet rs2 =
        verdictConn.createStatement()
            .executeQuery(String.format("SELECT avg(l_extendedprice) FROM %s.lineitem", database));
    watch.stop();
    if (rs2.next()) {
      System.out.println("Wiht VerdictDB: average(l_extendedprice) = " + rs2.getDouble(1));
    }
    time = watch.elapsedTime(TimeUnit.SECONDS);
    System.out.println("Time Taken = " + time + " s");
    rs2.close();
  }

  private static void createScrambleTable(
      Connection verdictConn, Connection mysqlConn, String database) throws SQLException {
    Statement stmt = verdictConn.createStatement();
    String dropQuery = String.format("DROP TABLE IF EXISTS %s.lineitem_scramble", database);
    String createQuery =
        String.format(
            "CREATE SCRAMBLE %s.lineitem_scramble " + "FROM %s.lineitem",
            database, database);
    mysqlConn.createStatement().execute(dropQuery);
    System.out.println("Creating a scrambled table for lineitem...");
    Stopwatch watch = new Stopwatch();
    watch.start();
    stmt.execute(createQuery);
    stmt.close();
    watch.stop();
    System.out.println("Scrambled table for lineitem has been created.");
    long time = watch.elapsedTime(TimeUnit.SECONDS);
    System.out.println("Time Taken = " + time + " s");
  }
}
