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

import connectors.{IncomeTaxUserDataConnector, RefreshIncomeSourceConnector}
import models._
import models.mongo.{CisCYAModel, CisUserData, DataNotUpdatedError, DatabaseError}
import org.joda.time.DateTimeZone
import play.api.Logging
import repositories.CisUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CISSessionService @Inject()(cisUserDataRepository: CisUserDataRepository,
                                  incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                  refreshIncomeSourceConnector: RefreshIncomeSourceConnector,
                                  clock: Clock)(implicit val ec: ExecutionContext) extends Logging {

  // TODO: Check why we need the tempEmployerRef. Think of a way to get rid of it.
  def getSessionData(taxYear: Int, employerRef: String, user: User, tempEmployerRef: Option[String]): Future[Either[DatabaseError, Option[CisUserData]]] = {

    cisUserDataRepository.find(taxYear, employerRef, user).flatMap {
      case Right(None) if tempEmployerRef.isDefined => cisUserDataRepository.find(taxYear, tempEmployerRef.get, user)
      case response => Future.successful(response)
    }
  }

  def createOrUpdateCISUserData(user: User,
                                taxYear: Int,
                                employerRef: String,
                                submissionId: Option[String],
                                isPriorSubmission: Boolean,
                                cisCYA: CisCYAModel): Future[Either[DatabaseError, CisUserData]] = {
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
      case Left(_) => Left(DataNotUpdatedError)
    }
  }

  def getPriorData(user: User, taxYear: Int)(implicit hc: HeaderCarrier): Future[Either[HttpParserError, IncomeTaxUserData]] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders(headers = "mtditid" -> user.mtditid)).map {
      case Left(error) => Left(HttpParserError(error.status))
      case Right(incomeTaxUserData) => Right(incomeTaxUserData)
    }
  }

  def refreshAndClear(user: User, employerRef: String, taxYear: Int, clearCYA: Boolean = true)
                     (implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    refreshIncomeSourceConnector.put(taxYear, user.nino)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(HttpParserError(error.status)))
      case _ =>
        if (clearCYA) {
          cisUserDataRepository.clear(taxYear, employerRef, user).map {
            case true => Right(())
            case false => Left(DataNotUpdatedError)
          }
        } else {
          Future.successful(Right(()))
        }
    }
  }

  def checkCyaAndReturnData(taxYear: Int, employerRef: String, user: User, month: Month, tempEmployerRef: Option[String])
                           (implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[CisUserData]]] = {
    getSessionData(taxYear, employerRef, user, tempEmployerRef).flatMap {
      case Right(data@Some(cyaData)) if cyaData.cis.isAnUpdateFor(month) =>
        logger.info("[CISSessionService][checkCyaAndReturnData] CYA data is for updates being made to an existing period.")
        Future.successful(Right(data))
      case Right(data@Some(cyaData)) if cyaData.cis.isNewSubmissionFor(month) =>
        logger.info("[CISSessionService][checkCyaAndReturnData] CYA data is a new period submission")
        Future.successful(Right(data))
      case Right(_) => getPriorAndSaveAsCYA(taxYear, employerRef, month, user)
      case Left(error) => Future.successful(Left(error))
    }
  }

  private def getPriorAndSaveAsCYA(taxYear: Int, employerRef: String, month: Month, user: User)
                                  (implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[CisUserData]]] = {
    getPriorData(user, taxYear).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(prior) => createAndSaveCYA(taxYear, employerRef, month, prior, user)
    }
  }

  private def createAndSaveCYA(taxYear: Int, employerRef: String, month: Month,
                               prior: IncomeTaxUserData, user: User): Future[Either[ServiceError, Option[CisUserData]]] = {
    logger.info("[CISSessionService][createAndSaveCYA] Creating CYA data from prior data and saving.")

    val deductions = prior.endOfYearCisDeductionsWith(employerRef, month)

    deductions match {
      case Some(deductions) =>
        val submissionId: Option[String] = deductions.submissionId
        val cya = deductions.toCYA(Some(month), prior.contractorPeriodsFor(employerRef), hasCompleted = true)

        createOrUpdateCISUserData(user, taxYear, employerRef, submissionId, isPriorSubmission = true, cya).map {
          case Left(_) => Left(DataNotUpdatedError)
          case Right(data) => Right(Some(data))
        }
      case None => Future.successful(Right(None))
    }
  }
}
