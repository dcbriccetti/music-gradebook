package com.dbschools.mgb.model

import scalaz._
import Scalaz._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import net.liftweb.util.Props

/**
  * School terms (e.g., trimesters)
  * @param opTermDates the dates of the terms, in ISO8601 format, or None if the terms
  *                    are to be read from the properties file.
  */
class Terms(opTermDates: Option[Iterable[String]]) {
  /** Term start dates, ordered from most recent to least */
  private val startDatesDesc: Seq[DateTime] = {
    val parser = ISODateTimeFormat.dateTimeParser()
    (opTermDates |
      Props.get("terms").openOrThrowException("terms property missing").split(',')).
      map(parser.parseDateTime).toSeq.sortBy(_.getMillis).reverse
  }

  def current: DateTime = startDatesDesc.find(_.getMillis < DateTime.now.getMillis).get

  def yearStart: DateTime = startDatesDesc.last

  def highestDateTermBefore(time: DateTime): Option[DateTime] = startDatesDesc.find(_.getMillis < time.getMillis)
}
