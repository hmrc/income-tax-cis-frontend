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

import forms.{AmountForm, FormsProvider}
import models.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.LabourPayPageBuilder.aLabourPayPage
import utils.ViewUtils.translatedMonthAndTaxYear
import views.html.LabourPayView

class LabourPayViewSpec extends ViewUnitTest {

  object Selectors {

    val continueButtonFormSelector = "#main-content > div > div > form"
    val hintTextSelector: String = "#amount-hint"
    val poundPrefixSelector: String = ".govuk-input__prefix"
    val expectedErrorHref = "#amount"
    val inputAmountField: String = "#amount"
    val buttonSelector: String = "#continue"

    def paragraphTextSelector(number: Int): String = s"p.govuk-body:nth-child(${number + 1})"
  }

  trait CommonExpectedResults {

    val expectedCaption: Int => String
    val expectedNoVATParagraph: String
    val expectedButtonText: String
    val expectedHintText: String
  }

  trait SpecificExpectedResults {

    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String => String
    val expectedP1: String => String
    val expectedP1Replay: String => String
    val expectedP2Replay: String => String
    val expectedEmptyErrorText: String
    val expectedWrongFormatErrorText: String
    val expectedMaxAmountErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedNoVATParagraph: String = "Do not include VAT or cost of materials."
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedNoVATParagraph: String = "Do not include VAT or cost of materials."
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did your contractor pay you for labour?"
    override val expectedErrorTitle: String = "Error: How much did your contractor pay you for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay you for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (amount: String) => s"If you were not paid $amount for labour, tell us the correct amount."
    override val expectedP2Replay: String => String = (statementDate: String) => s"It’s the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount you were paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount you were paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount you were paid for labour must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did the contractor pay your client for labour?"
    override val expectedErrorTitle: String = "Error: How much did the contractor pay your client for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay your client for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your client’s $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (amount: String) => s"If your client was not paid $amount for labour, tell us the correct amount."
    override val expectedP2Replay: String => String = (statementDate: String) => s"It’s the amount on their $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount your client was paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount your client was paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount your client was paid for labour must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did your contractor pay you for labour?"
    override val expectedErrorTitle: String = "Error: How much did your contractor pay you for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay you for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (amount: String) => s"If you were not paid $amount for labour, tell us the correct amount."
    override val expectedP2Replay: String => String = (statementDate: String) => s"It’s the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount you were paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount you were paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount you were paid for labour must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did the contractor pay your client for labour?"
    override val expectedErrorTitle: String = "Error: How much did the contractor pay your client for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay your client for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your client’s $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (amount: String) => s"If your client was not paid $amount for labour, tell us the correct amount."
    override val expectedP2Replay: String => String = (statementDate: String) => s"It’s the amount on their $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount your client was paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount your client was paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount your client was paid for labour must be less than £100,000,000,000"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[LabourPayView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render Labour pay page with empty form" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLabourPayPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref")
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1("some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedNoVATParagraph, Selectors.paragraphTextSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "")
        formPostLinkCheck(controllers.routes.LabourPayController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render Labour pay page with filled in form" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent)
        val pageModel = aLabourPayPage.copy(form = form.fill(value = 123.01), originalGrossAmount = Some(123.01))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1Replay("£123.01"), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2Replay("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedNoVATParagraph, Selectors.paragraphTextSelector(number = 3))
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "123.01")
      }

      "render Labour pay page with form containing empty form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> ""))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedEmptyErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "")
      }

      "render Labour pay page with form containing wrong format form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> "wrong-format"))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "wrong-format")
      }

      "render Labour pay page with form containing max amount form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> "100,000,000,000"))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedMaxAmountErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "100,000,000,000")
      }
    }
  }
}
