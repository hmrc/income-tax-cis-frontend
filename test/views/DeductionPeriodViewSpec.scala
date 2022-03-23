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

import forms.DeductionPeriodFormProvider
import models.AuthorisationRequest
import models.forms.DeductionPeriod
import models.pages.DeductionPeriodPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.cis.DeductionPeriodView

class DeductionPeriodViewSpec extends ViewUnitTest {

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
    val expectedErrorTitle: String
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
    override val expectedLabel: String = "Tax month ending"
    override val expectedHeading: String = "When did Michele Lamy Paving Ltd make CIS deductions?"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your CIS statement."
    override val expectedError: String = "You cannot select a date you already have information for"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your CIS statement."
    override val expectedError: String = "You cannot select a date you already have information for"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your client’s contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your client’s contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your client’s CIS statement."
    override val expectedError: String = "You cannot select a date your client already has information for"
    override val expectedButtonText: String = "Continue"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "When did your client’s contractor make CIS deductions?"
    override val expectedErrorTitle: String = "Error: When did your client’s contractor make CIS deductions?"
    override val expectedP1: String = "Tell us the end date on your client’s CIS statement."
    override val expectedError: String = "You cannot select a date your client already has information for"
    override val expectedButtonText: String = "Continue"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val pageModel = DeductionPeriodPage(Some("Michele Lamy Paving Ltd"),"111/11111",taxYearEOY,None,Seq())

  private lazy val underTest = inject[DeductionPeriodView]

  private def formProvider = new DeductionPeriodFormProvider()
  private def form(isAgent: Boolean): Form[DeductionPeriod] = formProvider.deductionPeriodForm(isAgent, pageModel.priorSubmittedPeriods)

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the deduction period page when at the end of the year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(pageModel,form(userScenario.isAgent)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedHeading)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedLabel, Selectors.labelSelector)
        buttonCheck(userScenario.specificExpectedResults.get.expectedButtonText, Selectors.buttonSelector)
      }

      "render the deduction period page with the correct error when at the end of the year" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        def formError(isAgent: Boolean): FormError = FormError("month", formProvider.error(isAgent))

        implicit val document: Document = Jsoup.parse(underTest(pageModel,form(userScenario.isAgent).withError(formError(userScenario.isAgent))).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
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
      implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(false)
      implicit val messages: Messages = getMessages(true)

      implicit val document: Document = Jsoup.parse(underTest(pageModel.copy(contractorName = None),form(false)).body)

      h1Check("When did Contractor: 111/11111 make CIS deductions?")
      textOnPageCheck("5 Mai 2020", Selectors.optionsSelector(1))
      textOnPageCheck("5 Mehefin 2020", Selectors.optionsSelector(2))
      textOnPageCheck("5 Gorffennaf 2020", Selectors.optionsSelector(3))
      textOnPageCheck("5 Awst 2020", Selectors.optionsSelector(4))
      textOnPageCheck("5 Medi 2020", Selectors.optionsSelector(5))
      textOnPageCheck("5 Hydref 2020", Selectors.optionsSelector(6))
      textOnPageCheck("5 Tachwedd 2020", Selectors.optionsSelector(7))
      textOnPageCheck("5 Rhagfyr 2020", Selectors.optionsSelector(8))
      textOnPageCheck("5 Ionawr 2021", Selectors.optionsSelector(9))
      textOnPageCheck("5 Chwefror 2021", Selectors.optionsSelector(10))
      textOnPageCheck("5 Mawrth 2021", Selectors.optionsSelector(11))
      textOnPageCheck("5 Ebrill 2021", Selectors.optionsSelector(12))
    }
    "display the month options correctly in english" which {
      implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(false)
      implicit val messages: Messages = getMessages(false)

      implicit val document: Document = Jsoup.parse(underTest(pageModel.copy(contractorName=None),form(false)).body)

      h1Check("When did Contractor: 111/11111 make CIS deductions?")
      textOnPageCheck("5 May 2020", Selectors.optionsSelector(1))
      textOnPageCheck("5 June 2020", Selectors.optionsSelector(2))
      textOnPageCheck("5 July 2020", Selectors.optionsSelector(3))
      textOnPageCheck("5 August 2020", Selectors.optionsSelector(4))
      textOnPageCheck("5 September 2020", Selectors.optionsSelector(5))
      textOnPageCheck("5 October 2020", Selectors.optionsSelector(6))
      textOnPageCheck("5 November 2020", Selectors.optionsSelector(7))
      textOnPageCheck("5 December 2020", Selectors.optionsSelector(8))
      textOnPageCheck("5 January 2021", Selectors.optionsSelector(9))
      textOnPageCheck("5 February 2021", Selectors.optionsSelector(10))
      textOnPageCheck("5 March 2021", Selectors.optionsSelector(11))
      textOnPageCheck("5 April 2021", Selectors.optionsSelector(12))
    }
  }
}