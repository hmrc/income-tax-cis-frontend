/*
 * Copyright 2022 HM Revenue & Customs
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
import support.builders.models.pages.ContractorSummaryPageBuilder.aContractorSummaryPage
import views.html.ContractorSummaryView
import utils.UrlUtils.encode
import controllers.routes.ContractorCYAController
import controllers.routes.DeductionsSummaryController

import java.time.Month

class ContractorSummaryViewSpec extends ViewUnitTest {

  private val page: ContractorSummaryView = inject[ContractorSummaryView]

  private val employerRef: String = "111/22333"
  private val deductions: Seq[Month] = Seq(Month.MAY, Month.FEBRUARY, Month.APRIL)

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > p"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#return-to-summary-button-id"

    def summaryListKeySelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListValueSelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd > a > span:nth-child(1)"

    def linkSelectorForSummaryListValue(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd > a"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: String

    def expectedAlternativeHeading(employerRef: String): String

    val taxMonthLineItem: String
    val taxMonthLineItem2: String
    val taxMonthLineItem3: String
    val hiddenText: String
    val hiddenText2: String
    val hiddenText3: String
    val viewText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedParagraphText: String
    val expectedInsetText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "Contractor CIS deductions"
    override val expectedHeading: String = "XYZ Constructions"
    override val expectedCaption: String = s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"

    override def expectedAlternativeHeading(employerRef: String): String = s"Contractor: $employerRef"

    override val taxMonthLineItem: String = s"Tax month end 5 May ${taxYear - 1}"
    override val taxMonthLineItem2: String = s"Tax month end 5 February $taxYear"
    override val taxMonthLineItem3: String = s"Tax month end 5 April $taxYear"
    override val hiddenText: String = s"View tax month end 5 May ${taxYear - 1}"
    override val hiddenText2: String = s"View tax month end 5 February $taxYear"
    override val hiddenText3: String = s"View tax month end 5 April $taxYear"
    override val viewText: String = "View"
    override val buttonText: String = "Return to CIS summary"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "Contractor CIS deductions"
    override val expectedHeading: String = "XYZ Constructions"
    override val expectedCaption: String = s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"

    override def expectedAlternativeHeading(employerRef: String): String = s"Contractor: $employerRef"

    override val taxMonthLineItem: String = s"Tax month end 5 Mai ${taxYear - 1}"
    override val taxMonthLineItem2: String = s"Tax month end 5 Chwefror $taxYear"
    override val taxMonthLineItem3: String = s"Tax month end 5 Ebrill $taxYear"
    override val hiddenText: String = s"View tax month end 5 Mai ${taxYear - 1}"
    override val hiddenText2: String = s"View tax month end 5 Chwefror $taxYear"
    override val hiddenText3: String = s"View tax month end 5 Ebrill $taxYear"
    override val viewText: String = "View"
    override val buttonText: String = "Return to CIS summary"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: String = s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: String = s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: String = s"You cannot update your client’s CIS information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: String = s"You cannot update your client’s CIS information until 6 April $taxYear."
  }


  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the in year contractor summary page with multiple deduction periods" which {

          val pageModel = aContractorSummaryPage.copy(taxYear, Some("XYZ Constructions"), employerRef, deductions)

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle)
          captionCheck(userScenario.commonExpectedResults.expectedCaption)
          h1Check(userScenario.commonExpectedResults.expectedHeading)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText, Selectors.insetTextSelector)
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem, Selectors.summaryListKeySelector(1))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(1), additionalTestText = "(first row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" + userScenario.commonExpectedResults.hiddenText, Selectors.linkSelectorForSummaryListValue(1),
            ContractorCYAController.show(taxYear, Month.MAY.toString.toLowerCase, encode(employerRef)).url, additionalTestText = "(first row)")
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem2, Selectors.summaryListKeySelector(2))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(2), additionalTestText = "(second row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" + userScenario.commonExpectedResults.hiddenText2, Selectors.linkSelectorForSummaryListValue(2),
            ContractorCYAController.show(taxYear, Month.FEBRUARY.toString.toLowerCase, encode(employerRef)).url, additionalTestText = "(second row)")
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem3, Selectors.summaryListKeySelector(3))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(3), additionalTestText = "(third row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" + userScenario.commonExpectedResults.hiddenText3, Selectors.linkSelectorForSummaryListValue(3),
            ContractorCYAController.show(taxYear, Month.APRIL.toString.toLowerCase, encode(employerRef)).url, additionalTestText = "(third row)")
          buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector, Some(DeductionsSummaryController.show(taxYear).url))

        }

        "render the in year contractor summary page with an alternative heading when there's no contractor name" which {

          val pageModel = aContractorSummaryPage.copy(taxYear, None, employerRef, deductions)

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle)
          captionCheck(userScenario.commonExpectedResults.expectedCaption)
          h1Check(userScenario.commonExpectedResults.expectedAlternativeHeading(employerRef))
          buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector, Some(DeductionsSummaryController.show(taxYear).url))

        }
      }
    }
  }
}
