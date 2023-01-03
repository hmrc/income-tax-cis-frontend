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

import controllers.routes.DeductionsFromPaymentsController
import forms.{FormsProvider, YesNoForm}
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.DeductionsFromPaymentsPageBuilder.aDeductionsFromPaymentsPage
import views.html.DeductionsFromPaymentsView

class DeductionsFromPaymentsViewSpec extends ViewUnitTest {

  object Selectors {
    val paragraphTextSelector: String = "p.govuk-body"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitleText: String
    val expectedParagraphText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitleText: String
    val expectedErrorTitleText: String
    val expectedParagraphText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitleText: String = "Have you had CIS deductions taken from your payments by contractors?"
    override val expectedParagraphText: String = "You’ll find the deductions on statements that contractors gave you."
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedTitleText: String = "A ydych wedi cael didyniadau CIS a gymerwyd o’ch taliadau gan gontractwyr?"
    override val expectedParagraphText: String = "Cewch hyd i’r didyniadau mewn datganiadau a roddwyd i chi gan gontractwyr."
    override val expectedYesText: String = "Iawn"
    override val expectedNoText: String = "Na"
    override val expectedButtonText: String = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Have you had CIS deductions taken from your payments by contractors?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedParagraphText: String = "You’ll find the deductions on statements that contractors gave you."
    override val expectedErrorText: String = "Select yes if you had CIS deductions taken from your payments by contractors."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitleText: String = "Has your client had CIS deductions taken from their payments by contractors?"
    override val expectedErrorTitleText: String = s"Error: $expectedTitleText"
    override val expectedParagraphText: String = "You’ll find the deductions on statements that contractors gave your client."
    override val expectedErrorText: String = "Select yes if your client had CIS deductions taken from their payments by contractors."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A ydych wedi cael didyniadau CIS a gymerwyd o’ch taliadau gan gontractwyr?"
    override val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    override val expectedParagraphText: String = "Cewch hyd i’r didyniadau mewn datganiadau a roddwyd i chi gan gontractwyr."
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os oedd gennych ddidyniadau CIS wedi'u cymryd o'ch taliadau gan gontractwyr."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitleText: String = "A yw’ch cleient wedi cael didyniadau CIS o’i daliadau gan gontractwyr?"
    override val expectedErrorTitleText: String = s"Gwall: $expectedTitleText"
    override val expectedParagraphText: String = "Cewch hyd i’r didyniadau mewn datganiadau a roddwyd i’ch cleient gan gontractwyr."
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os oedd gan eich cleient ddidyniadau CIS wedi'u cymryd o'i daliadau gan gontractwyr."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[DeductionsFromPaymentsView]

  userScenarios.foreach { userScenario =>

    val form = new FormsProvider().deductionsFromPaymentsForm(userScenario.isAgent)

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and no value selected" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDeductionsFromPaymentsPage.copy()
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, Selectors.paragraphTextSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(DeductionsFromPaymentsController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with filled in form using selected 'Yes' value" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDeductionsFromPaymentsPage.copy(form = form.fill(value = true))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedTitleText)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, Selectors.paragraphTextSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(DeductionsFromPaymentsController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aDeductionsFromPaymentsPage.copy(form = form.bind(Map(YesNoForm.yesNo -> "")))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitleText, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraphText, Selectors.paragraphTextSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(DeductionsFromPaymentsController.submit(taxYearEOY).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
