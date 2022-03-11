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

import java.net.URLEncoder

import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.{DatabaseHelper, IntegrationTest}
import utils.ViewHelpers

class DeductionPeriodControllerISpec extends IntegrationTest with ViewHelpers with DatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val employerRef: String = aCisDeductions.employerRef

  private def url(taxYear: Int, employerRef: String): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/when-deductions-made?contractor=${URLEncoder.encode(employerRef,"UTF8")}"
  }

  ".show" should {
    "Render Deductions Period page" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aCisUserData)
        urlGet(fullUrl(url(taxYearEOY, employerRef)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
      result.body should include("When did your contractor make CIS deductions?")
    }
  }

  ".submit" should {
    "submit Deductions Period data" in {

      val form: Map[String, String] = Map("month" -> "january")

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aCisUserData)
        urlPost(fullUrl(url(taxYearEOY, employerRef)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").get shouldBe controllers.routes.DeductionsSummaryController.show(taxYearEOY).url
    }
  }
}
