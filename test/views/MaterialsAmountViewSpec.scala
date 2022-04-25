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

import controllers.routes.MaterialsAmountController
import forms.{AmountForm, FormsProvider}
import models.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.MaterialsAmountPageBuilder.aMaterialsAmountPage
import utils.ViewUtils.translatedMonthAndTaxYear
import views.html.MaterialsAmountView

import java.time.Month

class MaterialsAmountViewSpec extends ViewUnitTest {

  object Selectors {
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector: String = ".govuk-input__prefix"
    val inputFieldSelector: String = "#amount"
    val expectedErrorHref = "#amount"
    val buttonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"

    def paragraphTextSelector(number: Int): String = s"p.govuk-body:nth-child(${number + 1})"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedHintText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String => String
    val expectedReplayContent1: String => String
    val expectedReplayContent2: String => String
    val expectedTellUsTheAmountText: String => String
    val expectedOnlyIncludeVATParagraph: String
    val expectedErrorNoEntry: String
    val expectedErrorIncorrectFormat: String
    val expectedErrorOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedButtonText: String = "Continue"
    override val expectedHintText: String = "For example, £193.52"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did you pay for building materials for your contractor?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String => String = (contractorName: String) => s"How much did you pay for building materials at $contractorName?"
    override val expectedReplayContent1: String => String = (amount: String) => s"If you did not pay $amount for materials, tell us the correct amount."
    override val expectedReplayContent2: String => String = (statementDate: String) => s"You can find this on your 5 $statementDate CIS statement."
    override val expectedTellUsTheAmountText: String => String = (statementDate: String) => s"Tell us the amount on your 5 $statementDate CIS statement."
    override val expectedOnlyIncludeVATParagraph: String = "Only include VAT if you are not VAT registered."
    override val expectedErrorNoEntry: String = "Enter the amount you paid for materials"
    override val expectedErrorIncorrectFormat: String = "Enter the amount you paid for materials in the correct format"
    override val expectedErrorOverMaximum: String = "The amount you paid for materials must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did you pay for building materials for your contractor?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String => String = (contractorName: String) => s"How much did you pay for building materials at $contractorName?"
    override val expectedReplayContent1: String => String = (amount: String) => s"If you did not pay $amount for materials, tell us the correct amount."
    override val expectedReplayContent2: String => String = (statementDate: String) => s"You can find this on your 5 $statementDate CIS statement."
    override val expectedTellUsTheAmountText: String => String = (statementDate: String) => s"Tell us the amount on your 5 $statementDate CIS statement."
    override val expectedOnlyIncludeVATParagraph: String = "Only include VAT if you are not VAT registered."
    override val expectedErrorNoEntry: String = "Enter the amount you paid for materials"
    override val expectedErrorIncorrectFormat: String = "Enter the amount you paid for materials in the correct format"
    override val expectedErrorOverMaximum: String = "The amount you paid for materials must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did your client pay for building materials for the contractor?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String => String = (contractorName: String) => s"How much did your client pay for building materials at $contractorName?"
    override val expectedReplayContent1: String => String = (amount: String) => s"If your client did not pay $amount for materials, tell us the correct amount."
    override val expectedReplayContent2: String => String = (statementDate: String) => s"You can find this on their 5 $statementDate CIS statement."
    override val expectedTellUsTheAmountText: String => String = (statementDate: String) => s"Tell us the amount on your client’s 5 $statementDate CIS statement."
    override val expectedOnlyIncludeVATParagraph: String = "Only include VAT if your client is not VAT registered."
    override val expectedErrorNoEntry: String = "Enter the amount your client paid for materials"
    override val expectedErrorIncorrectFormat: String = "Enter the amount your client paid for materials in the correct format"
    override val expectedErrorOverMaximum: String = "The amount your client paid for materials must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did your client pay for building materials for the contractor?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String => String = (contractorName: String) => s"How much did your client pay for building materials at $contractorName?"
    override val expectedReplayContent1: String => String = (amount: String) => s"If your client did not pay $amount for materials, tell us the correct amount."
    override val expectedReplayContent2: String => String = (statementDate: String) => s"You can find this on their 5 $statementDate CIS statement."
    override val expectedTellUsTheAmountText: String => String = (statementDate: String) => s"Tell us the amount on your client’s 5 $statementDate CIS statement."
    override val expectedOnlyIncludeVATParagraph: String = "Only include VAT if your client is not VAT registered."
    override val expectedErrorNoEntry: String = "Enter the amount your client paid for materials"
    override val expectedErrorIncorrectFormat: String = "Enter the amount your client paid for materials in the correct format"
    override val expectedErrorOverMaximum: String = "The amount your client paid for materials must be less than £100,000,000,000"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest: MaterialsAmountView = app.injector.instanceOf[MaterialsAmountView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the materials amount page with an empty amount" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aMaterialsAmountPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref", originalAmount = None)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1("some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTellUsTheAmountText(translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedOnlyIncludeVATParagraph, Selectors.paragraphTextSelector(number = 2))
        hintTextCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputFieldSelector, value = "")
        formPostLinkCheck(MaterialsAmountController.submit(taxYearEOY, Month.JUNE.toString.toLowerCase, "some-ref").url, Selectors.formSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render the materials amount page with non empty amount" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsAmountForm(isAgent = userScenario.isAgent).fill(value = 333.33)
        val pageModel = aMaterialsAmountPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref", form = form, originalAmount = Some(333.33))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1("some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayContent1("£333.33"), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedReplayContent2(translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedOnlyIncludeVATParagraph, Selectors.paragraphTextSelector(number = 3))
        hintTextCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputFieldSelector, value = "333.33")
        formPostLinkCheck(MaterialsAmountController.submit(taxYearEOY, Month.JUNE.toString.toLowerCase, contractor = "some-ref").url, Selectors.formSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsAmountForm(isAgent = userScenario.isAgent).bind(Map(AmountForm.amount -> ""))
        val pageModel = aMaterialsAmountPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref", form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1("some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTellUsTheAmountText(translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedOnlyIncludeVATParagraph, Selectors.paragraphTextSelector(number = 3))
        hintTextCheck(userScenario.commonExpectedResults.expectedHintText, Selectors.hintTextSelector)
        textOnPageCheck(text = "£", Selectors.poundPrefixSelector)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorNoEntry, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputFieldSelector, value = "")
        formPostLinkCheck(MaterialsAmountController.submit(taxYearEOY, Month.JUNE.toString.toLowerCase, contractor = "some-ref").url, Selectors.formSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing wrong format form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsAmountForm(isAgent = userScenario.isAgent).bind(Map(AmountForm.amount -> "wrong-format"))
        val pageModel = aMaterialsAmountPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorIncorrectFormat, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputFieldSelector, value = "wrong-format")
      }

      "render page with form containing max amount form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsAmountForm(isAgent = userScenario.isAgent).bind(Map(AmountForm.amount -> "100,000,000,000"))
        val pageModel = aMaterialsAmountPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorOverMaximum, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputFieldSelector, value = "100,000,000,000")
      }
    }
  }
}
