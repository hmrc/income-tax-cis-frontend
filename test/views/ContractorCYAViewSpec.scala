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
    val paragraphTextSelector = "#deduction-info-id"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#return-to-contractor-button-id"
    val saveButtonSelector = "#save-and-continue-button-id"

    def summaryListLabel(rowId: Int): String = s"div.govuk-summary-list__row:nth-child($rowId) > dt:nth-child(1)"

    def summaryListValue(rowId: Int, cell: Int = 2): String = s"div.govuk-summary-list__row:nth-child($rowId) > dd:nth-child($cell)"

    def summaryListLinksSelector(row: Int, cell: Int): String = s"div.govuk-summary-list__row:nth-child($row) > dd:nth-child($cell) > a:nth-child(1)"

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
    val expectedSaveButtonText: String
    val contractorDetailsHiddenText: String
    val periodHiddenText: String
    val labourHiddenText: String
    val cisDeductionsHiddenText: String
    val paidForMaterialsHiddenText: String
    val materialsHiddenText: String
    val changeHiddenText: String
    val yes: String
    val no: String
    val notProvided: String
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
    val expectedSaveButtonText: String = "Save and continue"
    val changeHiddenText: String = "Change"
    val contractorDetailsHiddenText: String = "Change contractor details"
    val periodHiddenText: String = "Change end of tax month"
    val labourHiddenText: String = "Change amount paid for labour"
    val cisDeductionsHiddenText: String = "Change CIS deduction"
    val paidForMaterialsHiddenText: String = "Change whether paid for materials"
    val materialsHiddenText: String = "Change cost of materials"
    val yes: String = "Yes"
    val no: String = "No"
    val notProvided: String = "Not provided"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Didyniadau Cynllun y Diwydiant Adeiladu (CIS) ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedContractorDetails: String = "Manylion y contractwr"
    override val expectedContractorDetailsNameValue: String => String = (contractorName: String) => s"Enw: $contractorName"
    override val expectedContractorDetailsERNValue: String => String = (contractorERN: String) => s"ERN: $contractorERN"
    override val expectedEndOfTaxMonth: String = "Diwedd mis treth"
    override val expectedLabour: String = "Llafur"
    override val expectedCISDeduction: String = "Didyniadau CIS"
    override val expectedPaidForMaterials: String = "Wedi talu am ddeunyddiau"
    override val expectedCostOfMaterials: String = "Cost deunyddiau"
    override val expectedButtonText: String = "Yn ôl i’r contractwr"
    val expectedSaveButtonText: String = "Cadw ac yn eich blaen"
    val changeHiddenText: String = "Newid"
    val contractorDetailsHiddenText: String = "Newidiwch fanylion y contractwr"
    val periodHiddenText: String = "Newidiwch ddiwedd mis treth"
    val labourHiddenText: String = "Newidiwch y swm a dalwyd am lafur"
    val cisDeductionsHiddenText: String = "Newidiwch ddidyniad CIS"
    val paidForMaterialsHiddenText: String = "Newidiwch os yw wedi talu am ddeunyddiau"
    val materialsHiddenText: String = "Newidiwch gost deunyddiau"
    val yes = "Iawn"
    val no = "Na"
    val notProvided: String = "Heb ddarparu"
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
    override val expectedTitle: String = "Gwiriwch eich didyniadau CIS"
    override val expectedH1: String = "Gwiriwch eich didyniadau CIS"
    override val expectedP1: String = "Bydd eich didyniadau CIS yn seiliedig ar yr wybodaeth sydd eisoes gennym amdanoch."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"Ni allwch ddiweddaru’ch manylion CIS tan 6 Ebrill $taxYear."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    override val expectedTitle: String = "Gwiriwch ddidyniadau CIS eich cleient"
    override val expectedH1: String = "Gwiriwch ddidyniadau CIS eich cleient"
    override val expectedP1: String = "Mae didyniadau CIS eich cleient yn seiliedig ar yr wybodaeth sydd eisoes gennym amdano."
    override val expectedInsetText: Int => String = (taxYear: Int) => s"Ni allwch ddiweddaru manylion CIS eich cleient tan 6 Ebrill $taxYear."
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
      "render end of year version of Check your CIS deductions page" which {
        "when non HMRC data (isContractorDeduction is false)" which {
          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(underTest(aContractorCYAPage.copy(isAgent = userScenario.isAgent, isInYear = false, isContractorDeduction = false)).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))
          h1Check(userScenario.specificExpectedResults.get.expectedH1)
          elementNotOnPageCheck(Selectors.paragraphTextSelector)
          textOnPageCheck(userScenario.commonExpectedResults.expectedContractorDetails, selector = Selectors.summaryListLabel(rowId = 1))
          val contractorNamePart = userScenario.commonExpectedResults.expectedContractorDetailsNameValue(aContractorCYAPage.contractorName.get) + " "
          val contractorRefPart = userScenario.commonExpectedResults.expectedContractorDetailsERNValue(aContractorCYAPage.employerRef)
          textOnPageCheck(contractorNamePart + contractorRefPart, selector = Selectors.summaryListValue(rowId = 1))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.contractorDetailsHiddenText, Selectors.summaryListLinksSelector(row = 1, 3),
            controllers.routes.ContractorDetailsController.show(taxYear, Some(aContractorCYAPage.employerRef)).url, additionalTestText = "(first row)")
          textOnPageCheck(userScenario.commonExpectedResults.expectedEndOfTaxMonth, selector = Selectors.summaryListLabel(rowId = 2))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.periodHiddenText, Selectors.summaryListLinksSelector(row = 2, 3),
            controllers.routes.DeductionPeriodController.show(taxYear, aContractorCYAPage.employerRef).url, additionalTestText = "(2nd row)")
          textOnPageCheck(text = "5 " + translatedMonthAndTaxYear(aContractorCYAPage.month, taxYear), selector = Selectors.summaryListValue(rowId = 2))
          textOnPageCheck(userScenario.commonExpectedResults.expectedLabour, selector = Selectors.summaryListLabel(rowId = 3))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.labourHiddenText, Selectors.summaryListLinksSelector(row = 3, 3),
            controllers.routes.LabourPayController.show(taxYear, aContractorCYAPage.month.toString.toLowerCase, aContractorCYAPage.employerRef).url, additionalTestText = "(3rd row)")
          textOnPageCheck(text = "£100", selector = Selectors.summaryListValue(rowId = 3), additionalTestText = "expectedLabour")
          textOnPageCheck(userScenario.commonExpectedResults.expectedCISDeduction, selector = Selectors.summaryListLabel(rowId = 4))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.cisDeductionsHiddenText, Selectors.summaryListLinksSelector(row = 4, 3),
            controllers.routes.DeductionAmountController.show(taxYear, aContractorCYAPage.month.toString.toLowerCase, aContractorCYAPage.employerRef).url, additionalTestText = "(4th row)")
          textOnPageCheck(text = "£200", selector = Selectors.summaryListValue(rowId = 4), additionalTestText = "expectedCISDeduction")
          textOnPageCheck(userScenario.commonExpectedResults.expectedPaidForMaterials, selector = Selectors.summaryListLabel(rowId = 5))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.paidForMaterialsHiddenText, Selectors.summaryListLinksSelector(row = 5, 3),
            controllers.routes.MaterialsController.show(taxYear, aContractorCYAPage.month.toString.toLowerCase, aContractorCYAPage.employerRef).url, additionalTestText = "(5th row)")
          textOnPageCheck(text = userScenario.commonExpectedResults.yes, selector = Selectors.summaryListValue(rowId = 5))
          textOnPageCheck(userScenario.commonExpectedResults.expectedCostOfMaterials, selector = Selectors.summaryListLabel(rowId = 6))
          linkCheck(userScenario.commonExpectedResults.changeHiddenText + " " +
            userScenario.commonExpectedResults.materialsHiddenText, Selectors.summaryListLinksSelector(row = 6, 3),
            controllers.routes.MaterialsAmountController.show(taxYear, aContractorCYAPage.month.toString.toLowerCase, aContractorCYAPage.employerRef).url, additionalTestText = "(6th row)")
          textOnPageCheck(text = "£300", selector = Selectors.summaryListValue(rowId = 6), additionalTestText = "Cost of materials ")
          buttonCheck(userScenario.commonExpectedResults.expectedSaveButtonText, Selectors.saveButtonSelector)
        }
      }

      "render in year version of Check your CIS deductions page" which {
        "when HMRC data (isContractorDeduction is true)" which {
          implicit val userPriorDataRequest: UserPriorDataRequest[AnyContent] = getUserPriorDataRequest(userScenario.isAgent)
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          implicit val document: Document = Jsoup.parse(underTest(aContractorCYAPage.copy(isAgent = userScenario.isAgent, isContractorDeduction = true)).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
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
          textOnPageCheck(text = userScenario.commonExpectedResults.yes, selector = Selectors.summaryListValue(rowId = 5))
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
            costOfMaterials = None,
            isAgent = userScenario.isAgent,
            isContractorDeduction = true
          )

          implicit val document: Document = Jsoup.parse(underTest(pageModel).body)

          welshToggleCheck(userScenario.isWelsh)
          titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
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
          textOnPageCheck(text = userScenario.commonExpectedResults.notProvided, selector = Selectors.summaryListHiddenValue(rowId = 3), additionalTestText = "expectedLabour")
          textOnPageCheck(userScenario.commonExpectedResults.expectedCISDeduction, selector = Selectors.summaryListLabel(rowId = 4))
          textOnPageCheck(text = userScenario.commonExpectedResults.notProvided, selector = Selectors.summaryListHiddenValue(rowId = 4), additionalTestText = "expectedCISDeduction")
          textOnPageCheck(userScenario.commonExpectedResults.expectedPaidForMaterials, selector = Selectors.summaryListLabel(rowId = 5))
          textOnPageCheck(text = userScenario.commonExpectedResults.no, selector = Selectors.summaryListValue(rowId = 5))
          elementNotOnPageCheck(selector = Selectors.summaryListLabel(rowId = 6))
          elementNotOnPageCheck(selector = Selectors.summaryListValue(rowId = 6))
          buttonCheck(userScenario.commonExpectedResults.expectedButtonText, Selectors.buttonSelector, href = Some(ContractorSummaryController.show(taxYear, pageModel.employerRef).url))
        }
      }
    }
  }
}