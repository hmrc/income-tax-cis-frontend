/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.routes.{ContractorCYAController, DeductionsSummaryController, DeleteCISPeriodController}
import models.UserPriorDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.ContractorSummaryPageBuilder.aContractorSummaryPage
import views.html.ContractorSummaryView

import java.time.Month
import java.time.Month.{APRIL, FEBRUARY, MAY}

class ContractorSummaryViewSpec extends ViewUnitTest {

  private val employerRef: String = "111/22333"
  private val page: ContractorSummaryView = inject[ContractorSummaryView]
  private val deductions: Seq[Month] = Seq(MAY, FEBRUARY, APRIL)

  object Selectors {

    val paragraphTextSelector = "#main-content > div > div > p.govuk-body"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val cisHelpLineLink = "#cis-read-more-link"
    val addAnotherLink = "#add-another-link"
    val buttonSelector = "#return-to-summary-button-id"

    def summaryListKeySelector(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def summaryListValueSelector(row: Int, cell: Int): String = s"div.govuk-summary-list__row:nth-child($row) > dd:nth-child($cell) > a:nth-child(1) > span:nth-child(1)"

    def summaryListLinksSelector(row: Int, cell: Int): String = s"div.govuk-summary-list__row:nth-child($row) > dd:nth-child($cell) > a:nth-child(1)"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedAlternativeHeading: String => String
    val expectedCaption: Int => String
    val expectedCIsHelplineLinkText: String
    val taxMonthLineItem: Int => String
    val taxMonthLineItem2: Int => String
    val taxMonthLineItem3: Int => String
    val hiddenText: (String, Int) => String
    val hiddenText2: (String, Int) => String
    val hiddenText3: String
    val viewText: String
    val changeText: String
    val removeText: String
    val expectedAddAnotherLinkText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedParagraphText: String
    val expectedInYearInsetText: String
    val expectedEOYInsetText: String
  }

  object CommonExpected extends CommonExpectedResults {
    override val expectedTitle: String = "Contractor CIS deductions"
    override val expectedHeading: String = "XYZ Constructions"
    override val expectedAlternativeHeading: String => String = (employerRef: String) => s"Contractor: $employerRef"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedCIsHelplineLinkText: String = "GOV.UK (opens in new tab)"
    override val taxMonthLineItem: Int => String = (taxYear: Int) => s"Tax month end 5 May ${taxYear - 1}"
    override val taxMonthLineItem2: Int => String = (taxYear: Int) => s"Tax month end 5 February $taxYear"
    override val taxMonthLineItem3: Int => String = (taxYear: Int) => s"Tax month end 5 April $taxYear"
    override val hiddenText: (String, Int) => String = (operation: String, taxYear: Int) => s"$operation tax month end 5 May ${taxYear - 1}"
    override val hiddenText2: (String, Int) => String = (operation: String, taxYear: Int) => s"$operation tax month end 5 February $taxYear"
    override val hiddenText3: String = s"View tax month end 5 April $taxYear"
    override val viewText: String = "View"
    override val changeText: String = "Change"
    override val removeText: String = "Remove"
    override val expectedAddAnotherLinkText: String = "Add another CIS deduction"
    override val buttonText: String = "Return to CIS summary"
  }

  object ExpectedIndividual extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInYearInsetText: String = s"You cannot update your CIS information until 6 April $taxYear."
    override val expectedEOYInsetText: String = "You can make changes but you cannot remove information we have entered for you. " +
      "Read more about the Construction Industry Scheme at GOV.UK (opens in new tab)."
  }

