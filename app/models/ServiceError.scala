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

package models

import scala.util.control.NoStackTrace

trait ServiceError

case class HttpParserError(status: Int) extends ServiceError
case object EmptyPriorCisDataError extends ServiceError
case object InvalidOrUnfinishedSubmission extends ServiceError
case object FailedTailoringRemoveDeductionError extends ServiceError
case object FailedTailoringOverrideDeductionError extends ServiceError
case class MissingAgentClientDetails(message: String) extends Exception(message) with NoStackTrace with ServiceError
