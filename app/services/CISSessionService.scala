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

import connectors.IncomeTaxUserDataConnector
import models._
import models.mongo.{CisCYAModel, CisUserData, DataNotUpdatedError, DatabaseError}
import org.joda.time.DateTimeZone
import repositories.CisUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CISSessionService @Inject()(cisUserDataRepository: CisUserDataRepository,
                                  incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                  clock: Clock)(implicit val ec: ExecutionContext) {

  def getSessionData(taxYear: Int, employerRef: String, user: User): Future[Either[DatabaseError, Option[CisUserData]]] = {
    cisUserDataRepository.find(taxYear, employerRef, user)
  }

  def createOrUpdateCISUserData(user: User,
                                taxYear: Int,
                                employerRef: String,
                                submissionId: Option[String],
                                isPriorSubmission: Boolean,
                                cisCYA: CisCYAModel): Future[Either[Unit, CisUserData]] = {
    val cisUserData = CisUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      employerRef,
      submissionId,
      isPriorSubmission,
      cisCYA,
      clock.now(DateTimeZone.UTC)
    )

    cisUserDataRepository.createOrUpdate(cisUserData).map {
      case Right(_) => Right(cisUserData)
      case Left(_) => Left(())
    }
  }

  def getPriorData(user: User, taxYear: Int)(implicit hc: HeaderCarrier): Future[Either[HttpParserError, IncomeTaxUserData]] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).map {
      case Left(error) => Left(HttpParserError(error.status))
      case Right(incomeTaxUserData) => Right(incomeTaxUserData)
    }
  }

  def getPriorAndMakeCYA(taxYear: Int, employerRef: String, user: User)
                        (implicit headerCarrier: HeaderCarrier): Future[Either[ServiceError, CisUserData]] = {

    getPriorData(user, taxYear).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(prior: IncomeTaxUserData) =>
        prior.eoyCisDeductionsWith(employerRef) match {
          case Some(deductions) =>

            val submissionId: Option[String] = deductions.submissionId
            val cya = deductions.toCYA

            createOrUpdateCISUserData(user, taxYear, employerRef, submissionId, isPriorSubmission = true, cya).map {
              case Left(_) => Left(DataNotUpdatedError)
              case Right(value) => Right(value)
            }

          case None => Future.successful(Left(EmptyPriorCisDataError))
        }
    }
  }
}
