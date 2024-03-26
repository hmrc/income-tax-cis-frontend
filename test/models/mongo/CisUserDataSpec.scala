/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalamock.scalatest.MockFactory
import support.{TaxYearProvider, UnitTest}
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import utils.{AesGcmAdCrypto, TestingClock}

import java.time.{LocalDateTime, Month, ZoneId}

class CisUserDataSpec extends UnitTest
  with MockFactory with TaxYearProvider {

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

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
        CISSubmission(None, None, List(
          PeriodData(
            deductionFromDate = s"$taxYearEndOfYearMinusOne-04-06",
            deductionToDate = s"$taxYearEndOfYearMinusOne-05-05",
            grossAmountPaid = Some(500.0),
            deductionAmount = 100.0,
            costOfMaterials = Some(250.0)
          ),
          PeriodData(
            deductionFromDate = s"$taxYearEndOfYearMinusOne-10-06",
            deductionToDate = s"$taxYearEndOfYearMinusOne-11-05",
            grossAmountPaid = Some(500.0),
            deductionAmount = 100.0,
            costOfMaterials = Some(250.0)
          )
        ), Some("submissionId"))
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
      val underTest = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))

      underTest.isFinished shouldBe false
    }
  }

  "CisUserData.encrypted" should {
    "return EncryptedCisUserData object" in {
      val encryptedCisCYAModel = EncryptedCisCYAModel()
      val underTest = aCisUserData.copy(cis = cisCYAModel)

      (cisCYAModel.encrypted(_: AesGcmAdCrypto, _: String)).expects(aesGcmAdCrypto, associatedText).returning(encryptedCisCYAModel)

      underTest.encrypted shouldBe EncryptedCisUserData(
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
        lastUpdated = LocalDateTime.now(ZoneId.of("UTC"))
      )

      (encryptedCisCYAModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(aesGcmAdCrypto, associatedText).returning(aCisCYAModel)

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
