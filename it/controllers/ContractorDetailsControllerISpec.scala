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

import controllers.routes.DeductionPeriodController
import forms.ContractorDetailsForm
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import support.IntegrationTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.ViewHelpers

class ContractorDetailsControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropCISDB()
  }

  private def url(taxYear: Int, optContractor: Option[String] = None): String =
    s"/update-and-submit-income-tax-return/construction-industry-scheme-deductions/$taxYear/contractor-details" + optContractor.map(contractor => s"?contractor=$contractor").getOrElse("")

  ".show" should {
    "redirect to income tax submission overview when in year" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        urlGet(fullUrl(url(taxYear)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "return OK when EOY and no contractor provided" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        urlGet(fullUrl(url(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }

    "return OK when EOY and existing contractor provided" in {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYearEOY)
        insertCyaData(aCisUserData)
        urlGet(fullUrl(url(taxYearEOY, Some(aCisDeductions.employerRef))), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to income tax submission overview when in year" in {
      lazy val body = Map(ContractorDetailsForm.contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/456787")
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aUser.nino, taxYear)
        insertCyaData(aCisUserData)
        urlPost(fullUrl(url(taxYear)), body = body, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

    "Save contractor details and redirect to the Deduction Period Page" in {
      lazy val body = Map(ContractorDetailsForm.contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/55555")
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, aCisUserData.nino, taxYearEOY)
        insertCyaData(aCisUserData)
        urlPost(fullUrl(url(taxYearEOY)), body = body, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe DeductionPeriodController.show(taxYearEOY, contractor = "123/55555").url
      val persistedCisUserData = findCyaData(taxYearEOY, "123/55555", aUser).get
      persistedCisUserData.employerRef shouldBe "123/55555"
      persistedCisUserData.cis.contractorName shouldBe Some("some-name")
    }
  }
}
