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

import builders.models.mongo.CisUserDataBuilder.aCisUserData
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import utils.SecureGCMCipher

class CisUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val cisCYAModel = mock[CisCYAModel]
  private val encryptedCisCYAModel = mock[EncryptedCisCYAModel]

  "CisUserData.encrypted" should {
    "return EncryptedCisUserData object" in {
      val underTest = aCisUserData.copy(cis = Some(cisCYAModel))

      (cisCYAModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(secureGCMCipher, textAndKey).returning(encryptedCisCYAModel)

      underTest.encrypted() shouldBe EncryptedCisUserData(
        sessionId = underTest.sessionId,
        mtdItId = underTest.mtdItId,
        nino = underTest.nino,
        taxYear = underTest.taxYear,
        employerRef = underTest.employerRef,
        submissionId = underTest.submissionId,
        isPriorSubmission = underTest.isPriorSubmission,
        cis = Some(encryptedCisCYAModel),
        lastUpdated = underTest.lastUpdated
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
        cis = Some(encryptedCisCYAModel),
        lastUpdated = DateTime.now
      )

      (encryptedCisCYAModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(secureGCMCipher, textAndKey).returning(cisCYAModel)

      underTest.decrypted shouldBe CisUserData(
        sessionId = underTest.sessionId,
        mtdItId = underTest.mtdItId,
        nino = underTest.nino,
        taxYear = underTest.taxYear,
        employerRef = underTest.employerRef,
        submissionId = underTest.submissionId,
        isPriorSubmission = underTest.isPriorSubmission,
        cis = Some(cisCYAModel),
        lastUpdated = underTest.lastUpdated
      )
    }
  }
}
