/*
 * Copyright 2024 HM Revenue & Customs
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

package support.mocks

import models.mongo.CisUserData
import models.{ServiceError, User}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.LabourPayService

import scala.concurrent.Future

trait MockLabourPayService { this: TestSuite =>


  protected val mockLabourPayService: LabourPayService =
    org.mockito.Mockito.mock(classOf[LabourPayService])

  def mockSaveLabourPay(user: User,
                        cisUserData: CisUserData,
                        amount: BigDecimal,
                        result: Either[ServiceError, CisUserData]): Unit = {
    when(
      mockLabourPayService.saveLabourPay(
        eqTo(user),
        eqTo(cisUserData),
        eqTo(amount)
      )
    ).thenReturn(Future.successful(result))
  }
}
