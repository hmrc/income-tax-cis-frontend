/*
 * Copyright 2023 HM Revenue & Customs
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

package actions

import config.MockAuditService
import models.User
import play.api.test.FakeRequest
import sttp.model.Method.{GET, POST}
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.UserPriorDataRequestBuilder.aUserPriorDataRequest
import support.builders.models.audit.ViewCisPeriodAuditBuilder.aViewCisPeriodAudit
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class InYearViewCisPeriodAuditActionSpec extends UnitTest with MockAuditService with TaxYearProvider {

  private val executionContext = ExecutionContext.global

  private val underTest = InYearViewCisPeriodAuditAction(taxYear = taxYear, employerRef = aCisDeductions.employerRef,
    auditService = mockAuditService, month = aPeriodData.deductionPeriod.toString.toLowerCase)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "not send an audit and return None when it's a POST request" in {
      await(underTest.filter(aUserPriorDataRequest.copy(request = FakeRequest.apply(POST.method, "/")))) shouldBe None
    }

    "send a ViewCisPeriodAudit and return None" in {
      mockSendAudit(aViewCisPeriodAudit.toAuditModel)

      val aUserWithIndividualAffinityGroup = User(aCisUserData.mtdItId, None, aCisUserData.nino, aCisUserData.sessionId, aUser.affinityGroup)
      await(underTest.filter(aUserPriorDataRequest.copy(request = FakeRequest.apply(GET.method, "/"), user = aUserWithIndividualAffinityGroup))) shouldBe None
    }

    "not send an audit and return None when there are no cis deductions for that user" in {
      val cisData = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq.empty)))

      val aUserWithIndividualAffinityGroup = User(aCisUserData.mtdItId, None, aCisUserData.nino, aCisUserData.sessionId, aUser.affinityGroup)
      await(underTest.filter(aUserPriorDataRequest.copy(incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(cisData)),
        request = FakeRequest.apply(GET.method, "/"), user = aUserWithIndividualAffinityGroup))) shouldBe None
    }
  }
}
