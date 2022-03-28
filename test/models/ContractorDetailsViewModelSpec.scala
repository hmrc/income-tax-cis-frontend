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

package models

import models.pages.ContractorDetailsViewModel
import play.api.libs.json.Json
import support.UnitTest

class ContractorDetailsViewModelSpec extends UnitTest{

  "ContractorDetailsViewModel" should {
    "write to json correctly when using implicit writes" in {
      val result = Json.toJson(ContractorDetailsViewModel("ABC Steelworks", "123/AB12345"))
      val aContractorDetailsViewModel = Json.obj("contractorName" -> "ABC Steelworks",
        "employerReferenceNumber" -> "123/AB12345")

      result shouldBe aContractorDetailsViewModel
    }
  }
}
