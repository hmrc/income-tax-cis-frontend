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

package models.mongo

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.Month

class CisCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedContractorName = EncryptedValue("encryptedContractorName", "some-nonce")
  private val cyaPeriodData1 = mock[CYAPeriodData]
  private val cyaPeriodData2 = mock[CYAPeriodData]
  private val encryptedPeriodData1 = mock[EncryptedCYAPeriodData]
  private val encryptedPeriodData2 = mock[EncryptedCYAPeriodData]

  "CisCYAModel.isFinished" should {
    "return false when periodData does not exist" in {
      val underTest = aCisCYAModel.copy(periodData = None)

      underTest.isFinished shouldBe false
    }

    "return false when CYAPeriodData is not finished" in {
      val underTest = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(grossAmountPaid = None)))

      underTest.isFinished shouldBe false
    }

    "return true when periodData is finished" in {
      val underTest = aCisCYAModel.copy(periodData = Some(aCYAPeriodData))

      underTest.isFinished shouldBe true
    }
  }

  ".cyaPeriodData" should {
    "return priorPeriodData when periodData is None" in {
      val expectedPriorPeriodData = Seq(
        aCYAPeriodData.copy(deductionPeriod = Month.MAY),
        aCYAPeriodData.copy(deductionPeriod = Month.JUNE)
      )

      val underTest = aCisCYAModel.copy(periodData = None, priorPeriodData = expectedPriorPeriodData)

      underTest.cyaPeriodData shouldBe expectedPriorPeriodData
    }

    "return periodData along with priorPeriodData for a new period" in {
      val periodData = aCYAPeriodData.copy(deductionPeriod = Month.JULY, originallySubmittedPeriod = None)
      val priorPeriodData = Seq(
        aCYAPeriodData.copy(deductionPeriod = Month.MAY),
        aCYAPeriodData.copy(deductionPeriod = Month.JUNE)
      )

      val underTest = aCisCYAModel.copy(periodData = Some(periodData), priorPeriodData = priorPeriodData)

      underTest.cyaPeriodData shouldBe periodData +: priorPeriodData
    }

    "return updated priorPeriodData when periodData is an update" in {
      val periodData = aCYAPeriodData.copy(deductionPeriod = Month.JULY, originallySubmittedPeriod = Some(Month.JUNE))
      val mayPeriodData = aCYAPeriodData.copy(deductionPeriod = Month.MAY)
      val junePeriodData = aCYAPeriodData.copy(deductionPeriod = Month.JUNE, originallySubmittedPeriod = Some(Month.JUNE))
      val underTest = aCisCYAModel.copy(periodData = Some(periodData), priorPeriodData = Seq(mayPeriodData, junePeriodData))

      underTest.cyaPeriodData shouldBe Seq(periodData, mayPeriodData)
    }
  }

  ".isNewSubmissionFor" should {
    "return true if no prior data" in {
      val underTest = aCisCYAModel.copy(priorPeriodData = Seq())

      underTest.isNewSubmissionFor(Month.MAY) shouldBe true
    }
    "return false if requested month is in the prior data" in {
      aCisCYAModel.isNewSubmissionFor(Month.NOVEMBER) shouldBe false
    }
  }

  ".isAnUpdateFor" should {
    "return true when an update is made to same month" in {
      aCisCYAModel.isAnUpdateFor(Month.MAY) shouldBe true
    }

    "return false if requested month is in the prior data" in {
      aCisCYAModel.isAnUpdateFor(Month.NOVEMBER) shouldBe false
    }
  }

  ".periodDataUpdated" should {
    "return true when prior period data does not contain period data" in {
      val underTest = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = Month.APRIL, grossAmountPaid = Some(100))),
        priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = Month.FEBRUARY), aCYAPeriodData.copy(deductionPeriod = Month.APRIL))
      )

      underTest.periodDataUpdated shouldBe true
    }

    "return true when period data is None" in {
      val underTest = aCisCYAModel.copy(
        periodData = None,
        priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = Month.APRIL))
      )

      underTest.periodDataUpdated shouldBe true
    }

    "return false when prior period data contains period data" in {
      val underTest = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = Month.APRIL)),
        priorPeriodData = Seq(aCYAPeriodData, aCYAPeriodData.copy(deductionPeriod = Month.APRIL))
      )

      underTest.periodDataUpdated shouldBe false
    }
  }

  "CisCYAModel.encrypted" should {
    "return EncryptedCisCYAModel" in {
      val underTest = CisCYAModel(Some("some-contractor-name"), Some(cyaPeriodData1), Seq(cyaPeriodData1, cyaPeriodData2))

      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.contractorName.get, textAndKey).returning(encryptedContractorName)
      (cyaPeriodData1.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedPeriodData1)
      (cyaPeriodData1.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedPeriodData1)
      (cyaPeriodData2.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedPeriodData2)

      underTest.encrypted shouldBe EncryptedCisCYAModel(
        contractorName = Some(encryptedContractorName),
        periodData = Some(encryptedPeriodData1),
        priorPeriodData = Seq(encryptedPeriodData1, encryptedPeriodData2)
      )
    }
  }

  "EncryptedCisCYAModel.decrypted" should {
    "return CisCYAModel" in {
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedContractorName.value, encryptedContractorName.nonce, textAndKey, *).returning(value = "contractor-name")
      (encryptedPeriodData1.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(cyaPeriodData1)
      (encryptedPeriodData1.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(cyaPeriodData1)
      (encryptedPeriodData2.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(cyaPeriodData2)

      EncryptedCisCYAModel(Some(encryptedContractorName),
        Some(encryptedPeriodData1),
        Seq(encryptedPeriodData1, encryptedPeriodData2)
      ).decrypted shouldBe CisCYAModel(
        contractorName = Some("contractor-name"),
        Some(cyaPeriodData1),
        Seq(cyaPeriodData1, cyaPeriodData2)
      )
    }
  }
}
