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

import models.{ServiceError, User}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorCYAService @Inject()(cisSessionService: CISSessionService)(implicit val ec: ExecutionContext) {

  def submitCisDeductionCYA(taxYear: Int, employerRef: String, user: User
                            //, cisUserData: CisUserData*/)(implicit hc: HeaderCarrier) TODO Uncomment when doing the post orchestration
                           ): Future[Either[ServiceError, Unit]] = {

    //TODO Create payload for POST orchestration from cis user data & send to cis BE connector

    //TODO Refresh cis cached prior data

    cisSessionService.clear(user,employerRef,taxYear).map {
      case Left(error) => Left(error)
      case Right(_) => Right(())
    }
  }
}
