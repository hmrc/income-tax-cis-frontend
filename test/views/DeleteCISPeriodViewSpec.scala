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

import controllers.routes.{ContractorSummaryController, DeleteCISPeriodController}
import models.UserPriorDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.DeleteCISPeriodPageBuilder.aDeleteCISPeriodPage
import utils.ViewUtils.translatedMonthAndTaxYear
import views.html.DeleteCISPeriodView

import java.time.Month

class DeleteCISPeriodViewSpec extends ViewUnitTest {

  object Selectors {
    val buttonFormSelector = "#main-content > div > div > form"
    val buttonSelector: String = "#remove-period-button-id"
    val linkSelector: String = "#cancel-link-id"

    def paragraphTextSelector(number: Int): String = s"p.govuk-body:nth-child(${number + 1})"
  }

  trait CommonExpectedResults {

    val expectedTitle: String
    val expectedCaption: Int => String
    val expectedH1: String
    val expectedP1: (Month, Int) => String
    val expectedButtonText: String
    val expectedLinkText: String

  }

  trait SpecificExpectedResults {
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "Are you sure you want to remove this CIS deduction?"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "Are you sure you want to remove this CIS deduction?"
    override val expectedP1: (Month, Int) => String = (month: Month, year: Int) =>
      s"You will remove the CIS deduction for the tax month ending 5 ${translatedMonthAndTaxYear(month, year)(getMessages(isWelsh = false))}."
    override val expectedButtonText: String = "Remove"
    override val expectedLinkText: String = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "A ydych yn siŵr eich bod am dynnu’r didyniad CIS hwn?"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedH1: String = "A ydych yn siŵr eich bod am dynnu’r didyniad CIS hwn?"
    override val expectedP1: (Month, Int) => String = (month: Month, year: Int) =>
      s"Byddwch yn tynnu’r didyniad CIS ar gyfer y mis treth sy’n dod i ben ar 5 ${translatedMonthAndTaxYear(month, year)(getMessages(isWelsh = true))}."
    override val expectedButtonText: String = "Tynnu"
    override val expectedLinkText: String = "Canslo"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults

  object ExpectedAgentEN extends SpecificExpectedResults

  object ExpectedIndividualCY extends SpecificExpectedResults

  object ExpectedAgentCY extends SpecificExpectedResults

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[DeleteCISPeriodView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page " which {
        implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val document: Document = Jsoup.parse(underTest(aDeleteCISPeriodPage).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedH1)
//        textOnPageCheck(userScenario.commonExpectedResults.expectedP1(aDeleteCISPeriodPage.month, aDeleteCISPeriodPage.taxYear), Selectors.paragraphTextSelector(number = 1))
        formPostLinkCheck(DeleteCISPeriodController.submit(taxYearEOY, aDeleteCISPeriodPage.employerRef, aDeleteCISPeriodPage.month.toString).url, Selectors.buttonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
        linkCheck(userScenario.commonExpectedResults.expectedLinkText, Selectors.linkSelector, ContractorSummaryController.show(taxYearEOY, aDeleteCISPeriodPage.employerRef).url)
      }
    }
  }
}
