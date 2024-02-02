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

package controllers

import actions.ActionsProvider
import com.google.inject.Inject
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.routes.{ContractorCYAController, DeductionPeriodController}
import forms.ContractorDetailsForm.contractorDetailsForm
import models.{AuthorisationRequest, User}
import models.forms.ContractorDetails
import models.mongo.CisUserData
import models.pages.ContractorDetailsPage
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CISSessionService, ContractorDetailsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.ContractorDetailsView

import scala.concurrent.{ExecutionContext, Future}

// TODO: Refactor controller
class ContractorDetailsController @Inject()(actionsProvider: ActionsProvider,
                                            contractorDetailsView: ContractorDetailsView,
                                            contractorDetailsService: ContractorDetailsService,
                                            errorHandler: ErrorHandler,
                                            cisSessionService: CISSessionService)
                                           (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  // TODO: I think getting the employerRefs for show does not make sense
  def show(taxYear: Int, contractor: Option[String]): Action[AnyContent] =
    actionsProvider.endOfYear(taxYear).async { implicit request =>
      contractorDetailsService.getPriorEmployerRefs(taxYear, request.user).flatMap {
        case Left(err) => Future.successful(errorHandler.handleError(err.status))
        case Right(refs) =>
          // The employer ref is used as the index to read data out of Mongo. We must prefer the one in the session cookie
          // value over the one in the query string because the latter may be out of date. That is, it can refer to a wrong
          // (earlier) Mongo record because the user has changed the employer ref and a new record has been created.

          // When thinking about what happens when the user hits Back, remember that the URL in the browser's history (and
          // the value in its query string) is frozen, whereas the one in the cookie can be updated on the side - this is
          // a bit like re-playing old function calls which rely on external state which isn't passed in (side-effects).
          // The current design has to work this way. If you want to get different (populated) fields in the HTML even
          // though you GET the same endpoint, this has to rely on some side-mechanism. That's why a better design would
          // be to create a session ID at the start of the journey that is outside the user's control.
          val maybeEmployerRef = getFromSession(SessionValues.TEMP_EMPLOYER_REF).orElse(contractor)

          val isAgent = request.user.isAgent

          maybeEmployerRef match {
            case Some(employerRef) =>
              cisSessionService.getSessionData(taxYear, employerRef, request.user).map {
                case Left(_) => errorHandler.handleError(INTERNAL_SERVER_ERROR)
                case Right(Some(data)) if data.isPriorSubmission => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
                case Right(maybeData) => okFromMaybeNonpriorData(maybeData, taxYear, isAgent, refs)
              }
            case None => Future.successful(okFromMaybeNonpriorData(None, taxYear, isAgent, refs))
          }
      }
    }

  private def okFromMaybeNonpriorData(maybeData: Option[CisUserData], taxYear: Int, isAgent: Boolean, refs: Seq[String])
                                     (implicit request: AuthorisationRequest[_]): Result = {
    val emptyForm = contractorDetailsForm(isAgent, refs)
    val (finishedForm, maybeEmployerRef) =
      maybeData.map(data => (emptyForm.fill(contractorDetailsFromData(data)), Some(data.employerRef))).getOrElse((emptyForm, None))
    Ok(contractorDetailsView(ContractorDetailsPage(taxYear, isAgent, finishedForm, maybeEmployerRef)))
  }

  private def contractorDetailsFromData(data: CisUserData): ContractorDetails =
    ContractorDetails(data.cis.contractorName.getOrElse(""), data.employerRef)

  def submit(taxYear: Int, contractor: Option[String]): Action[AnyContent] =
    actionsProvider.endOfYear(taxYear).async { implicit request =>
      contractorDetailsService.getPriorEmployerRefs(taxYear, request.user).flatMap {
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
        case Right(refs) =>
          contractorDetailsForm(request.user.isAgent, refs).bindFromRequest().fold(
            formWithErrors => {
              val maybeEmployerRef = getFromSession(SessionValues.TEMP_EMPLOYER_REF).orElse(contractor)
              Future.successful(BadRequest(contractorDetailsView(ContractorDetailsPage(taxYear, request.user.isAgent, formWithErrors, maybeEmployerRef))))
            },
            formValues =>
              cisSessionService.getSessionData(taxYear, formValues.employerReferenceNumber, request.user).flatMap {
                case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
                case Right(maybeData) => handleSubmitRequest(taxYear, request.user, maybeData, employerRefs = refs)
              }
          )
      }
    }

  private def handleSubmitRequest(taxYear: Int,
                                  user: User,
                                  optCisUserData: Option[CisUserData],
                                  employerRefs: Seq[String])(implicit request: Request[_]): Future[Result] =
    contractorDetailsForm(user.isAgent, employerRefs).bindFromRequest().fold(
      formWithErrors =>
        Future(BadRequest(contractorDetailsView(ContractorDetailsPage(taxYear, user.isAgent, formWithErrors, optCisUserData.map(_.employerRef))))),
      contractorDetails => contractorDetailsService.saveContractorDetails(taxYear, user, optCisUserData, contractorDetails).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(cisUserData) => getRedirect(taxYear, cisUserData)
      }
    )

  private def getRedirect(taxYear: Int, cisUserData: CisUserData)
                         (implicit requestHeader: RequestHeader): Result = {
    val contractor = cisUserData.employerRef
    if (cisUserData.isFinished) {
      val month = cisUserData.cis.periodData.get.deductionPeriod.toString.toLowerCase
      Redirect(ContractorCYAController.show(taxYear, month, contractor))
    } else {
      // This is where we update the record index (employer ref) stored in the session cookie. Note that if the user
      // goes back and forth in history and re-submits this page with a modified employer ref, this session cookie value
      // will be updated but the one in the query string will not.
      Redirect(DeductionPeriodController.show(taxYear, contractor)).addingToSession(SessionValues.TEMP_EMPLOYER_REF -> contractor)
    }
  }
}
