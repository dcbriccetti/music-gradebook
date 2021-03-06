package com.dbschools.mgb
package comet

import net.liftweb.http.js.JsCmds.Replace
import net.liftweb.http.{CometActor, CometListener}
import net.liftweb.util.PassThru
import org.apache.log4j.Logger
import snippet.LiftExtensions._

import scala.language.postfixOps

class GeneralSettingsCometActor extends CometActor with CometListener {
  val log = Logger.getLogger(getClass)
  import CommonCometActorMessages._
  import GeneralSettingsCometActorMessages._

  def registerWith = GeneralSettingsCometDispatcher

  override def lowPriority = {

    case ChangeServicingQueueSelection =>
      partialUpdate(replaceDefaultPageSection("queueService"))

    case ChangePeriodElements =>
      partialUpdate(snippet.Periods.js & replaceDefaultPageSection("periodSpan"))

    case ChangeSpecialSchedule =>
      partialUpdate(replaceDefaultPageSection("specialScheduleOuter"))

    case Start =>
  }

  private def replaceDefaultPageSection(elemId: String) = {
    val elem = elemFromTemplate("templates-hidden/default", s"#$elemId")
    Replace(elemId, elem)
  }

  def render = PassThru
}

object GeneralSettingsCometDispatcher extends CommonCometDispatcher

object GeneralSettingsCometActorMessages {
  case object ChangeServicingQueueSelection
  case object ChangePeriodElements
  case object ChangeSpecialSchedule
}
