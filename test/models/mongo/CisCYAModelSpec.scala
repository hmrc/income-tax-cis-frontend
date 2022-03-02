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

import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

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
    "return true when CYAPeriodData is not empty" in {
      aCisCYAModel.isFinished shouldBe true
    }

    "return false when CYAPeriodData is empty" in {
      val result = aCisCYAModel.copy(periodData = Seq())
      result.isFinished shouldBe false
    }
  }

  "CisCYAModel.encrypted" should {
    "return EncryptedCisCYAModel" in {
      val underTest = CisCYAModel("some-contractor-name", Seq(cyaPeriodData1, cyaPeriodData2))

      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(underTest.contractorName, textAndKey).returning(encryptedContractorName)
      (cyaPeriodData1.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedPeriodData1)
      (cyaPeriodData2.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedPeriodData2)

      underTest.encrypted shouldBe EncryptedCisCYAModel(
        contractorName = encryptedContractorName,
        periodData = Seq(encryptedPeriodData1, encryptedPeriodData2)
      )
    }
  }

  "EncryptedCisCYAModel.decrypted" should {
    "return CisCYAModel" in {
      (secureGCMCipher.decrypt[String](_: String, _: String)(_: TextAndKey, _: Converter[String]))
        .expects(encryptedContractorName.value, encryptedContractorName.nonce, textAndKey, *).returning(value = "contractor-name")
      (encryptedPeriodData1.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(cyaPeriodData1)
      (encryptedPeriodData2.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(cyaPeriodData2)

      EncryptedCisCYAModel(encryptedContractorName, Seq(encryptedPeriodData1, encryptedPeriodData2)).decrypted shouldBe
        CisCYAModel(contractorName = "contractor-name", periodData = Seq(cyaPeriodData1, cyaPeriodData2))
    }
  }
}
