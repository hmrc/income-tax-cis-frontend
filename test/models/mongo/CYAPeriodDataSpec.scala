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

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

import java.time.Month

class CYAPeriodDataSpec extends UnitTest
  with MockFactory {

  private val aCYAPeriodDataJson: JsObject = Json.obj(
    "deductionPeriod" -> Month.MAY.toString,
    "grossAmountPaid" -> Some(500.00),
    "deductionAmount" -> Some(100.00),
    "costOfMaterialsQuestion" -> Some(true),
    "costOfMaterials" -> Some(250.00),
    "contractorSubmitted" -> false,
    "originallySubmittedPeriod" -> Some(Month.MAY.toString)
  )

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val encryptedDeductionPeriod = EncryptedValue("encryptedDeductionPeriod", "some-nonce")
  private val encryptedContractorSubmitted = EncryptedValue("encryptedContractorSubmitted", "some-nonce")
  private val encryptedGrossAmountPaid = EncryptedValue("encryptedGrossAmountPaid", "some-nonce")
  private val encryptedDeductionAmount = EncryptedValue("encryptedDeductionAmount", "some-nonce")
  private val encryptedCostOfMaterialsQuestion = EncryptedValue("encryptedCostOfMaterialsQuestion", "some-nonce")
  private val encryptedCostOfMaterials = EncryptedValue("encryptedCostOfMaterials", "some-nonce")
  private val encryptedOriginallySubmittedPeriod = EncryptedValue("encryptedOriginallySubmittedPeriod", "some-nonce")

  ".isAnUpdateFor" should {
    "return true when an update is made to same month" in {
      aCYAPeriodData.isAnUpdateFor(Month.MAY) shouldBe true
    }
    "return false if requested month is in the prior data" in {
      aCYAPeriodData.isAnUpdateFor(Month.NOVEMBER) shouldBe false
    }
  }

  "CYAPeriodData" should {
    "write to Json correctly when using implicit writes" in {
      val actualResult = Json.toJson(aCYAPeriodData)
      actualResult shouldBe aCYAPeriodDataJson
    }

    "read to Json correctly when using implicit read" in {
      val result = aCYAPeriodDataJson.as[CYAPeriodData]
      result shouldBe aCYAPeriodData
    }
  }

  "CYAPeriodData.isFinished" should {
    "return true" when {
      "grossAmountPaid.isDefined, deductionAmount.isDefined and costOfMaterialsQuestion is false" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false), costOfMaterials = None)
        result.isFinished shouldBe true
      }

      "grossAmountPaid.isDefined, deductionAmount.isDefined, costOfMaterials section is complete" in {
        aCYAPeriodData.isFinished shouldBe true
      }
    }

    "return false" when {
      "grossAmountPaid isn't define" in {
        val result = aCYAPeriodData.copy(grossAmountPaid = None)
        result.isFinished shouldBe false
      }

      "deductionAmount isn't defined" in {
        val result = aCYAPeriodData.copy(deductionAmount = None)
        result.isFinished shouldBe false
      }

      "costOfMaterialsQuestion is None" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
        result.isFinished shouldBe false
      }

      "costOfMaterialsQuestion is Some(true) and costOfMaterials is None" in {
        val result = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true), costOfMaterials = None)
        result.isFinished shouldBe false
      }
    }
  }

  "CYAPeriodData.encrypted" should {
    "return EncryptedCYAPeriodData" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.deductionPeriod.toString, associatedText).returning(encryptedDeductionPeriod)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.grossAmountPaid.get.toString, associatedText).returning(encryptedGrossAmountPaid)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.deductionAmount.get.toString, associatedText).returning(encryptedDeductionAmount)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.costOfMaterialsQuestion.get.toString, associatedText).returning(encryptedCostOfMaterialsQuestion)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.costOfMaterials.get.toString, associatedText).returning(encryptedCostOfMaterials)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.contractorSubmitted.toString, associatedText).returning(encryptedContractorSubmitted)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aCYAPeriodData.originallySubmittedPeriod.get.toString, associatedText).returning(encryptedOriginallySubmittedPeriod)


      aCYAPeriodData.encrypted shouldBe EncryptedCYAPeriodData(
        deductionPeriod = encryptedDeductionPeriod,
        grossAmountPaid = Some(encryptedGrossAmountPaid),
        deductionAmount = Some(encryptedDeductionAmount),
        costOfMaterialsQuestion = Some(encryptedCostOfMaterialsQuestion),
        costOfMaterials = Some(encryptedCostOfMaterials),
        contractorSubmitted = encryptedContractorSubmitted,
        originallySubmittedPeriod = Some(encryptedOriginallySubmittedPeriod)
      )
    }
  }

  "EncryptedCYAPeriodData.decrypted" should {
    "return CYAPeriodData" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedDeductionPeriod, associatedText).returning(Month.JULY.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedOriginallySubmittedPeriod, associatedText).returning(Month.MAY.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedGrossAmountPaid, associatedText).returning(value = 100.0.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedDeductionAmount, associatedText).returning(value = 200.0.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCostOfMaterialsQuestion, associatedText).returning(value = true.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedContractorSubmitted, associatedText).returning(value = false.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedCostOfMaterials, associatedText).returning(value = 300.0.toString)

      val encryptedData = EncryptedCYAPeriodData(
        deductionPeriod = encryptedDeductionPeriod,
        grossAmountPaid = Some(encryptedGrossAmountPaid),
        deductionAmount = Some(encryptedDeductionAmount),
        costOfMaterialsQuestion = Some(encryptedCostOfMaterialsQuestion),
        costOfMaterials = Some(encryptedCostOfMaterials),
        contractorSubmitted = encryptedContractorSubmitted,
        originallySubmittedPeriod = Some(encryptedOriginallySubmittedPeriod)
      )

      encryptedData.decrypted shouldBe CYAPeriodData(
        deductionPeriod = Month.JULY,
        grossAmountPaid = Some(100.0),
        deductionAmount = Some(200.0),
        costOfMaterialsQuestion = Some(true),
        costOfMaterials = Some(300.0),
        contractorSubmitted = false,
        originallySubmittedPeriod = Some(Month.MAY)
      )
    }
  }
}
