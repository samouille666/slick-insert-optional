package com.affinytix.config.common

/**
  * Created by sam on 7/3/17.
  *
  * Allows to abstract the profile use to connect to a concrete DB and mixin
  * to any other trait that describe a schema.
  */
trait ProfileJdbc {
  val profile: slick.jdbc.JdbcProfile
}
