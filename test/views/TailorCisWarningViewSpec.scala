/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views

import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.TailorCisWarningView
import controllers.routes.TailorCisWarningController

class TailorCisWarningViewSpec extends ViewUnitTest {

  private val appUrl = "/update-and-submit-income-tax-return/construction-industry-scheme-deductions"
  private val cisSummaryUrl = s"$appUrl/$taxYearEOY/summary"


  object Selectors {
    val paragraphTextSelector = "#remove-info-id1"
    val paragraph2TextSelector = "#remove-info-id2"
    val paragraph3TextSelector = "#remove-info-id3"
    val remove_button = "#remove-cis-button-id"
    val cancelLinkSelector = "#cancel-link-id"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedTitle: String

    def expectedHeading(): String

    val expectedCaption: String
    val expectedP1: String
    val expectedP2: String
    val expectedP3: String
    val expectedCancelLink: String
    val expectedConfirmLink: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to change CIS deductions for the tax year?"
    def expectedHeading(): String = "Are you sure you want to change CIS deductions for the tax year?"

    override val expectedCaption: String = s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY"
    override val expectedP1: String = "If you make a change, the information you have entered will be deleted."
    override val expectedP2: String = "You can’t delete information we already hold. We will not use these details to calculate your Income Tax Return for this tax year."
    override val expectedP3: String = "You’ll still see some details, but all amounts are set to £0."
    override val expectedCancelLink: String = "Cancel"
    override val expectedConfirmLink: String = "Confirm"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am newid didyniadau CIS ar gyfer y flwyddyn dreth?"
    val expectedCaption = s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYearEOY - 1} i 5 Ebrill $taxYearEOY"

    override def expectedHeading(): String = "A ydych yn siŵr eich bod am newid didyniadau CIS ar gyfer y flwyddyn dreth?"

    override val expectedP1: String = "Os ydych yn gwneud newid, bydd yr wybodaeth rydych wedi’i nodi’n cael ei dileu."
    override val expectedP2: String = "Ni allwch ddileu gwybodaeth sydd eisoes gennym. Ni fyddwn yn defnyddio’r manylion hyn i gyfrifo’ch Ffurflen Dreth Incwm ar gyfer y flwyddyn dreth hon."
    override val expectedP3: String = "Byddwch yn dal i weld rhywfaint o fanylion, ond bydd yr holl symiau’n cael eu gosod i £0."
    override val expectedCancelLink: String = "Canslo"
    override val expectedConfirmLink: String = "Cadarnhau"
  }

  private val underTest = inject[TailorCisWarningView]

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  ".show" should {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common = userScenario.commonExpectedResults
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the remove all cis warning page" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          welshToggleCheck(userScenario.isWelsh)

          titleCheck(common.expectedTitle, userScenario.isWelsh)
          h1Check(common.expectedHeading())
          captionCheck(common.expectedCaption)
          textOnPageCheck(common.expectedP1, paragraphTextSelector)
          textOnPageCheck(common.expectedP2, paragraph2TextSelector)
          textOnPageCheck(common.expectedP3, paragraph3TextSelector)
          buttonCheck(common.expectedConfirmLink, remove_button)
          linkCheck(common.expectedCancelLink, cancelLinkSelector, cisSummaryUrl)
          formPostLinkCheck(TailorCisWarningController.submit(taxYearEOY).url, formSelector)
        }
      }
    }

  }
}
