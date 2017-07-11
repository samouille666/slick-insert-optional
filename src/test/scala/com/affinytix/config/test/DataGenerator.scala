package com.affinytix.config.test

import java.io.InputStream

import org.slf4j.LoggerFactory

import scala.io.Source

/**
  * Created by samuel on 7/6/17.
  */
object DataGenerator {

  private final val logger = LoggerFactory.getLogger(this.getClass)

  def getSource(is: InputStream): Source = Source.fromInputStream(is)

  def readResource(resourceName: String): InputStream =
    getClass.getClassLoader.getResourceAsStream(resourceName)

  def loopOnSource[T](resource: Source)(f: String => T): Seq[T] = {
    def loop(r: Source) = r.getLines.toList.map(f(_))

    using(resource)(loop)
  }

  def using[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }

  def id[T](x: T) = x

  def cast[T](x: String) = x.asInstanceOf[T]

  def lQuotes(s: String)(f: String => String = id[String]) = f(s).replaceAll("^\"*", "")

  def rQuotes(s: String)(f: String => String = id[String]) = f(s).replaceAll("\"*$", "")

  def lex[T](s: String)(f: String => String = id[String]) = f(s)

  def isNull(p: String) = p.toUpperCase == "NULL"

  def opt[T](datum: String)(f: String => T = id[String] _): Option[T] = datum match {
    case "None" => None
    case "NULL" => None
    case something => Some(f(something))
  }

  def simpleStr(s: String) = lQuotes(s)(rQuotes(_)())

  def optSimpStr(s: String) = opt(s)(simpleStr)

  def optInt(s: String) = opt[Int](s)((x: String) => x.toInt)

  def optLong(s: String) = opt[Long](s)((x: String) => x.toLong)

  def optTuple[T1, T2](s1: String, s2: String)
                      (f1: String => Option[T1])
                      (f2: String => Option[T2]): Option[(Option[T1], Option[T2])] = {
    val v1 = f1(s1)
    val v2 = f2(s2)
    (v1, v2) match {
      case (None, None) => None
      case (None, Some(_)) => None
      case (Some(_), None) => None
      case (Some(_), Some(_)) => Some((v1, v2))
    }
  }

  def optLongString(s1: String, s2: String) = optTuple(s1, s2)(optLong)(optSimpStr)

  type StrSplit = String => Seq[String]

  type BusinessDataMappingRaw = (String, String, Option[String], Option[String], Option[String], Option[Int])
  type ProfileDesc = Option[(Option[Long], Option[String])]
  type BDType = String
  type UALabel = String
  type BDMRaw = (BusinessDataMappingRaw, BDType, ProfileDesc, UALabel)

  def parseBusinessDataMapping(line: String)(f: StrSplit = _.split('\t')): (BDMRaw) = {
    val xs = f(line)

    if (xs.length != 10)
      throw new RuntimeException(s"The splitted line must be 10 fields, found [${xs.length}]")

    logger.debug(s"${xs}")
    logger.debug(s"${(0 until xs.length).map(i => (i, xs(i))).mkString("[", ",", "]")}")

    (
      (simpleStr(xs.head), simpleStr(xs(1)), optSimpStr(xs(2)),
        optSimpStr(xs(3)), optSimpStr(xs(4)), optInt(xs(5))),
      simpleStr(xs(6)),
      optLongString(xs(7), xs(8)),
      simpleStr(xs(9))
    )
  }

  // techEvt.name, evtType.name, pageType.name, Optional userAction.label
  type TechEvtRaw = (String, String, String, Option[String])
  type TechEvtAndDepRaw = (TechEvtRaw, Option[TechEvtRaw], Option[Long])

  def parseTechnicalEvent(line: String)(f: StrSplit = _.split('\t')): TechEvtAndDepRaw = {
    val xs = f(line)

    if (xs.length != 9)
      throw new RuntimeException(s"The splitted line must be 9 fields, found [${xs.length}]")

    def optTechEvnt(p1: String, p2: String, p3: String, p4: String): Option[TechEvtRaw] =
      if (isNull(p1) || isNull(p2) || isNull(p3)) None
      else Some((simpleStr(p1), simpleStr(p2), simpleStr(p3), optSimpStr(p4)))

    ((simpleStr(xs.head), simpleStr(xs(1)), simpleStr(xs(2)), optSimpStr(xs(3))),
      optTechEvnt(xs(4), xs(5), xs(6), xs(7)),
      optLong(xs(8)))

  }


}
