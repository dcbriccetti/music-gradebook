package com.dbschools.mgb
package snippet

import scala.xml.{Text, NodeSeq}
import org.apache.log4j.Logger
import org.squeryl.PrimitiveTypeMode._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.{RequestVar, SHtml}
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http.js.JsCmds.Confirm
import net.liftweb.util.Helpers._
import schema.{Assessment, Musician, MusicianGroup, AppSchema}
import model.{Cache, GroupAssignment, SelectedMusician, Terms}

case class MusicianDetails(musician: Musician, groups: Iterable[GroupAssignment], assessments: Iterable[Assessment])

object rvMusicianDetails extends RequestVar[Option[MusicianDetails]](None)

class GroupAssignments extends SelectedMusician {
  private val log = Logger.getLogger(getClass)
  private var selectedMusicianGroups = Set[Int]()
  private val groupSelectorValues = Cache.filteredGroups().map(gp => (gp.group.id.toString, gp.group.name)).toSeq
  private var newAssignmentGroupId = groupSelectorValues(0)._1.toInt
  private val opMusicianDetails = opMusician.map(musician =>
    MusicianDetails(musician, model.GroupAssignments(Some(musician.id)),
    AppSchema.assessments.where(_.musician_id === musician.id).toSeq))

  def render = {

    def delete() = {
      if (selectedMusicianGroups.nonEmpty) {
        Confirm(
          s"Are you sure you want to remove the ${selectedMusicianGroups.size} selected group assignments?",
          SHtml.ajaxInvoke(() => {
            val ids = selectedMusicianGroups
            AppSchema.musicianGroups.deleteWhere(_.id in ids)
            log.info("Deleted group assignment(s): " + ids)
            selectedMusicianGroups = selectedMusicianGroups.empty
            Reload
          }))
      } else Noop
    }

    def create() = {
      def opSoleAssignedInstrumentId: Option[Int] = {
        for {
          md <- opMusicianDetails
          gps = md.groups.map(_.instrument.id).toSet.toSeq
          if gps.size == 1
        } yield gps(0)
      }

      for {
        instrumentId <- opSoleAssignedInstrumentId orElse Cache.instruments.find(_.name.get == "Unassigned").map(_.id)
        musician     <- opMusician
      } {
        val musicianGroup = MusicianGroup(0, musician.id, newAssignmentGroupId, instrumentId,
          Terms.currentTerm)
        AppSchema.musicianGroups.insert(musicianGroup)
        log.info("Made musician assignment: " + musicianGroup)
      }
      Reload
    }

    def nextSel = {
      SHtml.ajaxSelect(groupSelectorValues, Empty, gid => {
        newAssignmentGroupId = gid.toInt
        Noop
      })
    }

    def groupsTable(groups: Iterable[model.GroupAssignment]) =
      groups.map(ga => {
        val curTerm = ga.musicianGroup.school_year == Terms.currentTerm
        ".sel *"        #> (if (curTerm) assignmentCheckbox(ga) else NodeSeq.Empty) &
        ".year *"       #> Terms.formatted(ga.musicianGroup.school_year) &
        ".group *"      #>  {
                              if (curTerm) groupSelector(ga)
                              else Text(Cache.groups.find(_.id == ga.musicianGroup.group_id).map(_.name) getOrElse "")
                            } &
        ".instrument *" #> (if (curTerm) instrumentSelector(ga) else
          Text(Cache.instruments.find(_.id == ga.musicianGroup.instrument_id).map(_.name.get) getOrElse ""))
      })

    ".assignmentRow *"  #> opMusicianDetails.toSeq.flatMap(md => groupsTable(md.groups)) &
    "#delete"   #> SHtml.ajaxButton("Remove from selected groups", () => delete()) &
    "#create"   #> SHtml.ajaxButton(s"Add to group", () => create()) &
    "#nextSel"  #> nextSel
  }

  private def assignmentCheckbox(ga: GroupAssignment) =
    SHtml.ajaxCheckbox(false, checked => {
      if (checked) selectedMusicianGroups += ga.musicianGroup.id
      else selectedMusicianGroups -= ga.musicianGroup.id
      if (selectedMusicianGroups.isEmpty) JsHideId("delete") else JsShowId("delete")
    })

  private def groupSelector(ga: GroupAssignment) =
    SHtml.ajaxSelect(groupSelectorValues, Full(ga.musicianGroup.group_id.toString), gid => {
      AppSchema.musicianGroups.update(mg => where(mg.id === ga.musicianGroup.id)
        set (mg.group_id := gid.toInt))
      Noop
    })

  private def instrumentSelector(ga: GroupAssignment) =
    SHtml.ajaxSelect(Cache.instruments.map(i => (i.id.toString, i.name.get)).toSeq,
      Full(ga.musicianGroup.instrument_id.toString), iid => {
        AppSchema.musicianGroups.update(mg => where(mg.id === ga.musicianGroup.id)
          set (mg.instrument_id := iid.toInt))
        Reload // Todo Instead, update the instrument(s) to test on
      })
}
