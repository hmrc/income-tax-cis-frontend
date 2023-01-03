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

import config.MockNrsService
import models.User
import models.nrs.{ContractorDetails, DeductionPeriod, ViewCisPeriodPayload}
import play.api.test.FakeRequest
import sttp.model.Method.{GET, POST}
import support.builders.models.UserBuilder.aUser
import support.builders.models.UserSessionDataRequestBuilder.aUserSessionDataRequest
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.{TaxYearProvider, UnitTest}

import scala.concurrent.ExecutionContext

class SessionViewCisPeriodNrsActionSpec extends UnitTest with MockNrsService with TaxYearProvider {

  private val executionContext = ExecutionContext.global

  private val underTest = SessionViewCisPeriodNrsAction(nrsService = mockNrsService)(executionContext)

  ".executionContext" should {
    "return the given execution context" in {
      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "not send message to  nrs and return None when it's a POST request" in {
      await(underTest.filter(aUserSessionDataRequest.copy(request = FakeRequest.apply(POST.method, "/")))) shouldBe None
    }

    "send a ViewCisPeriodNrs and return None" in {
      val priorData = aCisUserData.cis.periodData.get
      val contractorName = aCisUserData.cis.contractorName
      val employerRef = aCisUserData.employerRef
      val nrsPayload = ViewCisPeriodPayload(
        ContractorDetails(name = contractorName,
          ern = employerRef),
        DeductionPeriod(
          month = priorData.deductionPeriod.toString,
          labour = priorData.grossAmountPaid,
          cisDeduction = priorData.deductionAmount,
          costOfMaterialsQuestion = priorData.costOfMaterialsQuestion.get,
          materialsCost = priorData.costOfMaterials))
      mockSendNrs(nrsPayload)

      val aUserWithIndividualAffinityGroup = User(aCisUserData.mtdItId, None, aCisUserData.nino, aCisUserData.sessionId, aUser.affinityGroup)

      await(underTest.filter(aUserSessionDataRequest.copy(request = FakeRequest.apply(GET.method, "/"), user = aUserWithIndividualAffinityGroup))) shouldBe None
    }

    "not send an audit and return None when there are no cisUserData" in {
      val cisData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))

      val aUserWithIndividualAffinityGroup = User(aCisUserData.mtdItId, None, aCisUserData.nino, aCisUserData.sessionId, aUser.affinityGroup)
      await(underTest.filter(aUserSessionDataRequest.copy(cisUserData = cisData,
        request = FakeRequest.apply(GET.method, "/"), user = aUserWithIndividualAffinityGroup))) shouldBe None
    }
  }

}
