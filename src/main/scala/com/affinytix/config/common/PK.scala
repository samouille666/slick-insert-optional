package com.affinytix.config.common

import slick.lifted.MappedTo

/**
  * Created by sam on 7/3/17.
  */
// allows type safe usage of ids in table (useful to detect at compile time
// erroneous comparison between ids of different tables)
case class PK[A](value: Long) extends AnyVal with MappedTo[Long]
