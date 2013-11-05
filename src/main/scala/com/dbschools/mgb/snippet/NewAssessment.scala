package com.dbschools.mgb
package snippet

import java.sql.Timestamp
import scala.xml.Elem
import org.apache.log4j.Logger
import scalaz._
import Scalaz._
import org.squeryl.PrimitiveTypeMode._
import org.scala_tools.time.Imports._
import net.liftweb.util.Helpers._
import net.liftweb.http
import http.SHtml
import http.js.JsCmds.{Noop, Reload, Script}
import http.js.JsCmds.SetValById
import http.js.JE.JsRaw
import http.js.JsCmds.ReplaceOptions
import net.liftweb.common.Empty
import net.liftweb.common.Full
import model.{AssessmentRow, Cache, GroupAssignments, LastPassFinder, Terms}
import schema.{AssessmentTag, AppSchema, Piece}
import com.dbschools.mgb.comet.ActivityCometDispatcher
import com.dbschools.mgb.comet.ActivityStatusUpdate
import com.dbschools.mgb.schema.Assessment

class NewAssessment extends MusicianFromReq {
  private val log = Logger.getLogger(getClass)

  def render = {
    val lastPassFinder = new LastPassFinder()

    case class Pi(piece: Piece, instId: Int, opSubInstId: Option[Int] = None)

    val opNextPi = {
      val pi = for {
        musician  <- opMusician
        lastPass  <- lastPassFinder.lastPassed(Some(musician.musician_id.get)).headOption
        piece     <- Cache.pieces.find(_.id == lastPass.pieceId)
        nextPiece =  lastPassFinder.next(piece)
      } yield Pi(nextPiece, lastPass.instrumentId, lastPass.opSubinstrumentId)

      pi orElse GroupAssignments(opMusician.map(_.id), opSelectedTerm = Some(Terms.currentTerm)).headOption.map(ga =>
        Pi(Cache.pieces.head, ga.instrument.id))
    }

    val commentTagSelections = scala.collection.mutable.Map(Cache.tags.map(_.id -> false): _*)
    var opSelInstId = opNextPi.map(_.instId)
    var opSelSubinstId = opNextPi.flatMap(_.opSubInstId)
    var opSelPieceId = opNextPi.map(_.piece.id)
    var notes = ""
    var tempo = 0

    def findTempo = opSelPieceId.flatMap(selPieceId =>
      // First look for a tempo for the specific instrument
      Cache.tempos.find(t => t.instrumentId == opSelInstId && t.pieceId == selPieceId) orElse
      Cache.tempos.find(_.pieceId == selPieceId))

    def recordAss(pass: Boolean): Unit = {
      for {
        musician  <- opMusician
        iid       <- opSelInstId
        pid       <- opSelPieceId
        user      <- AppSchema.users.find(_.login == Authenticator.userName.get)
      } {
        val assTime = DateTime.now
        val newAss = Assessment(
          id                = 0,
          assessment_time   = new Timestamp(assTime.getMillis),
          musician_id       = musician.musician_id.get,
          instrument_id     = iid,
          subinstrument_id  = opSelSubinstId,
          user_id           = user.id,
          pieceId           = pid,
          pass              = pass,
          notes             = notes
        )
        AppSchema.assessments.insert(newAss)
        val selectedCommentIds = (for {
          (commentId, selected) <- commentTagSelections
          if selected
        } yield commentId).toSet
        val tags = selectedCommentIds.map(id => AssessmentTag(newAss.id, id))
        AppSchema.assessmentTags.insert(tags)
        log.info(s"Assessment: $newAss, $tags")

        ActivityCometDispatcher ! ActivityStatusUpdate {
          val inst = opSelInstId.flatMap(id => Cache.instruments.find(_.id == id)).map(_.name.get)
          val subinst = opSelSubinstId.flatMap(id => Cache.subinstruments.values.flatten.find(_.id == id)).map(_.name.get)
          val predef = Cache.tags.filter(t => selectedCommentIds.contains(t.id)).map(_.commentText).mkString(", ")
          val expandedNotes = (if (predef.isEmpty) "" else s"$predef; ") + notes
          AssessmentRow(assTime, musician, user.last_name,
            ~opSelPieceId.flatMap(id => Cache.pieces.find(_.id == id)).map(_.name.get),
            ~inst, subinst, pass, if (expandedNotes.isEmpty) None else Some(expandedNotes))
        }
      }
    }

    val subinstId = "subinstrument"
    val initialInstrumentSel = opSelInstId.map(i => Full(i.toString)) getOrElse Empty

    def subinstSels(instId: Int): List[(String, String)] =
      Cache.subinstruments.get(instId).toList.flatten.map(si => si.id.toString -> si.name.get)

    def selInst = SHtml.ajaxSelect(Cache.instruments.map(i => i.id.toString -> i.name.get), initialInstrumentSel, (p) => {
      val instId = p.toInt
      opSelInstId = Some(instId)
      val sels = subinstSels(instId)
      sels.headOption.foreach(sel => opSelSubinstId = Some(sel._1.toInt))
      ReplaceOptions(subinstId, sels, Empty)
    })

    def selSubinst = {
      val opts = opSelInstId.map(subinstSels) getOrElse Seq[(String, String)]()
      def setSubinstId(idString: String) { opSelSubinstId = Some(idString.toInt) }
      opts.headOption.foreach(sel => setSubinstId(sel._1))
      SHtml.select(opts, Empty, setSubinstId)
    }

    def setJsTempo(t: Option[Int]) = JsRaw(s"tempoBpm = ${~t}").cmd

    def selPiece = {
      val initialSel = opSelPieceId.map(p => Full(p.toString)) getOrElse Empty
      SHtml.ajaxSelect(Cache.pieces.map(p => p.id.toString -> p.name.get),
        initialSel, (p) => {
          opSelPieceId = Some(p.toInt)
          val t = findTempo.map(_.tempo)
          SetValById("tempo", ~t.map(_.toString)) & setJsTempo(t)
        })
    }

    def checkboxes(part: Int): Seq[Elem] = {
      val grouped = Cache.tags.grouped(Cache.tags.size / 2).toSeq
      grouped(part).map(tag =>
        <div class="checkbox">
          <label>
            {SHtml.checkbox(false, (checked) => commentTagSelections(tag.id) = checked)}{tag.commentText}
          </label>
        </div>)
    }

    def commentText = SHtml.textarea("", (s) => {
      notes = s
      Noop
    }, "id" -> "commentText", "rows" -> "3", "style" -> "width: 30em;", "placeholder" -> "Additional comments")

    "#instrument"     #> selInst &
    s"#$subinstId"    #> selSubinst &
    "#piece"          #> selPiece &
    "#tempo"          #> SHtml.ajaxText(~findTempo.map(_.tempo.toString), (t) => asInt(t).map(ti => {
                            tempo = ti
                            setJsTempo(Some(ti))
                          }) getOrElse Noop, "id" -> "tempo", "size" -> "3") &
    "#setTempo"       #> Script(setJsTempo(findTempo.map(_.tempo))) &
    "#checkbox1 *"    #> checkboxes(0) &
    "#checkbox2 *"    #> checkboxes(1) &
    "#commentText"    #> commentText &
    "#passButton"     #> SHtml.ajaxSubmit("Pass", () => { recordAss(pass = true ); Reload }) &
    "#failButton"     #> SHtml.ajaxSubmit("Fail", () => { recordAss(pass = false); Reload })
  }
}
