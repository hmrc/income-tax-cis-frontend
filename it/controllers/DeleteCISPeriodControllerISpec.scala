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

package controllers

import controllers.routes.ContractorSummaryController
import forms.AmountForm
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.ViewHelpers

class DeleteCISPeriodControllerISpec extends IntegrationTest
  with ViewHelpers with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCISDB()
  }

  private def url(taxYear: Int, month: String, employerRef: String): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/remove-deduction?contractor=$employerRef&month=$month"
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "redirect to income tax submission overview when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, month = aPeriodData.deductionPeriod.toString.toLowerCase, employerRef = aCisDeductions.employerRef)),
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
        urlGet(fullUrl(url(taxYearEOY, month = aPeriodData.deductionPeriod.toString.toLowerCase, employerRef = aCisDeductions.employerRef)),
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
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)), body = Map(AmountForm.amount -> "123"))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "remove deduction period and redirect to contractor summary page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aCisUserData.nino, taxYearEOY)
        insertCyaData(aCisUserData)
        urlPost(fullUrl(url(taxYearEOY, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = Json.obj())
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe ContractorSummaryController.show(taxYearEOY, aCisDeductions.employerRef).url
      // TODO: Verify item has been removed when service method is implemented.
    }
  }
}
