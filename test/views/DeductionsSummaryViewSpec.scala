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

import controllers.routes.ContractorSummaryController
import models.UserPriorDataRequest
import models.pages.DeductionsSummaryPage
import models.pages.elements.ContractorDeductionToDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.DeductionsSummaryView

class DeductionsSummaryViewSpec extends ViewUnitTest {

  object Selectors {
    val paragraphTextSelector = "#main-content > div > div > p:nth-child(2)"
    val addContractorSelector = "#add-contractor"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val tableCaption = ".govuk-table__caption"
    val tableHeadContractor = ".govuk-table__head > tr:nth-child(1) > th:nth-child(1)"
    var tableHeadDeductionsToDate = "th.govuk-table__header:nth-child(2)"
    val buttonSelector = "#return-to-overview-button-id"

    def contractorEmployerRef(rowId: Int): String = s"tr.govuk-table__row:nth-child($rowId) > th:nth-child(1) > a:nth-child(1)"

    def contractorDeductions(rowId: Int): String = s"tr.govuk-table__row:nth-child($rowId) > td:nth-child(2)"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedCaption: Int => String
    val expectedH1: String
    val expectedTableCaption: String
    val expectedTableHeadContractor: String
    val expectedTableHeadDeductionsToDate: String
    val expectedAddContractorLink: String
    val expectedAddAnotherContractorLink: String
    val expectedTableRowEmployerRef: String => String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedP1: String
    val expectedInsetText: Int => String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "CIS deductions"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "CIS deductions"
    override val expectedTableCaption: String = "Contractors and CIS deductions to date"
    override val expectedTableHeadContractor: String = "Contractor"
    override val expectedTableHeadDeductionsToDate: String = "CIS deductions to date"
    override val expectedTableRowEmployerRef: String => String = (employerRef: String) => s"Contractor: $employerRef"
    override val expectedButtonText: String = "Return to overview"
    val expectedAddContractorLink: String = "Add a contractor"
    val expectedAddAnotherContractorLink: String = "Add another contractor"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "CIS deductions"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedH1: String = "CIS deductions"
    override val expectedTableCaption: String = "Contractors and CIS deductions to date"
    override val expectedTableHeadContractor: String = "Contractor"
    override val expectedTableHeadDeductionsToDate: String = "CIS deductions to date"
    override val expectedTableRowEmployerRef: String => String = (employerRef: String) => s"Contractor: $employerRef"
    override val expectedButtonText: String = "Return to overview"
    val expectedAddContractorLink: String = "Add a contractor"
    val expectedAddAnotherContractorLink: String = "Add another contractor"
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
    override val expectedP1: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client’s CIS information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedP1: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client’s CIS information until 6 April $taxYear."
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val deductions = Seq(
    ContractorDeductionToDate(Some("Contractor-1"), "ref-1", Some(123.23)),
    ContractorDeductionToDate(None, "ref-2", Some(123.24)),
    ContractorDeductionToDate(Some("Contractor-3"), "ref-3", None),
  )

  private val pageModel = DeductionsSummaryPage(taxYear, isInYear = true, deductions)

  private lazy val underTest = inject[DeductionsSummaryView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render in year version of deduction summary page for a user with contractor cis deductions" which {
        implicit val authRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
        h1Check(userScenario.commonExpectedResults.expectedH1)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(taxYear), Selectors.insetTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableCaption, Selectors.tableCaption)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableHeadContractor, Selectors.tableHeadContractor)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableHeadDeductionsToDate, Selectors.tableHeadDeductionsToDate)
        linkCheck(text = "Contractor-1", selector = Selectors.contractorEmployerRef(rowId = 1),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-1").url, additionalTestText = "first column")
        textOnPageCheck(text = "£123.23", selector = Selectors.contractorDeductions(rowId = 1), additionalTestText = "second column")
        linkCheck(userScenario.commonExpectedResults.expectedTableRowEmployerRef("ref-2"), Selectors.contractorEmployerRef(rowId = 2),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-2").url, additionalTestText = "first column")
        textOnPageCheck(text = "£123.24", selector = Selectors.contractorDeductions(rowId = 2), additionalTestText = "second column")
        linkCheck(text = "Contractor-3", Selectors.contractorEmployerRef(rowId = 3),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-3").url, additionalTestText = "first column")
        textOnPageCheck(text = "", selector = Selectors.contractorDeductions(rowId = 3), additionalTestText = "second column")
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }

      "render end of year version of deduction summary page" which {
        implicit val authRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(pageModel.copy(isInYear = false)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
        h1Check(userScenario.commonExpectedResults.expectedH1)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableCaption, Selectors.tableCaption)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableHeadContractor, Selectors.tableHeadContractor)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTableHeadDeductionsToDate, Selectors.tableHeadDeductionsToDate)
        linkCheck(userScenario.commonExpectedResults.expectedAddAnotherContractorLink, Selectors.addContractorSelector, controllers.routes.ContractorDetailsController.show(taxYear, None).url)
        linkCheck(text = "Contractor-1", selector = Selectors.contractorEmployerRef(rowId = 1),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-1").url, additionalTestText = "first column")
        textOnPageCheck(text = "£123.23", selector = Selectors.contractorDeductions(rowId = 1), additionalTestText = "second column")
        linkCheck(userScenario.commonExpectedResults.expectedTableRowEmployerRef("ref-2"), Selectors.contractorEmployerRef(rowId = 2),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-2").url, additionalTestText = "first column")
        textOnPageCheck(text = "£123.24", selector = Selectors.contractorDeductions(rowId = 2), additionalTestText = "second column")
        linkCheck(text = "Contractor-3", Selectors.contractorEmployerRef(rowId = 3),
          href = ContractorSummaryController.show(taxYear, contractor = "ref-3").url, additionalTestText = "first column")
        textOnPageCheck(text = "", selector = Selectors.contractorDeductions(rowId = 3), additionalTestText = "second column")
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }

      "render in year version of deduction summary page when no cis deductions" which {
        implicit val authRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(pageModel.copy(isInYear = true, deductions = Seq.empty)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
        h1Check(userScenario.commonExpectedResults.expectedH1)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(taxYear), Selectors.insetTextSelector)
        elementNotOnPageCheck(Selectors.addContractorSelector)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }

      "render end of year version of deduction summary page when no cis deductions" which {
        implicit val authRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        implicit val document: Document = Jsoup.parse(underTest(pageModel.copy(isInYear = false, deductions = Seq.empty)).body)

        welshToggleCheck(userScenario.isWelsh)
        titleCheck(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
        h1Check(userScenario.commonExpectedResults.expectedH1)
        linkCheck(userScenario.commonExpectedResults.expectedAddContractorLink, Selectors.addContractorSelector, controllers.routes.ContractorDetailsController.show(taxYear, None).url)
        buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }
}
