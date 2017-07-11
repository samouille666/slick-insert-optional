package com.affinytix.config.common

import com.affinytix.config.tracking.scapping.TrackingScrappingTables
import slick.jdbc.JdbcProfile

/**
  * Created by sam on 7/3/17.
  */

final case class TrackingScrappingSchema(val profile: JdbcProfile)
  extends TrackingScrappingTables
    with ProfileJdbc