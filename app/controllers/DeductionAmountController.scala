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
import forms.FormsProvider
import models.mongo.DatabaseError
import models.pages.DeductionAmountPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeductionAmountService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.DeductionAmountView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionAmountController @Inject()(actionsProvider: ActionsProvider,
                                          formsProvider: FormsProvider,
                                          pageView: DeductionAmountView,
                                          deductionAmountService: DeductionAmountService,
                                          errorHandler: ErrorHandler)
                                         (implicit val cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.notInYearWithSessionData(taxYear, contractor) { implicit request =>
    Ok(pageView(DeductionAmountPage(Month.valueOf(month.toUpperCase), request.cisUserData, formsProvider.deductionAmountForm())))
  }

  def submit(taxYear: Int,
             month: String,
             contractor: String): Action[AnyContent] = actionsProvider.notInYearWithSessionData(taxYear, contractor).async { implicit request =>
    formsProvider.deductionAmountForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(DeductionAmountPage(Month.valueOf(month.toUpperCase), request.cisUserData, formWithErrors)))),
      amount => deductionAmountService.saveDeductionAmount(request.user, request.cisUserData, amount).map {
        case Left(_: DatabaseError) => errorHandler.internalServerError()
        // TODO: The following should be updated to redirect to the next page
        case Right(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    )
  }
}
