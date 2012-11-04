package com.dbschools.mgb.schema

import org.squeryl.Schema

object AppSchema extends Schema {
  val users           = table[User]         ("music_user")
  val musicians       = table[Musician]     ("musician")
  val groups          = table[Group]        ("music_group")
  val pieces          = table[Piece]        ("piece")
  val musicianGroups  = table[MusicianGroup]("musician_group")
  val instruments     = table[Instrument]   ("instrument")
  val subinstruments  = table[Subinstrument]("subinstrument")
  val assessments     = table[Assessment]   ("assessment")
  val assessmentTags  = table[AssessmentTag]("assessment_tag")
}
