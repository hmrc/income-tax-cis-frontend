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

import actions.{AuthorisedAction, TaxYearAction}
import config.{AppConfig, ErrorHandler}
import controllers.routes.LabourPayController
import forms.DeductionPeriodFormProvider
import models.forms.DeductionPeriod
import models.pages.DeductionPeriodPage
import models.{AuthorisationRequest, EmptyPriorCisDataError, HttpParserError}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{CISSessionService, DeductionPeriodService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.UrlUtils.{decoded, encoded}
import utils.{InYearUtil, SessionHelper}
import views.html.cis.DeductionPeriodView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionPeriodController @Inject()(authorisedAction: AuthorisedAction,
                                          view: DeductionPeriodView,
                                          inYearAction: InYearUtil,
                                          deductionPeriodService: DeductionPeriodService,
                                          errorHandler: ErrorHandler,
                                          formProvider: DeductionPeriodFormProvider,
                                          cisSessionService: CISSessionService,
                                          implicit val mcc: MessagesControllerComponents,
                                          implicit val ec: ExecutionContext,
                                          implicit val appConfig: AppConfig) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, contractor: String): Action[AnyContent] = (authorisedAction andThen TaxYearAction.taxYearAction(taxYear)).async { implicit request =>

    val employerRef = decoded(contractor)
    val inYear: Boolean = inYearAction.inYear(taxYear)

    if (inYear) {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      getPriorAndMakeCYA(taxYear, employerRef).flatMap { //
        case Left(result) => Future.successful(result) // TODO Remove once able to hit this page from CYA / Contractor details page (only needs pageModelFor)
        case Right(_) => //

          deductionPeriodService.pageModelFor(taxYear, employerRef, request.user).map {
            case Left(_) => defaultErrorPage
            case Right(None) => noCYALog()
              Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

            case Right(Some(pageModel)) =>
              //NO NEW PERIODS CAN BE ADDED -> REDIRECT
              if (pageModel.priorSubmittedPeriods.length == Month.values().length) {
                noAvailableDeductionPeriodsLog()
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              } else {
                Ok(view(pageModel, formProvider.deductionPeriodForm(request.user.isAgent, pageModel.priorSubmittedPeriods)))
              }
          }
      }
    }
  }

  /// TODO REMOVE TEMPORARY MAKE CYA FROM PRIOR - We will have CYA before this page either from CYA or following on from the name & employer ref page ///
  private def getPriorAndMakeCYA(taxYear: Int, employerRef: String)(implicit request: AuthorisationRequest[_]): Future[Either[Result, Unit]] = {
    deductionPeriodService.pageModelFor(taxYear, employerRef, request.user).flatMap {
      case Right(Some(_)) => Future.successful(Right(()))
      case _ =>
        cisSessionService.getPriorAndMakeCYA(taxYear, employerRef, request.user).map {
          case Left(HttpParserError(status)) => Left(errorHandler.handleError(status))
          case Left(EmptyPriorCisDataError) =>
            logger.info("[DeductionPeriodController][show] User has no prior data redirecting to overview page.")
            Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          case Left(_) => Left(defaultErrorPage)
          case Right(_) => Right(())
        }
    }
  }

  def submit(taxYear: Int, contractor: String): Action[AnyContent] = authorisedAction.async { implicit request =>

    lazy val method = "submit"
    val employerRef = decoded(contractor)
    val inYear: Boolean = inYearAction.inYear(taxYear)

    if (inYear) {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      deductionPeriodService.pageModelFor(taxYear, employerRef, request.user).flatMap {
        case Left(_) => Future.successful(defaultErrorPage)
        case Right(None) => noCYALog(method)
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

        case Right(Some(pageModel)) =>
          //NO NEW PERIODS CAN BE ADDED -> REDIRECT
          if (pageModel.priorSubmittedPeriods.length == Month.values().length) {
            noAvailableDeductionPeriodsLog(method)
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          } else {
            val form = formProvider.deductionPeriodForm(request.user.isAgent, pageModel.priorSubmittedPeriods)
            handleForm(taxYear, employerRef, form, pageModel)
          }
      }
    }
  }

  private def handleForm(taxYear: Int, employerRef: String, form: Form[DeductionPeriod], pageModel: DeductionPeriodPage)
                        (implicit request: AuthorisationRequest[_]): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(pageModel, formWithErrors))),
      deductionPeriod =>

        deductionPeriodService.submitDeductionPeriod(taxYear, employerRef, request.user, deductionPeriod.month).map {
          case Left(_) => defaultErrorPage
          case Right(_) =>
            Redirect(LabourPayController.show(taxYear, deductionPeriod.month.toString, encoded(employerRef)))
        }
    )
  }

  private def defaultErrorPage(implicit request: AuthorisationRequest[_]): Result = errorHandler.internalServerError

  private def noAvailableDeductionPeriodsLog(method: String = "show"): Unit = logger.info(
    s"[DeductionPeriodController][$method] User has no more deduction periods to submit for. Redirecting.")

  private def noCYALog(method: String = "show"): Unit = logger.info(
    s"[DeductionPeriodController][$method] User has no CYA data redirecting to overview page.")
}
