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

import controllers.routes.MaterialsController
import forms.{FormsProvider, YesNoForm}
import models.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.MaterialsPageBuilder.aMaterialsPage
import views.html.MaterialsView

class MaterialsViewSpec extends ViewUnitTest {

  object Selectors {
    val paragraphTextSelector: String = "p.govuk-body"
    val warningSelector: String = "div.govuk-warning-text > strong.govuk-warning-text__text"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"
    val buttonSelector: String = "#continue"

    def listItemSelector(number: Int): String = s"ul.govuk-list.govuk-list--bullet > li:nth-child($number)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedListItemOne: String
    val expectedListItemTwo: String
    val expectedWarning: String
    val expectedWarningText: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedTitleThisContract: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedH1ThisContract: String
    val expectedParagraph: String
    val expectedErrorText: String

    def expectedH1(contractorName: String): String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedListItemOne: String = "bricks, piping, timber, fixings"
    override val expectedListItemTwo: String = "renting or hiring equipment or scaffolding"
    override val expectedWarning: String = "Warning"
    override val expectedWarningText: String = "This does not include work-related expenses, such as fuel, accommodation, tools or work clothing." +
      " Use your software package to tell us about these types of expenses."
    override val expectedYesText: String = "Yes"
    override val expectedNoText: String = "No"
    override val expectedButtonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedListItemOne: String = "briciau, pibellau, pren, gosodiadau"
    override val expectedListItemTwo: String = "rhentu neu logi offer neu sgaffaldwaith"
    override val expectedWarning: String = "Rhybudd"
    override val expectedWarningText: String =
      "Nid yw hyn yn cynnwys treuliau sy’n gysylltiedig â gwaith, er enghraifft tanwydd, llety, offer neu ddillad gwaith. Defnyddiwch eich pecyn meddalwedd i ddweud wrthym am y mathau hyn o dreuliau."
    override val expectedYesText: String = "Iawn"
    override val expectedNoText: String = "Na"
    override val expectedButtonText: String = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "Did you pay for materials for your contractor?"
    override val expectedTitleThisContract: String = "Did you pay for building materials on this contract?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String = s"$expectedTitle"
    override val expectedH1ThisContract: String = s"$expectedTitleThisContract"
    override val expectedParagraph: String = "Materials include things you pay for as part of a construction project, for example:"
    override val expectedErrorText: String = "Select yes if you paid for materials"

    override def expectedH1(contractorName: String): String = s"Did you pay for materials at $contractorName?"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "Did your client pay for materials for their contractor?"
    override val expectedTitleThisContract: String = "Did your client pay for building materials on this contract?"
    override val expectedErrorTitle: String = s"Error: $expectedTitle"
    override val expectedH1: String = s"$expectedTitle"
    override val expectedH1ThisContract: String = s"$expectedTitleThisContract"
    override val expectedParagraph: String = "Materials include things your client pays for as part of a construction project, for example:"
    override val expectedErrorText: String = "Select yes if your client paid for materials"

    override def expectedH1(contractorName: String): String = s"Did your client pay for materials at $contractorName?"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "A wnaethoch dalu am ddeunyddiau ar gyfer eich contractwr?"
    override val expectedTitleThisContract: String = "A wnaethoch dalu am ddeunyddiau adeiladu ar y contract hwn?"
    override val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    override val expectedH1: String = s"$expectedTitle"
    override val expectedH1ThisContract: String = s"$expectedTitleThisContract"
    override val expectedParagraph: String = "Mae deunyddiau’n cynnwys pethau rydych yn talu amdanynt fel rhan o brosiect adeiladu, er enghraifft:"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os gwnaethoch dalu am ddeunyddiau"

    override def expectedH1(contractorName: String): String = s"A wnaethoch dalu am ddeunyddiau yn $contractorName?"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "A wnaeth eich cleient dalu am ddeunyddiau ar gyfer ei gontractwr?"
    override val expectedTitleThisContract: String = "A wnaeth eich cleient dalu am ddeunyddiau adeiladu ar y contract hwn?"
    override val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    override val expectedH1: String = s"$expectedTitle"
    override val expectedH1ThisContract: String = s"$expectedTitleThisContract"
    override val expectedParagraph: String = "Mae deunyddiau’n cynnwys pethau y mae’ch cleient yn talu amdanynt fel rhan o brosiect adeiladu, er enghraifft:"
    override val expectedErrorText: String = "Dewiswch ‘Iawn’ os gwnaeth eich cleient dalu am ddeunyddiau"

    override def expectedH1(contractorName: String): String = s"A wnaeth eich cleient dalu am ddeunyddiau yn $contractorName?"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[MaterialsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with empty form and provided contractor and no value selected" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val pageModel = aMaterialsPage.copy(contractorName = Some("some-contractor"), employerRef = "some-ref")
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1(contractorName = "some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemOne, Selectors.listItemSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemTwo, Selectors.listItemSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedWarning + " " + userScenario.commonExpectedResults.expectedWarningText, Selectors.warningSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(MaterialsController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render pay page with filled in form and missing contractor name and selected Yes value" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsYesNoForm(userScenario.isAgent)
        val pageModel = aMaterialsPage.copy(contractorName = None, form = form.fill(value = true))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedTitleThisContract, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1ThisContract)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemOne, Selectors.listItemSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemTwo, Selectors.listItemSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedWarning + " " + userScenario.commonExpectedResults.expectedWarningText, Selectors.warningSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(MaterialsController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)
      }

      "render page with form containing empty form error" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSessionDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val form = new FormsProvider().materialsYesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> ""))
        val pageModel = aMaterialsPage.copy(contractorName = Some("some-contractor"), form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.specificExpectedResults.get.expectedH1(contractorName = "some-contractor"))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedParagraph, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemOne, Selectors.listItemSelector(number = 1))
        textOnPageCheck(userScenario.commonExpectedResults.expectedListItemTwo, Selectors.listItemSelector(number = 2))
        textOnPageCheck(userScenario.commonExpectedResults.expectedWarning + " " + userScenario.commonExpectedResults.expectedWarningText, Selectors.warningSelector)
        radioButtonCheck(userScenario.commonExpectedResults.expectedYesText, radioNumber = 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.expectedNoText, radioNumber = 2, checked = false)
        formPostLinkCheck(MaterialsController.submit(taxYearEOY, pageModel.month.toString.toLowerCase, pageModel.employerRef).url, Selectors.continueButtonFormSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector)

        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorText, Selectors.expectedErrorHref)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorText)
      }
    }
  }
}