  object ExpectedAgent extends SpecificExpectedResults {
    override val expectedParagraphText: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInYearInsetText: String = s"You cannot update your client’s CIS information until 6 April $taxYear."
    override val expectedEOYInsetText: String = "You can make changes but you cannot remove information we have entered for your client. " +
      "Read more about the Construction Industry Scheme at GOV.UK (opens in new tab)."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpected, Some(ExpectedIndividual)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpected, Some(ExpectedAgent))
  )

  ".show" should {
    userScenarios.foreach { userScenario =>
      s"Request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the in year contractor summary page with multiple deduction periods" which {
          val pageModel = aContractorSummaryPage.copy(taxYear = taxYear, contractorName = Some("XYZ Constructions"), employerRef = employerRef, deductionPeriods = deductions)

          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          h1Check(userScenario.commonExpectedResults.expectedHeading)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInYearInsetText, Selectors.insetTextSelector)
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem(taxYear), Selectors.summaryListKeySelector(1))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(row = 1, cell = 2), additionalTestText = "(first row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" +
            userScenario.commonExpectedResults.hiddenText(userScenario.commonExpectedResults.viewText, taxYear), Selectors.summaryListLinksSelector(row = 1, cell = 2),
            ContractorCYAController.show(taxYear, MAY.toString.toLowerCase, employerRef).url, additionalTestText = "(first row)")
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem2(taxYear), Selectors.summaryListKeySelector(2))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(row = 2, cell = 2), additionalTestText = "(second row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" +
            userScenario.commonExpectedResults.hiddenText2(userScenario.commonExpectedResults.viewText, taxYear), Selectors.summaryListLinksSelector(row = 2, cell = 2),
            ContractorCYAController.show(taxYear, FEBRUARY.toString.toLowerCase, employerRef).url, additionalTestText = "(second row)")
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem3(taxYear), Selectors.summaryListKeySelector(3))
          textOnPageCheck(userScenario.commonExpectedResults.viewText, Selectors.summaryListValueSelector(row = 3, cell = 2), additionalTestText = "(third row)")
          linkCheck(userScenario.commonExpectedResults.viewText + "" + userScenario.commonExpectedResults.hiddenText3, Selectors.summaryListLinksSelector(row = 3, cell = 2),
            ContractorCYAController.show(taxYear, APRIL.toString.toLowerCase, employerRef).url, additionalTestText = "(third row)")
          elementNotOnPageCheck(Selectors.addAnotherLink)
          buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector, Some(DeductionsSummaryController.show(taxYear).url))
        }

        "render end of year contractor summary page with multiple deduction periods" which {
          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val pageModel = aContractorSummaryPage.copy(taxYear = taxYearEOY, isInYear = false, contractorName = Some("XYZ Constructions"),
            employerRef = employerRef, deductionPeriods = deductions, customerDeductionPeriods = Seq(MAY))
          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
          h1Check(userScenario.commonExpectedResults.expectedHeading)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedEOYInsetText, Selectors.insetTextSelector)
          linkCheck(userScenario.commonExpectedResults.expectedCIsHelplineLinkText, Selectors.cisHelpLineLink,
            href = "https://www.gov.uk/what-is-the-construction-industry-scheme")
          textOnPageCheck(userScenario.commonExpectedResults.taxMonthLineItem(taxYearEOY), Selectors.summaryListKeySelector(1))
          textOnPageCheck(userScenario.commonExpectedResults.changeText, Selectors.summaryListValueSelector(row = 1, cell = 2), additionalTestText = "(first row)")
          linkCheck(userScenario.commonExpectedResults.changeText + "" +
            userScenario.commonExpectedResults.hiddenText(userScenario.commonExpectedResults.changeText, taxYearEOY), Selectors.summaryListLinksSelector(row = 1, cell = 2),
            ContractorCYAController.show(taxYearEOY, MAY.toString.toLowerCase, employerRef).url, additionalTestText = "(first row)")
          textOnPageCheck(userScenario.commonExpectedResults.removeText, Selectors.summaryListValueSelector(row = 1, cell = 3), additionalTestText = "(first row)")
          linkCheck(userScenario.commonExpectedResults.removeText + " " +
            userScenario.commonExpectedResults.hiddenText(userScenario.commonExpectedResults.removeText, taxYearEOY), Selectors.summaryListLinksSelector(row = 1, cell = 3),
            DeleteCISPeriodController.show(taxYearEOY, MAY.toString.toLowerCase, employerRef).url, additionalTestText = "(first row)")
          textOnPageCheck(userScenario.commonExpectedResults.changeText, Selectors.summaryListValueSelector(row = 2, cell = 2), additionalTestText = "(second row)")
          linkCheck(userScenario.commonExpectedResults.changeText + "" +
            userScenario.commonExpectedResults.hiddenText2(userScenario.commonExpectedResults.changeText, taxYearEOY), Selectors.summaryListLinksSelector(row = 2, cell = 2),
            ContractorCYAController.show(taxYearEOY, FEBRUARY.toString.toLowerCase, employerRef).url, additionalTestText = "(second row)")
          elementNotOnPageCheck(Selectors.summaryListValueSelector(row = 2, cell = 3))
          linkCheck(userScenario.commonExpectedResults.expectedAddAnotherLinkText, Selectors.addAnotherLink,
            href = controllers.routes.ContractorSummaryController.addCisDeduction(taxYearEOY, employerRef).url)
        }

        "render the in year contractor summary page with an alternative heading when there's no contractor name" which {
          val pageModel = aContractorSummaryPage.copy(taxYear = taxYear, contractorName = None, employerRef = employerRef, deductionPeriods = deductions)

          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          h1Check(userScenario.commonExpectedResults.expectedAlternativeHeading(employerRef))
          buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector, Some(DeductionsSummaryController.show(taxYear).url))
        }
        "render the end of year contractor summary page with out the add another link" which {
          val pageModel = aContractorSummaryPage.copy(taxYear = taxYearEOY, isInYear = false, contractorName = None, employerRef = employerRef, deductionPeriods = Month.values().toIndexedSeq)


          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(page(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
          h1Check(userScenario.commonExpectedResults.expectedAlternativeHeading(employerRef))
          buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector, Some(DeductionsSummaryController.show(taxYearEOY).url))

        }

      }
    }
  }
}
