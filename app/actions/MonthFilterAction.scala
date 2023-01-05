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

package actions

import config.ErrorHandler
import models.AuthorisationRequest
import play.api.mvc.{ActionFilter, Result}

import java.time.Month
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class MonthFilterAction(monthValue: String,
                             errorHandler: ErrorHandler)
                            (implicit ec: ExecutionContext) extends ActionFilter[AuthorisationRequest] {

  override protected[actions] def executionContext: ExecutionContext = ec

  override protected[actions] def filter[A](input: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
    Try(Month.valueOf(monthValue.toUpperCase)) match {
      case Failure(_) => Some(errorHandler.internalServerError()(input))
      case Success(_) => None
    }
  }
}
