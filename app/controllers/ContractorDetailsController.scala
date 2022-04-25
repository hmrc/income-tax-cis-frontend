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
import config.{AppConfig, ErrorHandler}
import controllers.routes.DeductionPeriodController
import forms.ContractorDetailsForm.contractorDetailsForm
import models.User
import models.forms.ContractorDetailsFormData
import models.mongo.{CisUserData, DatabaseError}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.ContractorDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.ContractorDetailsView

import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsController @Inject()(actionsProvider: ActionsProvider,
                                            contractorDetailsView: ContractorDetailsView,
                                            contractorDetailsService: ContractorDetailsService,
                                            errorHandler: ErrorHandler)
                                           (implicit mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, contractor: Option[String]): Action[AnyContent] = contractor match {
    case None => actionsProvider.notInYear(taxYear)(implicit request =>
      Ok(contractorDetailsView(taxYear, contractorDetailsForm(request.user.isAgent), request.user.isAgent)))
    case Some(contractorRef) => actionsProvider.endOfYearWithSessionData(taxYear, contractorRef) { implicit request =>
      val fillModel = ContractorDetailsFormData(request.cisUserData.cis.contractorName.getOrElse(""), request.cisUserData.employerRef)
      val form = contractorDetailsForm(request.user.isAgent).fill(fillModel)
      Ok(contractorDetailsView(taxYear, form, request.user.isAgent, Some(request.cisUserData.employerRef)))
    }
  }

  def submit(taxYear: Int, contractor: Option[String]): Action[AnyContent] = contractor match {
    case None => actionsProvider.notInYear(taxYear).async { implicit request => handleSubmitRequest(taxYear, request.user) }
    case Some(contractorRef) => actionsProvider.endOfYearWithSessionData(taxYear, contractorRef).async { implicit request =>
      handleSubmitRequest(taxYear, request.user, Some(request.cisUserData))
    }
  }

  def handleSubmitRequest(taxYear: Int,
                          user: User,
                          optCisUserData: Option[CisUserData] = None)(implicit request: Request[_]): Future[Result] = {
    contractorDetailsForm(user.isAgent).bindFromRequest().fold(
      formWithErrors => Future(BadRequest(contractorDetailsView(taxYear, formWithErrors, user.isAgent, optCisUserData.map(_.employerRef)))),
      formData => contractorDetailsService.saveContractorDetails(taxYear, user, optCisUserData, formData).map {
        case Left(_: DatabaseError) => errorHandler.internalServerError()
        case Right(_) => Redirect(DeductionPeriodController.show(taxYear, optCisUserData.map(_.employerRef).getOrElse(formData.employerReferenceNumber)))
      }
    )
  }
}
