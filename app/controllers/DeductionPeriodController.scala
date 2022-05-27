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
import models.pages.DeductionPeriodPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeductionPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.cis.DeductionPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionPeriodController @Inject()(actionsProvider: ActionsProvider,
                                          pageView: DeductionPeriodView,
                                          deductionPeriodService: DeductionPeriodService,
                                          errorHandler: ErrorHandler,
                                          formProvider: DeductionPeriodFormProvider)
                                         (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, contractor: String): Action[AnyContent] =
    actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear, contractor) { implicit request =>
      Ok(pageView(DeductionPeriodPage(taxYear, request.cisUserData, formProvider.deductionPeriodForm(request.user.isAgent))))
    }

  def submit(taxYear: Int, contractor: String): Action[AnyContent] =
    actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear, contractor).async { implicit request =>
      val tempEmployerRef = request.session.get(TEMP_EMPLOYER_REF)
      val deductionPeriodForm = formProvider.deductionPeriodForm(request.user.isAgent, request.cisUserData.cis.priorPeriodData.map(_.deductionPeriod))

      deductionPeriodForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pageView(DeductionPeriodPage(taxYear, request.cisUserData, formWithErrors)))),
        deductionPeriod => deductionPeriodService.submitDeductionPeriod(taxYear, contractor, request.user, deductionPeriod.month, tempEmployerRef).map {
          case Left(_) => errorHandler.internalServerError
          case Right(_) => Redirect(LabourPayController.show(taxYear, deductionPeriod.month.toString, contractor))
        }
      )
    }
}
