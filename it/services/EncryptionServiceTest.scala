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

package services

import builders.models.mongo.CisUserDataBuilder.aCisUserData
import models.mongo.EncryptedCisUserData
import utils.IntegrationTest

class EncryptionServiceTest extends IntegrationTest {

  private val underTest: EncryptionService = app.injector.instanceOf[EncryptionService]

  "encryptUserData" should {
    val data = aCisUserData

    "encrypt relevant cis user data" in {
      val result = underTest.encryptUserData(aCisUserData)

      result shouldBe EncryptedCisUserData(
        sessionId = data.sessionId,
        mtdItId = data.mtdItId,
        nino = data.nino,
        taxYear = data.taxYear,
        employerRef = data.employerRef,
        submissionId = data.submissionId,
        isPriorSubmission = data.isPriorSubmission,
        cis = result.cis,
        lastUpdated = result.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = underTest.encryptUserData(data)
      val decryptResult = underTest.decryptUserData(encryptResult)

      decryptResult shouldBe data
    }
  }
}
