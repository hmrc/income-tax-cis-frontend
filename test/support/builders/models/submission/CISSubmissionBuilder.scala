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

package support.builders.models.submission

import models.submission.CISSubmission
import support.builders.models.submission.PeriodDataBuilder.aPeriodData

object CISSubmissionBuilder {
  val aCISSubmission: CISSubmission = CISSubmission(
    employerRef = Some("123/AB123456"),
    contractorName = Some("ABC Steelworks"),
    periodData = Seq(aPeriodData),
    submissionId = None
  )
}
