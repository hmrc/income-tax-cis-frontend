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

import org.mockito.ArgumentMatchers.{eq as eqTo}
import org.mockito.Mockito.{mock, when}
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

import java.time.Month

class CYAPeriodDataSpec extends UnitTest {

  private val aCYAPeriodDataJson: JsObject = Json.obj(
    "deductionPeriod" -> Month.MAY.toString,
    "grossAmountPaid" -> Some(500.00),
    "deductionAmount" -> Some(100.00),
    "costOfMaterialsQuestion" -> Some(true),
    "costOfMaterials" -> Some(250.00),
    "contractorSubmitted" -> false,
    "originallySubmittedPeriod" -> Some(Month.MAY.toString)
  )

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock(classOf[AesGcmAdCrypto])
  private implicit val associatedText: String = "some-associated-text"

  private val encryptedDeductionPeriod           = EncryptedValue("encryptedDeductionPeriod", "some-nonce")
  private val encryptedContractorSubmitted        = EncryptedValue("encryptedContractorSubmitted", "some-nonce")
  private val encryptedGrossAmountPaid            = EncryptedValue("encryptedGrossAmountPaid", "some-nonce")
  private val encryptedDeductionAmount            = EncryptedValue("encryptedDeductionAmount", "some-nonce")
  private val encryptedCostOfMaterialsQuestion    = EncryptedValue("encryptedCostOfMaterialsQuestion", "some-nonce")
  private val encryptedCostOfMaterials            = EncryptedValue("encryptedCostOfMaterials", "some-nonce")
  private val encryptedOriginallySubmittedPeriod  = EncryptedValue("encryptedOriginallySubmittedPeriod", "some-nonce")

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
      val testData = aCYAPeriodData.copy(deductionPeriod = Month.JUNE)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.deductionPeriod.toString))(eqTo(associatedText))).thenReturn(encryptedDeductionPeriod)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.grossAmountPaid.get.toString))(eqTo(associatedText))).thenReturn(encryptedGrossAmountPaid)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.deductionAmount.get.toString))(eqTo(associatedText))).thenReturn(encryptedDeductionAmount)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.costOfMaterialsQuestion.get.toString))(eqTo(associatedText))).thenReturn(encryptedCostOfMaterialsQuestion)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.costOfMaterials.get.toString))(eqTo(associatedText))).thenReturn(encryptedCostOfMaterials)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.contractorSubmitted.toString))(eqTo(associatedText))).thenReturn(encryptedContractorSubmitted)
      when(aesGcmAdCrypto.encrypt(eqTo(testData.originallySubmittedPeriod.get.toString))(eqTo(associatedText))).thenReturn(encryptedOriginallySubmittedPeriod)

      testData.encrypted shouldBe EncryptedCYAPeriodData(
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
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedDeductionPeriod))(eqTo(associatedText))).thenReturn(Month.JULY.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedOriginallySubmittedPeriod))(eqTo(associatedText))).thenReturn(Month.MAY.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedGrossAmountPaid))(eqTo(associatedText))).thenReturn(100.0.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedDeductionAmount))(eqTo(associatedText))).thenReturn(200.0.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedCostOfMaterialsQuestion))(eqTo(associatedText))).thenReturn(true.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedContractorSubmitted))(eqTo(associatedText))).thenReturn(false.toString)
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedCostOfMaterials))(eqTo(associatedText))).thenReturn(300.0.toString)

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
