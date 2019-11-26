package com.datastax

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable.ArrayBuffer

class jUnitXmlBuilder {

  val config: Config = ConfigFactory.load()
//  val config: Config = ConfigFactory.parseFileAnySyntax(new File("./src/main/resources/stdevcheck.json"))
  val testcases: Array[AnyRef] = config.getObjectList("testsuite.testcase").toArray
  var queryStructureArray: ArrayBuffer[QueryStructure] = ArrayBuffer[QueryStructure]()

  def test(): Unit = {
    for ( i <- testcases.indices) {
      println(config.getConfigList("testsuite.testcase").get(i).getString("name"))
    }
  }

  def buildQueryObjects(): ArrayBuffer[QueryStructure] = {
    val caseList = config.getConfigList("testsuite.testcase")
    for (i <- 0 until caseList.size()) {
      queryStructureArray += new QueryStructure(
        config.getString("testsuite.name"),
        caseList.get(i).getString("name"),
        caseList.get(i).getString("query"),
        caseList.get(i).getString("metric"),
        caseList.get(i).getString("successtext"),
        caseList.get(i).getString("failtype"),
        caseList.get(i).getString("failtext"),
        caseList.get(i).getBoolean("abovestdev"),
        0.00F,
        false
      )
    }
    queryStructureArray
  }
}
