package com.datastax

import java.io.{BufferedWriter, File, FileWriter}
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import net.liftweb.json.{DefaultFormats, parse}
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object BuildCompare extends App {
  val config      = ConfigFactory.load()
//  val config      = ConfigFactory.parseFileAnySyntax(new File("./src/main/resources/stdevcheck.json"))
  val grafana     = new httpClient
  val bMath       = new BuildMath
  val XmlBuilder  = new jUnitXmlBuilder
  val usejdbc     = config.getBoolean("jdbc.usejdbc")
  val jdbc        = new jdbcSession(config.getString("jdbc.host"),config.getString("jdbc.username"),config.getString("jdbc.password"),usejdbc)
  var metricArray = ArrayBuffer[BigDecimal]()

  case class dseInsert(build: Int, time: Long, metric: String, kind: String, value: BigDecimal)
  implicit val formats: DefaultFormats.type = DefaultFormats
  def longToTimestamp(timeInMillis: BigInt)= timeInMillis.toLong * 1000L
  def runBuildCompare(startTime:Long,endTime:Long,baseline:Int,tolerance:Float,dseHost:String,grafanaHost:String,buildId:Int): Unit = {
    val queries = XmlBuilder.buildQueryObjects()
    val build   = buildId
    val dse     = new dseSession(new InetSocketAddress(dseHost,config.getInt("dse.port")),!usejdbc)
    for (i <- queries.indices) {
      //Query Prometheus and create result set
      val json = parse(
        grafana.get(s"${config.getString("grafana.scheme")}" +
          s"$grafanaHost" + ":" +
          s"${config.getInt("grafana.port").toString}" +
          s"${config.getString("grafana.path")}" +
          s"?query=${queries(i).getQuery}" +
          s"&start=${startTime.toString}" +
          s"&end=${endTime.toString}" +
          s"&step=${config.getInt("grafana.step").toString}"))
      val value = if ((json \\ "values").values.nonEmpty) (json \\ "values").values else Map.empty[String,Any]
      val results = value("values").asInstanceOf[List[List[Any]]]

      //Insert individual metrics into C* or JDBC from Prometheus
      for (r <- results) {
        println(r(1).asInstanceOf[String])

        Try {
          metricArray += BigDecimal(r(1).asInstanceOf[String])

          val dseObject = dseInsert(
            build, longToTimestamp(r.head.asInstanceOf[BigInt]), queries(i).getTestCase,
            "value", BigDecimal(r(1).asInstanceOf[String]).doubleValue())

          if (usejdbc) jdbc.insertValue(dseObject) else dse.insertValue(dseObject)
        }
      }

      //Insert mean and stdev values into C* or JDBC
      val meanAndStdDev: (BigDecimal,BigDecimal) = bMath.meanStd(metricArray)
      val insertMeanObject = dseInsert(build, longToTimestamp(endTime),
        queries(i).getTestCase, "mean", meanAndStdDev._1.doubleValue())
      val insertStdevObject = dseInsert(build, longToTimestamp(endTime),
        queries(i).getTestCase, "stdev", meanAndStdDev._2.doubleValue())

      if (usejdbc) jdbc.insertValue(insertMeanObject) else dse.insertValue(insertMeanObject)
      if (usejdbc) jdbc.insertValue(insertStdevObject) else dse.insertValue(insertStdevObject)

      //Select baseline mean and stdev from C* or JDBC
      var baselineBuildMean: Double = 0.00
      var baselineBuildStdev: Double = 0.00
      try {
        baselineBuildMean = if (usejdbc) jdbc.selectBaselineBuild(baseline, queries(i).getTestCase, "mean").getDouble(1)
        else dse.selectBaselineBuild(baseline, queries(i).getTestCase,"mean").one().getDouble("value")
      } catch { case e: Exception => println("Error retrieving baseline metric "+e.toString) }

      try {
        baselineBuildStdev = if (usejdbc) jdbc.selectBaselineBuild(baseline, queries(i).getTestCase, "stdev").getDouble(1)
        else dse.selectBaselineBuild(baseline, queries(i).getTestCase,"stdev").one().getDouble("value")
      } catch {
        case e: Exception => println("Error retrieving baseline metric "+e.toString)
      }

      //Check whether the percentage of values in `metricArray` that are above or below stdev are within tolerance
      val stdevCheck = bMath.stdevCheck(
        baselineBuildStdev, baselineBuildMean, metricArray,
        tolerance, queries(i).getAboveStdev)

      //Prepare the XML document with results from the stdev check
      queries(i).failure = stdevCheck("failure").asInstanceOf[Boolean]
      queries(i).value = stdevCheck("value").asInstanceOf[Float]

      //Write the XML document to a file
      val xmlFile = new File(config.getString("jenkins.xmlfilepath")+queries(i).getTestCase.filterNot((x: Char)=>x.isWhitespace)+".xml")
      val bw = new BufferedWriter(new FileWriter(xmlFile,false))
      bw.write(queries(i).toXml().toString())
      bw.close()

      //Clear metrics before looping again
      metricArray.clear()
    }

    if (usejdbc) jdbc.closeSession() else dse.closeSession()
    if (usejdbc && jdbc.connection.isClosed) System.exit(0)
    else if (!usejdbc && dse.session.isClosed) System.exit(0)
    else System.exit(1)
  }
}
