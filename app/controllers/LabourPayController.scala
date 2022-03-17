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
import config.{AppConfig, ErrorHandler}
import controllers.routes.DeductionPeriodController
import forms.FormsProvider
import models.NoCYAPeriodDataError
import models.mongo.DatabaseError
import models.pages.LabourPayPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.LabourPayService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import utils.UrlUtils.decode
import views.html.LabourPayView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LabourPayController @Inject()(actionsProvider: ActionsProvider,
                                    formsProvider: FormsProvider,
                                    pageView: LabourPayView,
                                    labourPayService: LabourPayService,
                                    errorHandler: ErrorHandler)
                                   (implicit val cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.notInYearWithSessionData(taxYear, contractor).async { implicit request =>
    val monthValue = Month.valueOf(month.toUpperCase)
    val form = formsProvider.labourPayAmountForm(request.user.isAgent)

    Future.successful(Ok(pageView(LabourPayPage(monthValue, request.cisUserData, form))))
  }

  def submit(taxYear: Int,
             month: String,
             contractor: String): Action[AnyContent] = actionsProvider.notInYearWithSessionData(taxYear, contractor).async { implicit request =>
    val monthValue = Month.valueOf(month.toUpperCase)

    formsProvider.labourPayAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(LabourPayPage(monthValue, request.cisUserData, formWithErrors)))),
      amount => labourPayService.saveLabourPay(taxYear, decode(contractor), request.user, amount).map {
        case Left(_: NoCYAPeriodDataError.type) => Redirect(DeductionPeriodController.show(taxYear, contractor))
        case Left(_: DatabaseError) => errorHandler.internalServerError()
        case Left(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        // TODO: The following should be updated to redirect to the next page
        case Right(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    )
  }
}
