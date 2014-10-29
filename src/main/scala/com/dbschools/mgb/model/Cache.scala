package com.dbschools.mgb.model

import com.dbschools.mgb.schema.{Group, Piece, AppSchema}
import AppSchema.dateTrunc
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.PrimitiveTypeMode.{inTransaction => inT}
import org.joda.time.DateTime
import scalaz._
import Scalaz._
import Terms.{toTs, termStart, currentTerm}

object Cache {
  var groups = readGroups
  var groupTerms = readGroupTerms
  var instruments = readInstruments
  var (subinstruments, subsByInstrument) = readSubinstruments
  var tags = readTags
  var pieces = readPieces
  var tempos = readTempos

  private var _lastAssTimeByMusician = inT(for {
    gm <- from(AppSchema.assessments)(a => groupBy(a.musician_id) compute max(a.assessment_time))
    m <- gm.measures
  } yield gm.key -> new DateTime(m.getTime)).toMap

  def lastAssTimeByMusician = _lastAssTimeByMusician
  def updateLastAssTime(musicianId: Int, time: DateTime): Unit = {
    _lastAssTimeByMusician += musicianId -> time
    updateNumDaysTestedThisYearByMusician(musicianId)
  }

  private var _numPassesThisTermByMusician = inT(for {
    gm <- from(AppSchema.assessments)(a =>
      where(a.pass === true and a.assessment_time > toTs(termStart(currentTerm)))
      groupBy a.musician_id
      compute count(a.assessment_time)
    )
    m = gm.measures
  } yield gm.key -> m.toInt).toMap

  def numPassesThisTermByMusician = _numPassesThisTermByMusician
  def incrementNumPassesThisTermByMusician(musicianId: Int): Unit = {
    _numPassesThisTermByMusician += musicianId -> (_numPassesThisTermByMusician.getOrElse(musicianId, 0) + 1)
  }

  private var _numDaysTestedThisYearByMusician = inT(for {
    gm <- from(AppSchema.assessments)(a =>
      where(a.assessment_time > toTs(termStart(currentTerm)))
      groupBy a.musician_id
      compute countDistinct(dateTrunc("day", a.assessment_time))
    )
    m = gm.measures
  } yield gm.key -> m.toInt).toMap

  def numDaysTestedThisYearByMusician = _numDaysTestedThisYearByMusician
  private def updateNumDaysTestedThisYearByMusician(musicianId: Int): Unit = {
    inT{
      from(AppSchema.assessments)(a =>
        where(a.musician_id === musicianId and a.assessment_time > toTs(termStart(currentTerm)))
        compute countDistinct(dateTrunc("day", a.assessment_time))
      ).headOption.foreach(m => {
        _numDaysTestedThisYearByMusician += musicianId -> m.measures.toInt
      })
    }
  }

  private def readGroups      = inT {AppSchema.groups.toSeq.sortBy(_.name)}
  private def readGroupTerms  = inT {AppSchema.groupTerms.toList}
  private def readInstruments = inT {AppSchema.instruments.toSeq.sortBy(_.sequence.get)}
  private def readSubinstruments = inT {
    val subs = AppSchema.subinstruments.toSeq
    (subs, subs.groupBy(_.instrumentId.get))
  }
  private def readTags        = inT {AppSchema.predefinedComments.toSeq.sortBy(_.commentText)}
  private def readPieces      = inT {AppSchema.pieces.toSeq.sortBy(_.testOrder.get)}
  private def readTempos      = inT {AppSchema.tempos.toSeq.sortBy(_.instrumentId)}

  def init(): Unit = {}

  def invalidateGroups(): Unit = { groups = readGroups }

  def invalidateGroupTerms(): Unit = { groupTerms = readGroupTerms }

  def invalidateInstruments(): Unit = { instruments = readInstruments }

  def invalidateSubinstruments(): Unit = {
    val (a, b) = readSubinstruments
    subinstruments = a
    subsByInstrument = b
  }

  def invalidateTags(): Unit = { tags = readTags }

  def invalidatePieces(): Unit = { pieces = readPieces }

  def invalidateTempos(): Unit = { tempos = readTempos }

  def nextPiece(piece: Piece) = pieces.find(_.testOrder.get.compareTo(piece.testOrder.get) > 0)

  case class GroupPeriod(group: Group, period: Int)
  
  def filteredGroups(opSelectedTerm: Option[Int]) = {
    val groupIdToPeriod = (for {
      gt      <- Cache.groupTerms
      selTerm <- opSelectedTerm
      if gt.term == selTerm
    } yield gt.groupId -> gt.period).toMap

    val unsorted = if (groupIdToPeriod.isEmpty)
      Cache.groups.map(g => GroupPeriod(g, 0))
    else
      for {
        group   <- Cache.groups
        period  <- groupIdToPeriod.get(group.id)
      } yield GroupPeriod(group, period)
    unsorted.toSeq.sortBy(gp => (gp.period, gp.group.shortName | gp.group.name))
  }
}