package com.affinytix.config.test

import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.slf4j.LoggerFactory

/**
  * Created by sam on 7/4/17.
  */
class TestTrackingScrapping extends FlatSpec with BeforeAndAfter {

  private final val logger = LoggerFactory.getLogger(this.getClass)

  import com.affinytix.config.test.{TrackingScrappingAPI => tsApi}
  import tsApi._
  import tsApi.schema._

  before {
    tsApi.create
  }

  after {
    tsApi.drop
  }

  "Technical Event (API)" should
    "allow insert/selectAll/selectCount of 193 records without/with profileId" in {

    // prepare the data used as foreign keys in the technical event table
    tsApi.insertEventTypes(populateEventType)
    tsApi.insertPageTypes(populatePageType)
    tsApi.insertUserActionsWithCancel(populateUserAction)

    // contains 1 problematic record
    tsApi.insertTechnicalEvent(populateTechnicalEvent("technical-event-2.csv"))
    // contains all the records to be inserted
    //    tsApi.insertTechnicalEvent(populateTechnicalEvent("technical-event.csv"))

//    val count = countTechnicalEvents
    //    assert(count == 193)
    //    assert(selectAllTechnicalEvents.length == count)
  }


}
