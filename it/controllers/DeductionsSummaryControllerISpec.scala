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
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import support.IntegrationTest
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

class DeductionsSummaryControllerISpec extends IntegrationTest with ViewHelpers {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def url(taxYear: Int): String = s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/summary"

  ".show" should {
    "Render in year Deductions Summary page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }

    "Render end of year Deductions Summary page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlGet(fullUrl(url(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }
}
