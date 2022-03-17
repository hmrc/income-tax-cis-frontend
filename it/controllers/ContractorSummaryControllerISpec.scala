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
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

import java.net.URLEncoder.encode

class ContractorSummaryControllerISpec extends IntegrationTest with ViewHelpers {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def url(taxYear: Int, employerRef: String): String = {
    val encodedRef = encode(employerRef, UTF_8)
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/contractor?contractor=$encodedRef"
  }

  ".show" should {

    "render the contractor summary page for in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, aCisDeductions.employerRef)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }

    "redirect to the overview page when the tax year is end of year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear - 1)
        urlGet(fullUrl(url(taxYear - 1, aCisDeductions.employerRef)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear - 1)))
      }

      result.status shouldBe SEE_OTHER
      result.header(HeaderNames.LOCATION).contains(appConfig.incomeTaxSubmissionOverviewUrl(taxYear - 1)) shouldBe true
    }
  }
}
