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

package models.mongo

import models.submission.{CISSubmission, PeriodData}
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.{SecureGCMCipher, TestingClock}

import java.time.Month

class CisUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val cisCYAModel = mock[CisCYAModel]
  private val encryptedCisCYAModel = mock[EncryptedCisCYAModel]

  "CisUserData.toSubmission" should {
    "return None when no period data" in {
      val underTest = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))

      underTest.toSubmission shouldBe None
    }
    "return a submission when finished" in {
      val underTest = aCisUserData

      underTest.toSubmission shouldBe Some(
        CISSubmission(None,None, List(
          PeriodData(
            deductionFromDate = "2021-04-06",
            deductionToDate = "2021-05-05",
            grossAmountPaid = Some(500.0),
            deductionAmount = 100.0,
            costOfMaterials = Some(250.0)
          ),
          PeriodData(
            deductionFromDate = "2021-10-06",
            deductionToDate = "2021-11-05",
            grossAmountPaid = Some(500.0),
            deductionAmount = 100.0,
            costOfMaterials = Some(250.0)
          )
        ),Some("submissionId"))
      )
    }
  }

  "CisUserData.hasPeriodData" should {
    "return false when does not have periodData is None" in {
      val underTest = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))

      underTest.hasPeriodData shouldBe false
    }

    "return true when periodData exists" in {
      val underTest = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData)))

      underTest.hasPeriodData shouldBe true
    }
  }

  ".isCyaDataFor" should {
    "return true if the current cya data has the same period data as the requested month" in {
      val underTest = aCisUserData

      underTest.isCyaDataFor(Month.MAY) shouldBe true
    }
    "return false if the current cya data does not have the same period data as the requested month" in {
      val underTest = aCisUserData

      underTest.isCyaDataFor(Month.NOVEMBER) shouldBe false
    }
  }

  ".isFinished" should {
    "return true when cis.isFinished returns true" in {
      val underTest = aCisUserData.copy(cis = aCisCYAModel)

      underTest.isFinished shouldBe true
    }

    "return true when cis.isFinished returns false" in {
      val underTest = aCisUserData.copy(cis = aCisCYAModel.copy(contractorName = None))

      underTest.isFinished shouldBe false
    }
  }

  "CisUserData.encrypted" should {
    "return EncryptedCisUserData object" in {
      val encryptedCisCYAModel = EncryptedCisCYAModel()
      val underTest = aCisUserData.copy(cis = cisCYAModel)

      (cisCYAModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(secureGCMCipher, textAndKey).returning(encryptedCisCYAModel)

      underTest.encrypted() shouldBe EncryptedCisUserData(
        sessionId = underTest.sessionId,
        mtdItId = underTest.mtdItId,
        nino = underTest.nino,
        taxYear = underTest.taxYear,
        employerRef = underTest.employerRef,
        submissionId = underTest.submissionId,
        isPriorSubmission = underTest.isPriorSubmission,
        cis = encryptedCisCYAModel,
        lastUpdated = underTest.lastUpdated
      )
    }
  }

  "CisUserData.createFrom" should {
    "create a CisUserData" in {
      val anyYear = 2020

      CisUserData.createFrom(aUser, taxYear = anyYear, employerRef = "any-ref", aCisCYAModel, TestingClock.now()) shouldBe new CisUserData(
        sessionId = aUser.sessionId,
        mtdItId = aUser.mtditid,
        nino = aUser.nino,
        taxYear = anyYear,
        employerRef = "any-ref",
        submissionId = None,
        isPriorSubmission = false,
        cis = aCisCYAModel,
        lastUpdated = TestingClock.now()
      )
    }
  }

  "EncryptedCisUserData.decrypted" should {
    "return CisUserData object" in {
      val underTest = EncryptedCisUserData(
        sessionId = "some-session-id",
        mtdItId = "some-mtd-it-id",
        nino = "some-nino",
        taxYear = 2020,
        employerRef = "some-employer-ref",
        submissionId = Some("some-submission-id"),
        isPriorSubmission = true,
        cis = encryptedCisCYAModel,
        lastUpdated = DateTime.now
      )

      (encryptedCisCYAModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(secureGCMCipher, textAndKey).returning(aCisCYAModel)

      underTest.decrypted shouldBe CisUserData(
        sessionId = underTest.sessionId,
        mtdItId = underTest.mtdItId,
        nino = underTest.nino,
        taxYear = underTest.taxYear,
        employerRef = underTest.employerRef,
        submissionId = underTest.submissionId,
        isPriorSubmission = underTest.isPriorSubmission,
        cis = aCisCYAModel,
        lastUpdated = underTest.lastUpdated
      )
    }
  }
}
