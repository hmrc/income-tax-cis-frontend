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

import forms.ContractorDetailsForm._
import models.AuthorisationRequest
import models.forms.ContractorDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.ContractorDetailsPageBuilder.aContractorDetailsPage
import views.html.ContractorDetailsView

// TODO: Tests are missing for error messages
class ContractorDetailsViewSpec extends ViewUnitTest {

  object Selectors {
    val contractorNameFieldHead = "#main-content > div > div > form > div:nth-child(1) > label"
    val contractorNameFieldHint = "#contractorName-hint"
    val contractorNameFieldBox = "#contractorName"
    val ernFieldHead = "#main-content > div > div > form > div:nth-child(2) > label"
    val ernFieldHint = "#employerReferenceNumber-hint"
    val ernFieldBox = "#employerReferenceNumber"
    val buttonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedCaption: Int => String
    val expectedH1: String
    val contractorName: String
    val contractorNameHint: String
    val employerRef: String
    val employerRefHint: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedP1: String
    val expectedInsetText: Int => String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "Contractor details"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "CIS deductions"
    override val contractorName: String = "Contractor name"
    override val contractorNameHint: String = "For example, ABC Steelworks."
    override val employerRef: String = "Employer Reference Number (ERN)"
    override val employerRefHint: String = "For example, 123/AB12345."
    override val buttonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "Contractor details"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "CIS deductions"
    override val contractorName: String = "Contractor name"
    override val contractorNameHint: String = "For example, ABC Steelworks."
    override val employerRef: String = "Employer Reference Number (ERN)"
    override val employerRefHint: String = "For example, 123/AB12345."
    override val buttonText: String = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedP1: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedP1: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedP1: String = "Your client???s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client???s CIS information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedP1: String = "Your client???s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client???s CIS information until 6 April $taxYear."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[ContractorDetailsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render end of year version of contractor details page" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val form = contractorDetailsForm(userScenario.isAgent, Seq.empty)
        val pageModel = aContractorDetailsPage.copy(isAgent = userScenario.isAgent, form = form)
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        textOnPageCheck(userScenario.commonExpectedResults.contractorName, Selectors.contractorNameFieldHead)
        hintTextCheck(userScenario.commonExpectedResults.contractorNameHint, Selectors.contractorNameFieldHint)
        inputFieldValueCheck("contractorName", Selectors.contractorNameFieldBox, "")
        textOnPageCheck(userScenario.commonExpectedResults.employerRef, Selectors.ernFieldHead)
        hintTextCheck(userScenario.commonExpectedResults.employerRefHint, Selectors.ernFieldHint)
        inputFieldValueCheck("employerReferenceNumber", Selectors.ernFieldBox, "")
        formPostLinkCheck(controllers.routes.ContractorDetailsController.submit(taxYearEOY, None).url, Selectors.formSelector)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector)

      }

      "render end of year version of contractor details page with a pre-filled form" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val form = contractorDetailsForm(userScenario.isAgent, Seq.empty).fill(ContractorDetails("ABC Steelworks", "123/AB12345"))
        val pageModel = aContractorDetailsPage.copy(isAgent = userScenario.isAgent, form = form, originalEmployerRef = Some("123/AB12345"))
        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        textOnPageCheck(userScenario.commonExpectedResults.contractorName, Selectors.contractorNameFieldHead)
        hintTextCheck(userScenario.commonExpectedResults.contractorNameHint, Selectors.contractorNameFieldHint)
        inputFieldValueCheck("contractorName", Selectors.contractorNameFieldBox, "ABC Steelworks")
        textOnPageCheck(userScenario.commonExpectedResults.employerRef, Selectors.ernFieldHead)
        hintTextCheck(userScenario.commonExpectedResults.employerRefHint, Selectors.ernFieldHint)
        inputFieldValueCheck("employerReferenceNumber", Selectors.ernFieldBox, "123/AB12345")
        formPostLinkCheck(controllers.routes.ContractorDetailsController.submit(taxYearEOY, Some("123/AB12345")).url, Selectors.formSelector)
        buttonCheck(userScenario.commonExpectedResults.buttonText, Selectors.buttonSelector)
      }
    }
  }
}
