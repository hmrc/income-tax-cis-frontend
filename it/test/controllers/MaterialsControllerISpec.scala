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

package controllers

import controllers.routes.ContractorCYAController
import forms.YesNoForm
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.ViewHelpers

class MaterialsControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCISDB()
  }

  private def url(taxYear: Int, month: String, employerRef: String): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/materials?month=$month&contractor=$employerRef"
  }

  ".show" should {
    "redirect to income tax submission overview when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "return OK when EOY" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        insertCyaData(aCisUserData)
        urlGet(fullUrl(url(taxYearEOY, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to income tax submission overview when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlPost(fullUrl(url(taxYear, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map(YesNoForm.yesNo -> "true"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "persist deduction amount and redirect to next page when Yes" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aCisUserData.nino, taxYearEOY)
        val cyaPeriodData = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
        insertCyaData(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(cyaPeriodData))))
        urlPost(fullUrl(url(taxYearEOY, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Map(YesNoForm.yesNo -> "true"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe ContractorCYAController.show(taxYearEOY, aPeriodData.deductionPeriod.toString.toLowerCase, aCisDeductions.employerRef).url
      findCyaData(taxYearEOY, aCisDeductions.employerRef, aUser).get.cis.periodData.get.costOfMaterialsQuestion shouldBe Some(true)
    }
  }
}
