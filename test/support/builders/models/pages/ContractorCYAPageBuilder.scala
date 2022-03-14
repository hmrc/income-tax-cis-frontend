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

package support.builders.models.pages

import models.pages.ContractorCYAPage
import support.TaxYearHelper

import java.time.Month

object ContractorCYAPageBuilder extends TaxYearHelper {

  val aContractorCYAPage: ContractorCYAPage = ContractorCYAPage(
    taxYear = taxYear,
    isInYear = true,
    contractorName = Some("default-contractor-name"),
    employerRef = "default-employer-ref",
    month = Month.MAY,
    labourAmount = Some(100),
    deductionAmount = Some(200),
    costOfMaterials = Some(300)
  )
}
