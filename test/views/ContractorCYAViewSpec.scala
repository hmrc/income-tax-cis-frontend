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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import support.builders.models.pages.ContractorCYAPageBuilder.aContractorCYAPage
import utils.ViewUtils.translatedMonthAndTaxYear
import views.html.ContractorCYAView

class ContractorCYAViewSpec extends ViewUnitTest {

  object Selectors {

    val paragraphTextSelector = "#main-content > div > div > p.govuk-body"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#return-to-contractor-button-id"

    def summaryListLabel(rowId: Int): String = s"div.govuk-summary-list__row:nth-child($rowId) > dt:nth-child(1)"

    def summaryListValue(rowId: Int): String = s"div.govuk-summary-list__row:nth-child($rowId) > dd:nth-child(2)"

    def summaryListHiddenValue(rowId: Int): String = s"div.govuk-summary-list__row:nth-child($rowId) > dd:nth-child(2) > span:nth-child(1)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedContractorDetails: String
    val expectedContractorDetailsNameValue: String => String
    val expectedContractorDetailsERNValue: String => String
    val expectedEndOfTaxMonth: String
    val expectedLabour: String
    val expectedCISDeduction: String
    val expectedPaidForMaterials: String
    val expectedCostOfMaterials: String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedP1: String
    val expectedInsetText: Int => String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedContractorDetails: String = "Contractor details"
    override val expectedContractorDetailsNameValue: String => String = (contractorName: String) => s"Name: $contractorName"
    override val expectedContractorDetailsERNValue: String => String = (contractorERN: String) => s"ERN: $contractorERN"
    override val expectedEndOfTaxMonth: String = "End of tax month"
    override val expectedLabour: String = "Labour"
    override val expectedCISDeduction: String = "CIS deduction"
    override val expectedPaidForMaterials: String = "Paid for materials"
    override val expectedCostOfMaterials: String = "Cost of materials"
    override val expectedButtonText: String = "Return to contractor"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Construction Industry Scheme (CIS) deductions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedContractorDetails: String = "Contractor details"
    override val expectedContractorDetailsNameValue: String => String = (contractorName: String) => s"Name: $contractorName"
    override val expectedContractorDetailsERNValue: String => String = (contractorERN: String) => s"ERN: $contractorERN"
    override val expectedEndOfTaxMonth: String = "End of tax month"
    override val expectedLabour: String = "Labour"
    override val expectedCISDeduction: String = "CIS deduction"
    override val expectedPaidForMaterials: String = "Paid for materials"
    override val expectedCostOfMaterials: String = "Cost of materials"
    override val expectedButtonText: String = "Return to contractor"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    override val expectedTitle: String = "Check your CIS deductions"
    override val expectedH1: String = "Check your CIS deductions"
    override val expectedP1: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    override val expectedTitle: String = "Check your client’s CIS deductions"
    override val expectedH1: String = "Check your client’s CIS deductions"
    override val expectedP1: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client’s CIS information until 6 April $taxYear."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Check your CIS deductions"
    override val expectedH1: String = "Check your CIS deductions"
    override val expectedP1: String = "Your CIS deductions are based on the information we already hold about you."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your CIS information until 6 April $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Check your client’s CIS deductions"
    override val expectedH1: String = "Check your client’s CIS deductions"
    override val expectedP1: String = "Your client’s CIS deductions are based on the information we already hold about them."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"You cannot update your client’s CIS information until 6 April $taxYear."
  }


  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private val underTest = inject[ContractorCYAView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render in year version of Check your CIS deductions page" which {
        "full model" which {
          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(underTest(aContractorCYAPage).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(taxYear), Selectors.insetTextSelector)
          textOnPageCheck(userScenario.commonExpectedResults.expectedContractorDetails, selector = Selectors.summaryListLabel(rowId = 1))
          val contractorNamePart = userScenario.commonExpectedResults.expectedContractorDetailsNameValue(aContractorCYAPage.contractorName.get) + " "
          val contractorRefPart = userScenario.commonExpectedResults.expectedContractorDetailsERNValue(aContractorCYAPage.employerRef)
          textOnPageCheck(contractorNamePart + contractorRefPart, selector = Selectors.summaryListValue(rowId = 1))
          textOnPageCheck(userScenario.commonExpectedResults.expectedEndOfTaxMonth, selector = Selectors.summaryListLabel(rowId = 2))
          textOnPageCheck(text = "5 " + translatedMonthAndTaxYear(aContractorCYAPage.month, taxYear), selector = Selectors.summaryListValue(rowId = 2))
          textOnPageCheck(userScenario.commonExpectedResults.expectedLabour, selector = Selectors.summaryListLabel(rowId = 3))
          textOnPageCheck(text = "£100", selector = Selectors.summaryListValue(rowId = 3), additionalTestText = "expectedLabour")
          textOnPageCheck(userScenario.commonExpectedResults.expectedCISDeduction, selector = Selectors.summaryListLabel(rowId = 4))
          textOnPageCheck(text = "£200", selector = Selectors.summaryListValue(rowId = 4), additionalTestText = "expectedCISDeduction")
          textOnPageCheck(userScenario.commonExpectedResults.expectedPaidForMaterials, selector = Selectors.summaryListLabel(rowId = 5))
          textOnPageCheck(text = "Yes", selector = Selectors.summaryListValue(rowId = 5))
          textOnPageCheck(userScenario.commonExpectedResults.expectedCostOfMaterials, selector = Selectors.summaryListLabel(rowId = 6))
          textOnPageCheck(text = "£300", selector = Selectors.summaryListValue(rowId = 6), additionalTestText = "Cost of materials ")
          buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector,
            href = Some(ContractorSummaryController.show(taxYear, aContractorCYAPage.employerRef).url))
        }

