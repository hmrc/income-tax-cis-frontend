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

import akka.util.ByteString.UTF_8
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import repositories.CisUserDataRepositoryImpl
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.ViewHelpers

import java.net.URLEncoder.encode

class ContractorSummaryControllerISpec extends IntegrationTest with ViewHelpers {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  private val repoUnderTest: CisUserDataRepositoryImpl = app.injector.instanceOf[CisUserDataRepositoryImpl]

  private def url(taxYear: Int, employerRef: String): String = {
    val encodedRef = encode(employerRef, UTF_8)
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/contractor?contractor=$encodedRef"
  }

  private def setupNewDeductionUrl(taxYear: Int, employerRef: String): String = {
    val encodedRef = encode(employerRef, UTF_8)
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/add-cis-deduction?contractor=$encodedRef"
  }

  ".show" should {
    "render the contractor summary page for in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, employerRef = aCisDeductions.employerRef)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }

    "render the contractor summary page for end of year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlGet(fullUrl(url(taxYearEOY, employerRef = aCisDeductions.employerRef)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".addCisDeduction" should {
    "do the setup for a new cis deduction to be added and redirect to period page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlGet(fullUrl(setupNewDeductionUrl(taxYearEOY, employerRef = aCisDeductions.employerRef)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      val data = aCisDeductions.toCYA(None,Seq())
      val updatedCya = data.copy(
        priorPeriodData = data.priorPeriodData.map(_.copy(contractorSubmitted = true))
      )

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe controllers.routes.DeductionPeriodController.show(taxYearEOY,aCisDeductions.employerRef).url
      val response = await(repoUnderTest.find(taxYearEOY,aCisDeductions.employerRef,aUser))
      val responseTime = response.right.get.get.lastUpdated

      response shouldBe Right(Some(aCisUserData.copy(cis = updatedCya, lastUpdated = responseTime)))
    }
  }
}
