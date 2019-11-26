package com.datastax

import java.sql.{Connection, DriverManager, ResultSet}
import com.datastax.BuildCompare.dseInsert

/**
  * A Scala JDBC connection example by Alvin Alexander, adapted by James Colvin
  * https://alvinalexander.com
  */
class jdbcSession(contactPoint: String, username: String, password: String, connect: Boolean) {
  var connection: Connection = _

  if (connect) {
    Class.forName("oracle.jdbc.driver.OracleDriver")
    connection = DriverManager.getConnection(contactPoint, username, password)
  }

  def insertValue(dseObject: dseInsert): Unit = {
    try {
      val statement = connection.createStatement()
      statement.executeUpdate("INSERT INTO results " +
        s"VALUES (${dseObject.build},'${dseObject.metric}','${dseObject.kind}',${dseObject.time},${dseObject.value}))"
      )
    } catch { case e: Exception => println(e) }
  }

  def selectBaselineBuild(buildNbr: Int, metric: String, kind: String): ResultSet = {
    val statement = connection.createStatement()
    statement.executeQuery("SELECT value FROM results " +
      s"WHERE build_nbr = $buildNbr AND metric = $metric AND kind = $kind"
    )
  }

  def closeSession(): Unit = connection.close()
}