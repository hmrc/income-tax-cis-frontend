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

import builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsObject, Json}
import support.UnitTest
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.Month

class CYAPeriodDataSpec extends UnitTest
  with MockFactory {

  private val aCYAPeriodDataJson: JsObject = Json.obj(
    "deductionPeriod" -> Month.MAY.toString,
    "grossAmountPaid" -> Some(500.00),
    "deductionAmount" -> Some(100.00),
    "costOfMaterialsQuestion" -> Some(true),
    "costOfMaterials" -> Some(250.00)
  )

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val encryptedDeductionPeriod = EncryptedValue("encryptedDeductionPeriod", "some-nonce")
  private val encryptedGrossAmountPaid = EncryptedValue("encryptedGrossAmountPaid", "some-nonce")
  private val encryptedDeductionAmount = EncryptedValue("encryptedDeductionAmount", "some-nonce")
  private val encryptedCostOfMaterialsQuestion = EncryptedValue("encryptedCostOfMaterialsQuestion", "some-nonce")
  private val encryptedCostOfMaterials = EncryptedValue("encryptedCostOfMaterials", "some-nonce")

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
      (secureGCMCipher.encrypt(_: Month)(_: TextAndKey)).expects(aCYAPeriodData.deductionPeriod, textAndKey).returning(encryptedDeductionPeriod)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCYAPeriodData.grossAmountPaid.get, textAndKey).returning(encryptedGrossAmountPaid)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCYAPeriodData.deductionAmount.get, textAndKey).returning(encryptedDeductionAmount)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aCYAPeriodData.costOfMaterialsQuestion.get, textAndKey).returning(encryptedCostOfMaterialsQuestion)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aCYAPeriodData.costOfMaterials.get, textAndKey).returning(encryptedCostOfMaterials)


      aCYAPeriodData.encrypted shouldBe EncryptedCYAPeriodData(
        deductionPeriod = encryptedDeductionPeriod,
        grossAmountPaid = Some(encryptedGrossAmountPaid),
        deductionAmount = Some(encryptedDeductionAmount),
        costOfMaterialsQuestion = Some(encryptedCostOfMaterialsQuestion),
        costOfMaterials = Some(encryptedCostOfMaterials),
      )
    }
  }

  "EncryptedCYAPeriodData.decrypted" should {
    "return CYAPeriodData" in {
      (secureGCMCipher.decrypt[Month](_: String, _: String)(_: TextAndKey, _: Converter[Month]))
        .expects(encryptedDeductionPeriod.value, encryptedDeductionPeriod.nonce, textAndKey, *).returning(Month.JULY)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedGrossAmountPaid.value, encryptedGrossAmountPaid.nonce, textAndKey, *).returning(value = 100.0)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedDeductionAmount.value, encryptedDeductionAmount.nonce, textAndKey, *).returning(value = 200.0)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedCostOfMaterialsQuestion.value, encryptedCostOfMaterialsQuestion.nonce, textAndKey, *).returning(value = true)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedCostOfMaterials.value, encryptedCostOfMaterials.nonce, textAndKey, *).returning(value = 300.0)


      val encryptedData = EncryptedCYAPeriodData(
        deductionPeriod = encryptedDeductionPeriod,
        grossAmountPaid = Some(encryptedGrossAmountPaid),
        deductionAmount = Some(encryptedDeductionAmount),
        costOfMaterialsQuestion = Some(encryptedCostOfMaterialsQuestion),
        costOfMaterials = Some(encryptedCostOfMaterials),
      )

      encryptedData.decrypted shouldBe CYAPeriodData(
        deductionPeriod = Month.JULY,
        grossAmountPaid = Some(100.0),
        deductionAmount = Some(200.0),
        costOfMaterialsQuestion = Some(true),
        costOfMaterials = Some(300.0)
      )
    }
  }
}
