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
    val expectedTitleNoContractorName: String
    val expectedErrorTitle: String
    val expectedH1: String => String
    val expectedH1NoContractorName: String
    val expectedP1: String => String
    val expectedP1Replay: String => String
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
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedNoVATParagraph: String = "Peidiwch â chynnwys TAW na chost deunyddiau."
    override val expectedButtonText: String = "Yn eich blaen"
    override val expectedHintText: String = "Er enghraifft, £193.52"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did your contractor pay you for labour?"
    override val expectedTitleNoContractorName: String = "How much did this contractor pay you for labour?"
    override val expectedErrorTitle: String = "Error: How much did your contractor pay you for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay you for labour?"
    override val expectedH1NoContractorName: String = "How much did this contractor pay you for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (statementDate: String) => s"It’s the amount on your $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount you were paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount you were paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount you were paid for labour must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "How much did the contractor pay your client for labour?"
    override val expectedTitleNoContractorName: String = "How much did this contractor pay your client for labour?"
    override val expectedErrorTitle: String = "Error: How much did the contractor pay your client for labour?"
    override val expectedH1: String => String = (contractor: String) => s"How much did $contractor pay your client for labour?"
    override val expectedH1NoContractorName: String = "How much did this contractor pay your client for labour?"
    override val expectedP1: String => String = (statementDate: String) => s"Tell us the amount on your client’s $statementDate CIS statement, before any deductions were made."
    override val expectedP1Replay: String => String = (statementDate: String) => s"It’s the amount on their $statementDate CIS statement, before any deductions were made."
    override val expectedEmptyErrorText: String = "Enter the amount your client was paid for labour"
    override val expectedWrongFormatErrorText: String = "Enter the amount your client was paid for labour in the correct format"
    override val expectedMaxAmountErrorText: String = "The amount your client was paid for labour must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Faint gwnaeth eich contractwr eich dalu am lafur?"
    override val expectedTitleNoContractorName: String = "Faint gwnaeth y contractwr hwn eich dalu am lafur?"
    override val expectedErrorTitle: String = "Gwall: Faint gwnaeth eich contractwr eich dalu am lafur?"
    override val expectedH1: String => String = (contractor: String) => s"Faint gwnaeth $contractor eich dalu am lafur?"
    override val expectedH1NoContractorName: String = "Faint gwnaeth y contractwr hwn eich dalu am lafur?"
    override val expectedP1: String => String = (statementDate: String) => s"Rhowch wybod i ni y swm ar eich datganiad CIS $statementDate, cyn i unrhyw ddidyniadau gael eu gwneud."
    override val expectedP1Replay: String => String = (statementDate: String) => s"Dyma’r swm ar eich datganiad CIS $statementDate, cyn i unrhyw ddidyniadau gael eu gwneud."
    override val expectedEmptyErrorText: String = "Nodwch swm a dalwyd i chi am lafur"
    override val expectedWrongFormatErrorText: String = "Nodwch y swm a dalwyd i chi am lafur yn y fformat cywir"
    override val expectedMaxAmountErrorText: String = "Mae’n rhaid i’r swm a dalwyd i chi am lafur fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Faint gwnaeth y contractwr dalu’ch cleient am lafur?"
    override val expectedTitleNoContractorName: String = "Faint gwnaeth y contractwr hwn dalu’ch cleient am lafur?"
    override val expectedErrorTitle: String = "Gwall: Faint gwnaeth y contractwr dalu’ch cleient am lafur?"
    override val expectedH1: String => String = (contractor: String) => s"Faint gwnaeth $contractor ei dalu i’ch cleient am lafur?"
    override val expectedH1NoContractorName: String = "Faint gwnaeth y contractwr hwn ei dalu i’ch cleient am lafur?"
    override val expectedP1: String => String = (statementDate: String) => s"Rhowch wybod i ni y swm ar ddatganiad CIS $statementDate eich cleient, cyn i unrhyw ddidyniadau gael eu gwneud."
    override val expectedP1Replay: String => String = (statementDate: String) => s"Dyma’r swm ar ei ddatganiad CIS $statementDate, cyn i unrhyw ddidyniadau gael eu gwneud."
    override val expectedEmptyErrorText: String = "Nodwch y swm a dalwyd i’ch cleient am lafur"
    override val expectedWrongFormatErrorText: String = "Nodwch y swm a dalwyd i’ch cleient am lafur yn y fformat cywir"
    override val expectedMaxAmountErrorText: String = "Mae’n rhaid i’r swm a dalwyd i’ch cleient am lafur fod yn llai na £100,000,000,000"
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
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
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

      "render the labour pay page with an alternative title and H1 when contractor name is None" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aLabourPayPage.copy(contractorName = None, employerRef = "some-ref", originalGrossAmount = None)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleNoContractorName, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1NoContractorName)
      }

      "render Labour pay page with filled in form" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent)
        val pageModel = aLabourPayPage.copy(form = form.fill(value = 123.01), originalGrossAmount = Some(123.01))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1Replay("5 " + translatedMonthAndTaxYear(pageModel.month, taxYearEOY)), Selectors.paragraphTextSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedNoVATParagraph, Selectors.paragraphTextSelector(number = 2))
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "123.01")
      }

      "render Labour pay page with form containing empty form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> ""))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedEmptyErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "")
      }

      "render Labour pay page with form containing wrong format form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> "wrong-format"))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedWrongFormatErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "wrong-format")
      }

      "render Labour pay page with form containing max amount form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().labourPayAmountForm(userScenario.isAgent).bind(Map("amount" -> "100,000,000,000"))
        val pageModel = aLabourPayPage.copy(form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedMaxAmountErrorText, Selectors.expectedErrorHref)
        inputFieldValueCheck(AmountForm.amount, Selectors.inputAmountField, value = "100,000,000,000")
      }
    }
  }
}
