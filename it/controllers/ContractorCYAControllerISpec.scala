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

import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import repositories.CisUserDataRepositoryImpl
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

class ContractorCYAControllerISpec extends IntegrationTest with ViewHelpers {

  private def url(taxYear: Int, month: String, employerRef: String): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/check-construction-industry-scheme-deductions?month=$month&contractor=$employerRef"
  }

  private val repoUnderTest: CisUserDataRepositoryImpl = app.injector.instanceOf[CisUserDataRepositoryImpl]
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "return Check your CIS deductions page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "return Check your CIS deductions page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlPost(fullUrl(url(taxYearEOY, month = aPeriodData.deductionPeriod.toString, employerRef = aCisDeductions.employerRef)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)), body = "")
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.ContractorSummaryController.show(taxYearEOY, aCisDeductions.employerRef).url
      await(repoUnderTest.find(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Right(None)
    }
  }
}