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

import forms.DeductionPeriodFormProvider
import models.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.DeductionPeriodPageBuilder.aDeductionPeriodPage
import views.html.DeductionPeriodView

import java.time.Month
import java.time.Month.AUGUST

class DeductionPeriodViewSpec extends ViewUnitTest {

  private val taxYearEOYStart: Int = taxYearEOY - 1

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > p.govuk-body"
    val buttonSelector = "#continue"
    val labelSelector = "#main-content > div > div > form > div > label"
    val optionSelector = "#month"
    val optionsSelector: Int => String = (option: Int) => s"#month > option:nth-child($option)"
  }

  trait CommonExpectedResults {
    val expectedLabel: String
    val expectedHeading: String
    val expectedCaption: Int => String

  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedTitleNoContractorName: String
    val expectedErrorTitle: String
    val expectingHeadingNoContractorName: String
    val expectedP1: String
    val expectedError: String
    val expectedButtonText: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedLabel: String = "Tax month ending"
    override val expectedHeading: String = "When did Michele Lamy Paving Ltd make CIS deductions?"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedLabel: String = "Mis treth yn dod i ben"
    override val expectedHeading: String = "Pryd gwnaeth Michele Lamy Paving Ltd wneud didyniadau CIS?"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your contractor make CIS deductions?"
    override val expectedTitleNoContractorName: String = "When did this contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your contractor make CIS deductions?"
    override val expectingHeadingNoContractorName: String = "When did this contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your CIS statement."
    override val expectedError: String = "You cannot select a date you already have information for"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Pryd gwnaeth eich contractwr wneud didyniadau CIS?"
    override val expectedTitleNoContractorName: String = "Pryd gwnaeth y contractwr hwn wneud didyniadau CIS?"
    override val expectedErrorTitle: String = "Gwall: Pryd gwnaeth eich contractwr wneud didyniadau CIS?"
    override val expectingHeadingNoContractorName: String = "Pryd gwnaeth y contractwr hwn wneud didyniadau CIS?"
    override val expectedP1: String = "Rhowch wybod i ni y dyddiad dod i ben ar eich datganiad CIS."
    override val expectedError: String = "Ni allwch ddewis dyddiad y mae gennych wybodaeth eisoes ar ei gyfer"
    override val expectedButtonText: String = "Yn eich blaen"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your client’s contractor make CIS deductions?"
    override val expectedTitleNoContractorName: String = "When did this contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your client’s contractor make CIS deductions?"
    override val expectingHeadingNoContractorName: String = "When did this contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your client’s CIS statement."
    override val expectedError: String = "You cannot select a date your client already has information for"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Pryd gwnaeth contractwr eich cleient wneud didyniadau CIS?"
    override val expectedTitleNoContractorName: String = "Pryd gwnaeth y contractwr hwn wneud didyniadau CIS?"
    override val expectedErrorTitle: String = "Gwall: Pryd gwnaeth contractwr eich cleient wneud didyniadau CIS?"
    override val expectingHeadingNoContractorName: String = "Pryd gwnaeth y contractwr hwn wneud didyniadau CIS?"
    override val expectedP1: String = "Rhowch wybod i ni y dyddiad dod i ben ar ddatganiad CIS eich cleient."
    override val expectedError: String = "Ni allwch ddewis dyddiad y mae gan eich cleient wybodaeth eisoes ar ei gyfer"
    override val expectedButtonText: String = "Yn eich blaen"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[DeductionPeriodView]

  private def formProvider = new DeductionPeriodFormProvider()

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the deduction period page without errors" which {
        implicit val authRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(aDeductionPeriodPage).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedHeading)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedLabel, Selectors.labelSelector)
        buttonCheck(userScenario.specificExpectedResults.get.expectedButtonText, Selectors.buttonSelector)
      }

      "render the deduction period page with an alternative H1 when contractor name is None" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDeductionPeriodPage.copy(contractorName = None, employerRef = "some-ref", priorSubmittedPeriods = Seq(Month.APRIL))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleNoContractorName, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectingHeadingNoContractorName)
      }

      "render the deduction period page with error when at the end of the year" which {
        implicit val authRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val formWithError = formProvider.deductionPeriodForm(userScenario.isAgent, Seq(AUGUST)).bind(Map("month" -> "august"))

        val pageModel = aDeductionPeriodPage.copy(form = formWithError)

        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedHeading)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedLabel, Selectors.labelSelector)
        buttonCheck(userScenario.specificExpectedResults.get.expectedButtonText, Selectors.buttonSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedError, Selectors.optionSelector)
      }
    }
  }

  "the view" should {
    "display the month options correctly in welsh" which {
      implicit val authRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(isAgent = false)
      implicit val messages: Messages = getMessages(isWelsh = true)

      implicit val document: Document = Jsoup.parse(underTest(aDeductionPeriodPage.copy(contractorName = None)).body)

      textOnPageCheck(text = s"5 Mai $taxYearEOYStart", selector = Selectors.optionsSelector(1))
      textOnPageCheck(text = s"5 Mehefin $taxYearEOYStart", Selectors.optionsSelector(2))
      textOnPageCheck(text = s"5 Gorffennaf $taxYearEOYStart", Selectors.optionsSelector(3))
      textOnPageCheck(text = s"5 Awst $taxYearEOYStart", Selectors.optionsSelector(4))
      textOnPageCheck(text = s"5 Medi $taxYearEOYStart", Selectors.optionsSelector(5))
      textOnPageCheck(text = s"5 Hydref $taxYearEOYStart", Selectors.optionsSelector(6))
      textOnPageCheck(text = s"5 Tachwedd $taxYearEOYStart", Selectors.optionsSelector(7))
      textOnPageCheck(text = s"5 Rhagfyr $taxYearEOYStart", Selectors.optionsSelector(8))
      textOnPageCheck(text = s"5 Ionawr $taxYearEOY", Selectors.optionsSelector(9))
      textOnPageCheck(text = s"5 Chwefror $taxYearEOY", Selectors.optionsSelector(10))
      textOnPageCheck(text = s"5 Mawrth $taxYearEOY", Selectors.optionsSelector(11))
      textOnPageCheck(text = s"5 Ebrill $taxYearEOY", selector = Selectors.optionsSelector(12))
    }

    "display the month options correctly in english" which {
      implicit val authRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(isAgent = false)
      implicit val messages: Messages = getMessages(isWelsh = false)

      implicit val document: Document = Jsoup.parse(underTest(aDeductionPeriodPage.copy(contractorName = None)).body)

      textOnPageCheck(text = s"5 May $taxYearEOYStart", selector = Selectors.optionsSelector(1))
      textOnPageCheck(text = s"5 June $taxYearEOYStart", Selectors.optionsSelector(2))
      textOnPageCheck(text = s"5 July $taxYearEOYStart", Selectors.optionsSelector(3))
      textOnPageCheck(text = s"5 August $taxYearEOYStart", Selectors.optionsSelector(4))
      textOnPageCheck(text = s"5 September $taxYearEOYStart", Selectors.optionsSelector(5))
      textOnPageCheck(text = s"5 October $taxYearEOYStart", Selectors.optionsSelector(6))
      textOnPageCheck(text = s"5 November $taxYearEOYStart", Selectors.optionsSelector(7))
      textOnPageCheck(text = s"5 December $taxYearEOYStart", Selectors.optionsSelector(8))
      textOnPageCheck(text = s"5 January $taxYearEOY", Selectors.optionsSelector(9))
      textOnPageCheck(text = s"5 February $taxYearEOY", Selectors.optionsSelector(10))
      textOnPageCheck(text = s"5 March $taxYearEOY", Selectors.optionsSelector(11))
      textOnPageCheck(text = s"5 April $taxYearEOY", Selectors.optionsSelector(12))
    }
  }
}
