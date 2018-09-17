package pages

import pages.behaviours.PageBehaviours


class PSANAmePageSpec extends PageBehaviours {

  "PSANAmePage" must {

    beRetrievable[String](PsaNamePage)

    beSettable[String](PsaNamePage)

    beRemovable[String](PsaNamePage)
  }
}
