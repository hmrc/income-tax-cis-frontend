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
import common.SessionValues.TEMP_EMPLOYER_REF
import config.{AppConfig, ErrorHandler}
import controllers.routes.LabourPayController
import forms.DeductionPeriodFormProvider
import models.UserSessionDataRequest
import models.forms.DeductionPeriod
import models.mongo.CisUserData
import models.pages.DeductionPeriodPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DeductionPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.cis.DeductionPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionPeriodController @Inject()(actionsProvider: ActionsProvider,
                                          view: DeductionPeriodView,
                                          deductionPeriodService: DeductionPeriodService,
                                          errorHandler: ErrorHandler,
                                          formProvider: DeductionPeriodFormProvider)
                                         (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(
    taxYear, contractor, needsPeriodData = false).async { implicit request =>

    val cya = request.cisUserData
    val pageModel = DeductionPeriodPage(cya.cis.contractorName, cya.employerRef, taxYear,
      cya.cis.periodData.map(_.deductionPeriod), cya.cis.priorPeriodData.map(_.deductionPeriod))

    validatePageAccess(taxYear, pageModel, cya) match {
      case Some(redirect) => redirect
      case None => Future.successful(Ok(view(pageModel, formProvider.deductionPeriodForm(request.user.isAgent, pageModel.priorSubmittedPeriods))))
    }
  }

  def submit(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(
    taxYear, contractor, needsPeriodData = false).async { implicit request =>

    val cya = request.cisUserData
    val pageModel = DeductionPeriodPage(cya.cis.contractorName, cya.employerRef, taxYear,
      cya.cis.periodData.map(_.deductionPeriod), cya.cis.priorPeriodData.map(_.deductionPeriod))

    validatePageAccess(taxYear, pageModel, cya, "submit") match {
      case Some(redirect) => redirect
      case None =>
        val form = formProvider.deductionPeriodForm(request.user.isAgent, pageModel.priorSubmittedPeriods)
        handleForm(taxYear, contractor, form, pageModel)
    }
  }

  private def validatePageAccess(taxYear: Int, pageModel: DeductionPeriodPage, cya: CisUserData, method: String = "show"): Option[Future[Result]] = {
    lazy val overviewRedirect = Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))

    if (pageModel.noPeriodsToSubmitFor) {
      noAvailableDeductionPeriodsLog(method)
      Some(overviewRedirect)
    } else if (cya.cis.periodData.exists(_.contractorSubmitted)) {
      cannotChangeContractorSubmittedPeriodLog(method)
      Some(overviewRedirect)
    } else {
      None
    }
  }

  private def handleForm(taxYear: Int, employerRef: String, form: Form[DeductionPeriod], pageModel: DeductionPeriodPage)
                        (implicit request: UserSessionDataRequest[_]): Future[Result] = {

    val tempEmployerRef = request.session.get(TEMP_EMPLOYER_REF)

    form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(pageModel, formWithErrors))),
      deductionPeriod => deductionPeriodService.submitDeductionPeriod(taxYear, employerRef, request.user, deductionPeriod.month, tempEmployerRef).map {
        case Left(_) => errorHandler.internalServerError
        case Right(_) => Redirect(LabourPayController.show(taxYear, deductionPeriod.month.toString, employerRef))
      }
    )
  }

  private def noAvailableDeductionPeriodsLog(method: String): Unit = logger.info(
    s"[DeductionPeriodController][$method] User has no more deduction periods to submit for. Redirecting.")

  private def cannotChangeContractorSubmittedPeriodLog(method: String): Unit = logger.info(
    s"[DeductionPeriodController][$method] User cannot change contractor submitted period. Redirecting.")
}
