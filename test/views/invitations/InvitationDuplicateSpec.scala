package views.invitations

import base.SpecBase
import play.twirl.api.HtmlFormat
import viewmodels.Message
import views.behaviours.ViewBehaviours

class InvitationDuplicateSpec extends ViewBehaviours {

  import InvitationDuplicateSpec._

    "Invitation Success Page" must {

      behave like normalPage(
        createView(this),
        messageKeyPrefix,
        Message("messages__invitationDuplicate__heading", testInviteeName)
      )

      "state the scheme name" in {
        createView(this) must haveElementWithText("schemeName", Message("messages__invitationDuplicate__schemeName", testSchemeName))
      }

    }
}

object InvitationDuplicateSpec {

  val testInviteeName: String = "Joe Bloggs"
  val testSchemeName: String = "Test Scheme Ltd"
  val messageKeyPrefix = "invitationDuplicate"


  def createView(base: SpecBase): () => HtmlFormat.Appendable = () =>
    invitation_duplicate(
      base.frontendAppConfig,
      testInviteeName,
      testSchemeName,
    )(base.fakeRequest, base.messages)

}
