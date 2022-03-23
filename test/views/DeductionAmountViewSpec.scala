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

import controllers.routes.DeductionAmountController
import forms.{AmountForm, FormsProvider}
import models.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.DeductionAmountPageBuilder.aDeductionAmountPage
import utils.ViewUtils.translatedMonthAndTaxYear
import views.html.DeductionAmountView

class DeductionAmountViewSpec extends ViewUnitTest {

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

    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedCaption: Int => String
    val expectedH1: String
    val expectedP1Replay: String => String
    val expectedEmptyErrorText: String
    val expectedWrongFormatErrorText: String
    val expectedMaxAmountErrorText: String
    val expectedButtonText: String
    val expectedHintText: String

    def expectedH1(contractorName: String): String
  }

  trait SpecificExpectedResults {
    val expectedP1: String => String
    val expectedP2Replay: String => String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "How much was taken by this contractor in CIS deductions?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "How much was taken by this contractor in CIS deductions?"
    override val expectedP1Replay: String => String = (amount: String) => s"If $amount was not taken in CIS deductions, tell us the correct amount."
    override val expectedEmptyErrorText: String = "Enter the CIS deduction amount"
    override val expectedWrongFormatErrorText: String = "Enter the CIS deduction in the correct format"
    override val expectedMaxAmountErrorText: String = "The CIS deduction must be less than £100,000,000,000"
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"

    override def expectedH1(contractorName: String): String = s"How much was taken by $contractorName in CIS deductions?"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "How much was taken by this contractor in CIS deductions?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "How much was taken by this contractor in CIS deductions?"
    override val expectedP1Replay: String => String = (amount: String) => s"If $amount was not taken in CIS deductions, tell us the correct amount."
    override val expectedEmptyErrorText: String = "Enter the CIS deduction amount"
    override val expectedWrongFormatErrorText: String = "Enter the CIS deduction in the correct format"
    override val expectedMaxAmountErrorText: String = "The CIS deduction must be less than £100,000,000,000"
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"

    override def expectedH1(contractorName: String): String = s"How much was taken by $contractorName in CIS deductions?"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your $statementDate CIS statement."
    override val expectedP2Replay: String => String = (statementDate: String) => s"You can find this on your $statementDate CIS statement."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your client’s $statementDate CIS statement."
    override val expectedP2Replay: String => String = (statementDate: String) => s"You can find this on your client’s $statementDate CIS statement."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your $statementDate CIS statement."
    override val expectedP2Replay: String => String = (statementDate: String) => s"You can find this on your $statementDate CIS statement."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your client’s $statementDate CIS statement."
    override val expectedP2Replay: String => String = (statementDate: String) => s"You can find this on your client’s $statementDate CIS statement."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[DeductionAmountView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDeductionAmountPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref")
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedH1(contractorName = "some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "")
        formPostLinkCheck(DeductionAmountController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render pay page with filled in form" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().deductionAmountForm()
        val pageModel = aDeductionAmountPage.copy(contractorName = None, form = form.fill(value = 123.01), originalAmount = Some(123.01))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedH1)
        textOnPageCheck(userScenario.commonExpectedResults.expectedP1Replay("£123.01"), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP2Replay("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "123.01")
        formPostLinkCheck(DeductionAmountController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().deductionAmountForm().bind(Map("amount" -> ""))
        val pageModel = aDeductionAmountPage.copy(contractorName = Some("some-contractor"), form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedH1(contractorName = "some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        errorSummaryCheck(userScenario.commonExpectedResults.expectedEmptyErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "")
        formPostLinkCheck(DeductionAmountController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing wrong format form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().deductionAmountForm().bind(Map("amount" -> "wrong-format"))
        val pageModel = aDeductionAmountPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle)
        errorSummaryCheck(userScenario.commonExpectedResults.expectedWrongFormatErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "wrong-format")
      }

      "render page with form containing max amount form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().deductionAmountForm().bind(Map("amount" -> "100,000,000,000"))
        val pageModel = aDeductionAmountPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle)
        errorSummaryCheck(userScenario.commonExpectedResults.expectedMaxAmountErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "100,000,000,000")
      }
    }
  }
}
