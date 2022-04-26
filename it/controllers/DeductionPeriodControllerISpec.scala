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
import java.time.Month

import controllers.routes.LabourPayController
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.{DatabaseHelper, IntegrationTest}
import utils.UrlUtils.encode
import utils.ViewHelpers

class DeductionPeriodControllerISpec extends IntegrationTest with ViewHelpers with DatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val employerRef: String = aCisDeductions.employerRef

  private def url(taxYear: Int, employerRef: String): String = {
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/when-deductions-made?contractor=${URLEncoder.encode(employerRef, "UTF8")}"
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
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aCisUserData)
        urlPost(fullUrl(url(taxYearEOY, employerRef)), body = Map("month" -> "january"), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.header(name = "location").get shouldBe LabourPayController.show(taxYearEOY, Month.JANUARY.toString, encode(aCisDeductions.employerRef)).url
      findCyaData(taxYearEOY, employerRef, aUser).get.cis.periodData.get.deductionPeriod shouldBe Month.JANUARY
    }
  }
}
