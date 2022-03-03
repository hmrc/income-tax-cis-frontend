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

package utils

import models.mongo.TextAndKey
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, monthEncryptor, stringEncryptor}

import java.time.Month

class EncryptorInstancesTest extends UnitTest
  with MockFactory {

  private val encryptedBoolean = mock[EncryptedValue]
  private val encryptedString = mock[EncryptedValue]
  private val encryptedBigDecimal = mock[EncryptedValue]
  private val encryptedMonth = mock[EncryptedValue]

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  "booleanEncryptor" should {
    "encrypt boolean values" in {
      val someBoolean = true
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(someBoolean, textAndKey).returning(encryptedBoolean)

      booleanEncryptor.encrypt(someBoolean) shouldBe encryptedBoolean
    }
  }

  "stringEncryptor" should {
    "encrypt string values" in {
      val stringValue = "some-string-value"

      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(stringValue, textAndKey).returning(encryptedString)

      stringEncryptor.encrypt(stringValue) shouldBe encryptedString
    }
  }

  "bigDecimalEncryptor" should {
    "encrypt BigDecimal values" in {
      val bigDecimalValue: BigDecimal = 500.0

      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(bigDecimalValue, textAndKey).returning(encryptedBigDecimal)

      bigDecimalEncryptor.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }
  }

  "monthEncryptor" should {
    "encrypt Month values" in {
      val monthValue: Month = Month.MAY

      (secureGCMCipher.encrypt(_: Month)(_: TextAndKey)).expects(monthValue, textAndKey).returning(encryptedMonth)

      monthEncryptor.encrypt(monthValue) shouldBe encryptedMonth
    }
  }
}
