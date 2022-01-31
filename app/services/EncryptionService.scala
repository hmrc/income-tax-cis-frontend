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

import config.AppConfig
import javax.inject.Inject
import models.mongo._
import utils.SecureGCMCipher

class EncryptionService @Inject()(secureGCMCipher: SecureGCMCipher, appConfig: AppConfig) {

  def encryptUserData(userData: CisUserData): EncryptedCisUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EncryptedCisUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      cis = userData.cis.map(secureGCMCipher.encrypt(_)),
      lastUpdated = userData.lastUpdated
    )
  }

  def decryptUserData(userData: EncryptedCisUserData): CisUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    CisUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      cis = userData.cis.map(x => secureGCMCipher.decrypt[String](x.value,x.nonce)),
      lastUpdated = userData.lastUpdated
    )
  }
}
