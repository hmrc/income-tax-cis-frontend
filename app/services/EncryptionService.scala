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
import utils.{Month, SecureGCMCipher}

class EncryptionService @Inject()(secureGCMCipher: SecureGCMCipher, appConfig: AppConfig) {

  def encryptUserData(userData: CisUserData): EncryptedCisUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EncryptedCisUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employerRef = userData.employerRef,
      submissionId = userData.submissionId,
      isPriorSubmission = userData.isPriorSubmission,
      cis = userData.cis.map(encryptCisData),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptCisData(e: CisCYAModel)(implicit textAndKey: TextAndKey): EncryptedCisCYAModel = {
    EncryptedCisCYAModel(
      contractorName = secureGCMCipher.encrypt(e.contractorName),
      periodData = e.periodData.map(encryptPeriodData)
    )
  }

  private def decryptCisData(e: EncryptedCisCYAModel)(implicit textAndKey: TextAndKey): CisCYAModel = {
    CisCYAModel(
      contractorName = secureGCMCipher.decrypt[String](e.contractorName.value, e.contractorName.nonce),
      periodData = e.periodData.map(decryptPeriodData)
    )
  }

  private def encryptPeriodData(e: CYAPeriodData)(implicit textAndKey: TextAndKey): EncryptedCYAPeriodData = {
    EncryptedCYAPeriodData(
      deductionFromDate = secureGCMCipher.encrypt(e.deductionFromDate),
      deductionToDate = secureGCMCipher.encrypt(e.deductionToDate),
      grossAmountPaid = e.grossAmountPaid.map(secureGCMCipher.encrypt),
      deductionAmount = e.deductionAmount.map(secureGCMCipher.encrypt),
      costOfMaterialsQuestion = e.costOfMaterialsQuestion.map(secureGCMCipher.encrypt),
      costOfMaterials = e.costOfMaterials.map(secureGCMCipher.encrypt)
    )
  }

  private def decryptPeriodData(e: EncryptedCYAPeriodData)(implicit textAndKey: TextAndKey): CYAPeriodData = {
    CYAPeriodData(
      deductionFromDate = secureGCMCipher.decrypt[Month](e.deductionFromDate.value, e.deductionFromDate.nonce),
      deductionToDate = secureGCMCipher.decrypt[Month](e.deductionToDate.value, e.deductionToDate.nonce),
      grossAmountPaid = e.grossAmountPaid.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      deductionAmount = e.deductionAmount.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      costOfMaterialsQuestion = e.costOfMaterialsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      costOfMaterials = e.costOfMaterials.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  def decryptUserData(userData: EncryptedCisUserData): CisUserData = {

    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    CisUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      employerRef = userData.employerRef,
      submissionId = userData.submissionId,
      isPriorSubmission = userData.isPriorSubmission,
      cis = userData.cis.map(decryptCisData),
      lastUpdated = userData.lastUpdated
    )
  }
}
