package com.dbschools.mgb
package snippet

import xml.Text
import scalaz._
import Scalaz._
import org.squeryl.PrimitiveTypeMode._
import net.liftweb._
import common.{Full, Loggable}
import util._
import http._
import js._
import js.JE.JsRaw
import js.JsCmds._
import Helpers.asInt
import js.JsCmds.Confirm
import model._
import model.GroupAssignment
import schema.{AppSchema, Musician, Assessment, MusicianGroup}

class StudentDetails extends Loggable {
  private var selectedMusicianGroups = Map[Int, MusicianGroup]()
  private val reloadPage = JsRaw("location.reload()")

  def render = {
    val groups = AppSchema.groups.toSeq.sortBy(_.name)
    val instruments = AppSchema.instruments.toSeq.sortBy(_.sequence.is)

    case class MusicianDetails(musician: Musician, groups: Iterable[GroupAssignment], assessments: Iterable[Assessment])
    val opId = S.param("id").flatMap(asInt).toOption
    val matchingMusicians = opId.map(id => from(AppSchema.musicians)(musician =>
      where(musician.musician_id === id)
      select(musician)
      orderBy(musician.last_name, musician.first_name)).toSeq) | Seq[Musician]()

    val musicianDetailsItems = matchingMusicians.map(musician =>
      MusicianDetails(musician, GroupAssignments(Some(musician.musician_id), None),
      AppSchema.assessments.where(_.musician_id === musician.musician_id).toSeq))

    def makeGroups(ga: GroupAssignment) =
      "* *" #>  (SHtml.ajaxCheckbox(false, checked => {
                  if (checked) selectedMusicianGroups += ga.musicianGroup.id -> ga.musicianGroup
                  else selectedMusicianGroups -= ga.musicianGroup.id
                  Noop
                }) ++ Text(Terms.formatted(ga.musicianGroup.school_year) + ": ") ++
                (SHtml.ajaxSelect(groups.map(g => (g.group_id.toString, g.name)).toSeq,
                  Full(ga.musicianGroup.group_id.toString), gid => {
                  AppSchema.musicianGroups.update(mg => where(mg.id === ga.musicianGroup.id)
                  set(mg.group_id := gid.toInt))
                  Noop
                })) ++
                (SHtml.ajaxSelect(instruments.map(i => (i.id.toString, i.name.is)).toSeq,
                  Full(ga.musicianGroup.instrument_id.toString), iid => {
                  AppSchema.musicianGroups.update(mg => where(mg.id === ga.musicianGroup.id)
                  set(mg.instrument_id := iid.toInt))
                  Noop
                })))

    def makeDetails(md: MusicianDetails) =
      ".heading *"      #> "%s, %d, %d, %d, %s".format(md.musician.name, md.musician.student_id,
                           md.musician.musician_id, Terms.graduationYearAsGrade(md.musician.graduation_year),
                           new LastPassFinder().lastPassed(Some(md.musician.musician_id)) mkString ", ") &
      ".groups"         #> md.groups.map(makeGroups) &
      ".assessments *"  #> {
        val (pass, fail) = md.assessments.partition(_.pass)
        "Assessments: pass: %d, fail: %d".format(pass.size, fail.size)
      }

    "#student" #> musicianDetailsItems.map(makeDetails)
  }

  def deleteGroupAssignment = SHtml.ajaxButton("Delete", () => {
    if (! selectedMusicianGroups.isEmpty) {
      Confirm("Are you sure you want to remove the %d selected group assignments?".format(selectedMusicianGroups.size),
        SHtml.ajaxInvoke(() => {
          AppSchema.musicianGroups.deleteWhere(_.id in selectedMusicianGroups.keys)
          selectedMusicianGroups = selectedMusicianGroups.empty
          reloadPage
        }))
    } else Noop
  })
}
