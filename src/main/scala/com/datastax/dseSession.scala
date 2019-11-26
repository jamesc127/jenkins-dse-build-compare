package com.datastax

import java.net.InetSocketAddress
import com.datastax.driver.core.ResultSet
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.dse.{DseCluster, DseSession}
import com.datastax.BuildCompare.dseInsert

class dseSession(contactPoint: InetSocketAddress, connect: Boolean) {
  var cluster: DseCluster = _
  var session: DseSession = _

  if (connect) {
    cluster = DseCluster.builder().addContactPoint(contactPoint.getHostString).build()
    session = cluster.connect()
  }

  def closeSession(): Unit = session.close()

  def insertValue(dseObject: dseInsert ): Unit = {
    try{
      session.execute(s"INSERT INTO grafana.results (build_nbr,stamp_time,metric,kind,value) " +
        s"VALUES (${dseObject.build},${dseObject.time},'${dseObject.metric}','${dseObject.kind}',${dseObject.value})")
      println(s"VALUES (${dseObject.build},${dseObject.time},'${dseObject.metric}','${dseObject.kind}',${dseObject.value})")
    } catch { case e: Exception => println(e) }
  }

  def selectBaselineBuild(buildNbr: Int, metric: String, kind: String): ResultSet = {
      session.execute(QueryBuilder
        .select().all().from("grafana","results")
        .where(QueryBuilder.eq("build_nbr",buildNbr))
          .and(QueryBuilder.eq("metric",metric))
          .and(QueryBuilder.eq("kind",kind))
      )
  }
}