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

package controllers

import actions.ActionsProvider
import com.google.inject.Inject
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.routes.{ContractorCYAController, DeductionPeriodController}
import forms.ContractorDetailsForm.contractorDetailsForm
import models.User
import models.forms.ContractorDetails
import models.mongo.CisUserData
import models.pages.ContractorDetailsPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.ContractorDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.ContractorDetailsView

import scala.concurrent.{ExecutionContext, Future}

// TODO: Refactor controller
class ContractorDetailsController @Inject()(actionsProvider: ActionsProvider,
                                            contractorDetailsView: ContractorDetailsView,
                                            contractorDetailsService: ContractorDetailsService,
                                            errorHandler: ErrorHandler)
                                           (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def handlePriorEmployerRefs(taxYear: Int, user: User)(implicit hc: HeaderCarrier, request: Request[_]): Future[Either[Result, Seq[String]]] = {
    contractorDetailsService.getPriorEmployerRefs(taxYear, user).map {
      case Left(error) => Left(errorHandler.handleError(error.status))
      case Right(employerRefs) => Right(employerRefs)
    }
  }

  // TODO: I think getting the employerRefs for show does not make sense
  def show(taxYear: Int, contractor: Option[String]): Action[AnyContent] = contractor match {
    case None => actionsProvider.endOfYear(taxYear).async { implicit request =>
      handlePriorEmployerRefs(taxYear, request.user).map {
        case Left(result) => result
        case Right(employerRefs) =>
          Ok(contractorDetailsView(ContractorDetailsPage(taxYear, request.user.isAgent, contractorDetailsForm(request.user.isAgent, employerRefs), None)))
      }
    }
    case Some(contractorRef) => actionsProvider.endOfYearWithSessionData(taxYear, contractorRef, redirectIfPrior = true).async { implicit request =>
      handlePriorEmployerRefs(taxYear, request.user).map {
        case Left(result) => result
        case Right(employerRefs) =>
          val formData = ContractorDetails(request.cisUserData.cis.contractorName.getOrElse(""), request.cisUserData.employerRef)
          val form = contractorDetailsForm(request.user.isAgent, employerRefs).fill(formData)
          Ok(contractorDetailsView(ContractorDetailsPage(taxYear, request.user.isAgent, form, Some(request.cisUserData.employerRef))))
      }
    }
  }

  def submit(taxYear: Int, contractor: Option[String]): Action[AnyContent] = contractor match {
    case None => actionsProvider.endOfYear(taxYear).async { implicit request =>
      handlePriorEmployerRefs(taxYear, request.user).flatMap {
        case Left(result) => Future.successful(result)
        case Right(employerRefs) => handleSubmitRequest(taxYear, request.user, employerRefs = employerRefs)
      }
    }
    case Some(contractorRef) => actionsProvider.endOfYearWithSessionData(taxYear, contractorRef, redirectIfPrior = true).async { implicit request =>
      handlePriorEmployerRefs(taxYear, request.user).flatMap {
        case Left(result) => Future.successful(result)
        case Right(employerRefs) => handleSubmitRequest(taxYear, request.user, Some(request.cisUserData), employerRefs = employerRefs)
      }
    }
  }

  def handleSubmitRequest(taxYear: Int,
                          user: User,
                          optCisUserData: Option[CisUserData] = None,
                          employerRefs: Seq[String])(implicit request: Request[_]): Future[Result] = {
    contractorDetailsForm(user.isAgent, employerRefs).bindFromRequest().fold(
      formWithErrors =>
        Future(BadRequest(contractorDetailsView(ContractorDetailsPage(taxYear, user.isAgent, formWithErrors, optCisUserData.map(_.employerRef))))),
      contractorDetails => contractorDetailsService.saveContractorDetails(taxYear, user, optCisUserData, contractorDetails).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(cisUserData) => getRedirect(taxYear, cisUserData)
      }
    )
  }

  private def getRedirect(taxYear: Int, cisUserData: CisUserData)
                         (implicit requestHeader: RequestHeader): Result = {
    val contractor = cisUserData.employerRef
    if (cisUserData.isFinished) {
      val month = cisUserData.cis.periodData.get.deductionPeriod.toString.toLowerCase
      Redirect(ContractorCYAController.show(taxYear, month, contractor))
    } else {
      Redirect(DeductionPeriodController.show(taxYear, contractor)).addingToSession(SessionValues.TEMP_EMPLOYER_REF -> contractor)
    }
  }
}