        "without contractor name and None for other optional fields" which {
          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val pageModel = aContractorCYAPage.copy(
            contractorName = None,
            labourAmount = None,
            deductionAmount = None,
            costOfMaterials = None
          )

          implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.specificExpectedResults.get.expectedTitle)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedP1, Selectors.paragraphTextSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedInsetText(taxYear), Selectors.insetTextSelector)
          textOnPageCheck(userScenario.commonExpectedResults.expectedContractorDetails, selector = Selectors.summaryListLabel(rowId = 1))
          val contractorNamePart = userScenario.commonExpectedResults.expectedContractorDetailsNameValue("")
          val contractorRefPart = userScenario.commonExpectedResults.expectedContractorDetailsERNValue(pageModel.employerRef)
          textOnPageCheck(contractorNamePart + contractorRefPart, selector = Selectors.summaryListValue(rowId = 1))
          textOnPageCheck(userScenario.commonExpectedResults.expectedEndOfTaxMonth, selector = Selectors.summaryListLabel(rowId = 2))
          textOnPageCheck(text = "5 " + translatedMonthAndTaxYear(pageModel.month, taxYear), selector = Selectors.summaryListValue(rowId = 2))
          textOnPageCheck(userScenario.commonExpectedResults.expectedLabour, selector = Selectors.summaryListLabel(rowId = 3))
          textOnPageCheck(text = "Not provided", selector = Selectors.summaryListHiddenValue(rowId = 3), additionalTestText = "expectedLabour")
          textOnPageCheck(userScenario.commonExpectedResults.expectedCISDeduction, selector = Selectors.summaryListLabel(rowId = 4))
          textOnPageCheck(text = "Not provided", selector = Selectors.summaryListHiddenValue(rowId = 4), additionalTestText = "expectedCISDeduction")
          textOnPageCheck(userScenario.commonExpectedResults.expectedPaidForMaterials, selector = Selectors.summaryListLabel(rowId = 5))
          textOnPageCheck(text = "No", selector = Selectors.summaryListValue(rowId = 5))
          elementNotOnPageCheck(selector = Selectors.summaryListLabel(rowId = 6))
          elementNotOnPageCheck(selector = Selectors.summaryListValue(rowId = 6))
          buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, href = Some(ContractorSummaryController.show(taxYear, pageModel.employerRef).url))
        }
      }
    }
  }
}