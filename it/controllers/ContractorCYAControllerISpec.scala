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
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import utils.ViewHelpers

import java.net.URLEncoder

class ContractorCYAControllerISpec extends IntegrationTest with ViewHelpers {

  private def url(taxYear: Int, month: String, employerRef: String): String =
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/check-construction-industry-scheme-deductions?month=$month&contractor=$employerRef"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "return Check your CIS deductions page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear, month = aPeriodData.deductionPeriod.toString, employerRef = URLEncoder.encode(aCisDeductions.employerRef, UTF_8))),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe OK
    }
  }
}