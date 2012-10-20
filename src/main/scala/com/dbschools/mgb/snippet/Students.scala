package com.dbschools.mgb
package snippet

import org.squeryl.PrimitiveTypeMode._
import net.liftweb._
import util._
import Helpers._
import http._
import schema.{Musician, Group, MusicianGroup, Instrument, AppSchema}

class Students {

  private case class RowData(musician: Musician, group: Group, musicianGroup: MusicianGroup, instrument: Instrument)

  def inGroups =
    "#studentRow"   #> studentGroups(None).map(row =>
      ".schYear  *" #> row.musicianGroup.school_year &
      ".stuName  *" #> row.musician.name &
      ".gradYear *" #> row.musician.graduation_year &
      ".id       *" #> row.musician.musician_id &
      ".stuId    *" #> row.musician.student_id &
      ".group    *" #> row.group.name &
      ".instr    *" #> row.instrument.name
    )

  def inNoGroups = {
    val musicians = join(AppSchema.musicians, AppSchema.musicianGroups.leftOuter)((m, mg) =>
      where(mg.map(_.id).isNull)
      select(m)
      on(m.musician_id === mg.map(_.musician_id))
    )

    "#studentRow"   #> musicians.map(m =>
      ".stuName  *" #> m.name &
        ".id     *" #> m.musician_id &
      ".stuId    *" #> m.student_id &
      ".gradYear *" #> m.graduation_year
    )
  }

  def details = {
    import AppSchema.{musicians, assessments}
    val ms = S.param("name").map(search => from(musicians)(m =>
      where(m.last_name.like("%" + search + "%"))
      select(m)
      orderBy(m.last_name, m.first_name))).getOrElse(List[Musician]())
    "#student"   #> ms.map(m =>
      ".details" #>
        <div>
          <p>{"%s, %d, %d, %d".format(m.name, m.student_id, m.musician_id, m.graduation_year)}</p>
          {
            studentGroups(Some(m.musician_id)).map(r => <p> {
              "%d: In %s on %s".format(r.musicianGroup.school_year, r.group.name, r.instrument.name)
            }</p>) ++ {
              val (pass, fail) = assessments.where(_.musician_id === m.musician_id).partition(_.pass)
              <p>{"Assessments: pass: %d, fail: %d".format(pass.size, fail.size)}</p>
            }
          }
        </div>
    )
  }

  private def studentGroups(id: Option[Int]) = {
    import AppSchema._
    val rows = from(musicians, groups, musicianGroups, instruments)((m, g, mg, i) =>
      where(conditions(id, m, mg, g, i))
      select(RowData(m, g, mg, i))
      orderBy(mg.school_year desc, m.last_name, m.first_name, g.name)
    )
    rows
  }

  private def conditions(opId: Option[Int], m: Musician, mg: MusicianGroup, g: Group, i: Instrument) = {
    val joinConditions = m.musician_id === mg.musician_id and mg.group_id === g.group_id and
      mg.instrument_id === i.instrument_id
    opId match {
      case None => joinConditions
      case Some(id) => joinConditions and m.musician_id === id
    }
  }
}
