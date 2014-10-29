package com.dbschools.mgb.schema

import java.sql.Timestamp

import org.squeryl.Schema
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.DateExpression
import org.squeryl.dsl.ast.{TokenExpressionNode, FunctionNode}
import org.squeryl.internals.OutMapper

object AppSchema extends Schema {
  val users               = table[User]               ("music_user")
  val musicians           = table[Musician]           ("musician")
  val groups              = table[Group]              ("music_group")
  val groupTerms          = table[GroupTerm]
  on(groupTerms)(gt => declare(columns(gt.groupId, gt.term) are (unique, indexed("GroupTerm_groupId_term"))))
  val pieces              = table[Piece]              ("piece")
  val musicianGroups      = table[MusicianGroup]      ("musician_group")
  on(musicianGroups)(t => declare(t.musician_id is indexed("musician_group_musician_id")))
  on(musicianGroups)(mg => declare(columns(mg.musician_id, mg.school_year, mg.group_id) are (
    unique, indexed("musician_group_unique"))))
  val instruments         = table[Instrument]         ("instrument")
  val subinstruments      = table[Subinstrument]      ("subinstrument")
  val assessments         = table[Assessment]         ("assessment")
  on(assessments)(a => declare(
    a.assessment_time is indexed("assessments_assessment_time"),
    a.musician_id is indexed("assessments_musician_id")
  ))
  val assessmentTags      = table[AssessmentTag]      ("assessment_tag")
  val predefinedComments  = table[PredefinedComment]  ("predefined_comment")
  val rejectionReasons    = table[RejectionReason]    ("rejection_reason")
  val tempos              = table[Tempo]              ("tempo")

  val groupToGroupTerms   = oneToManyRelation(groups, groupTerms).via((g, t) => g.id === t.groupId)

  class DateTrunc(
    span: String,
    e:    DateExpression[Timestamp],
    m:    OutMapper[Timestamp])
  extends FunctionNode[Timestamp]("date_trunc", Some(m), Seq(new TokenExpressionNode("'"+span+"'"), e))
  with DateExpression[Timestamp]

  def dateTrunc(span: String, e: DateExpression[Timestamp])(implicit m: OutMapper[Timestamp]) = new DateTrunc(span,e,m)
}
