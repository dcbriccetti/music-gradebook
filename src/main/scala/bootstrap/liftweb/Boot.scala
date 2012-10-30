package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import js.jquery.JQueryArtifacts
import sitemap._
import Loc._
import mapper._

import net.liftmodules.JQueryModule
import org.squeryl.PrimitiveTypeMode._
import com.dbschools.mgb.Db

object RunState {
  object loggedIn extends SessionVar[Boolean] (false)
}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("com.dbschools.mgb")

    val loggedIn    = If(() => RunState.loggedIn,   "Not logged in")
    val notLoggedIn = If(() => ! RunState.loggedIn, "Already logged in")

    // Build SiteMap
    def sitemap = SiteMap(
      Menu.i("Home"      ) / "index",
      Menu.i("Log In"    ) / "logIn"          >> notLoggedIn,
      Menu.i("Groups"    ) / "groups"         >> loggedIn,
      Menu.i("No Groups" ) / "noGroups"       >> loggedIn,
      Menu.i("Details"   ) / "studentDetails" >> loggedIn >> Hidden,
      Menu.i("Problems"  ) / "problems"       >> loggedIn,
      Menu.i("Statistics") / "stats"          >> loggedIn,
      Menu.i("Log Out"   ) / "logOut"         >> loggedIn
    )

    LiftRules.setSiteMap(sitemap)

    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery172
    JQueryModule.init()

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => true)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    Db.initialize()
    S.addAround(new LoanWrapper{override def apply[T](f: => T): T = {inTransaction {f}}})
  }
}
