package com.affinytix.config.test

import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory

/**
  * Created by sam on 7/6/17.
  */
class TestDataGenerator extends FlatSpec {

  private final val logger = LoggerFactory.getLogger(this.getClass)

  "technical-event.csv" should "" in {
    import DataGenerator._
    val source = getSource(readResource("technical-event.csv"))
    val res = loopOnSource(source)(parseTechnicalEvent(_)())

    assert(res.length == 193)
    res.foreach(t => logger.info(s"${t}"))

    assert(res.filter(xs => xs._3.isDefined).length == 0)
  }


}
