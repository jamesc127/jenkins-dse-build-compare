package com.datastax

import scala.xml.Elem

class QueryStructure(testsuite: String, testcase: String, query: String, metric: String, successtext: String,
                     failtype: String, failtext: String, abovestdev: Boolean, var value: Float, var failure: Boolean) {

  def toXml(): Elem = {
    if (failure) {
      <testsuite name={testsuite} tests="1" errors="0" failures="1" time="0">
        <testcase name={testcase} status="false" time="0">
          <failure type={failtype}>{metric} {failtext} {value.toString}</failure>
        </testcase>
      </testsuite>
    } else {
      <testsuite name={testsuite} tests="1" errors="0" failures="0" time="0">
        <testcase name={testcase} status="true" time="0">
          <system-out>{metric} {successtext} {value.toString}</system-out>
        </testcase>
      </testsuite>
    }
  }

  def getQuery: String = query
  def getAboveStdev: Boolean = abovestdev
  def getMetric: String = metric
  def getTestCase: String = testcase

  override def toString: String = {
    if (failure){
      s"testsuite: $testsuite, testcase: $testcase, metric: $metric, failtext: $failtext, value: $value, failtype: $failtype"
    } else {
      s"testsuite: $testsuite, testcase: $testcase, metric: $metric, successtext: $successtext, value: $value"
    }
  }
}