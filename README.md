
# income-tax-cis-frontend

This is where users can review and make changes to the CIS section of their income tax return.

## Running the service locally

you will need to have the following:
- Installed [MondoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service-manager](https://github.com/hmrc/service-manager)

This can be found in the [developer handbook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/)

The service manager profile for this service is:

    sm2 --start INCOME_TAX_CIS_FRONTEND
Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL

To run the service locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL
    sm2 --stop INCOME_TAX_CIS_FRONTEND
    ./run.sh **OR** sbt -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes run

This service runs on port: `localhost:9338`

### Running Tests

- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `./check.sh`<br/>
  which runs `sbt clean coverage test it/test coverageReport dependencyUpdates`


### Feature Switches

| Feature                         | Description                                                                                                              |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| AlwaysEOY                       | Returns false to inYear when enabled                                                                                     |
| taxYearErrorFeatureSwitch       | Nothing (needs removing)                                                                                                 |
| welshToggleEnabled              | Enables a toggle to allow the user to change language to/from Welsh                                                      |
| sectionCompletedQuestionEnabled | Allows the user to redirect to the section completed page when enabled and combined with tailoringPhase2Enabled          |
| tailoringPhase2Enabled          | Allows the user to redirect to the section completed page when enabled and combined with sectionCompletedQuestionEnabled |
| sessionCookieServiceEnabled     | Retrieves session data from V&C when enabled                                                                             |
| useEncryption                   | Enables SymmetricCryptoFactory instead of EncryptedValue                                                                 |

### CIS Sources (Contractor and Customer Data)
CIS data can come from different sources: Contractor and Customer. Contractor data is CIS data that HMRC have for the user within the tax year, prior to any updates made by the user. The CIS data displayed in-year is Contractor data.

Customer data is provided by the user. At the end of the tax year, users can view any existing CIS data and make changes (create, update and delete).

Examples can be found here in the [income-tax-submission-stub](https://github.com/hmrc/income-tax-submission-stub/blob/main/app/models/CISUsers.scala)

## Ninos with stub data for CIS

### In-Year
| Nino      | CIS data                              | Source     |
|-----------|---------------------------------------|------------|
| AC150000B | CIS User with multiple CIS deductions | Contractor |
| AA123459A | CIS User with multiple CIS deductions | Contractor |

### End of Year
| Nino      | CIS data                              | Source               |
|-----------|---------------------------------------|----------------------|
| AC150000B | CIS User with multiple CIS deductions | Contractor, Customer |

<details>
<summary>Click here to see an example of a user with Contractor and Customer data (JSON)</summary>

```json
{
  "cis" : [
    {
      "taxYear" : 2023,
      "customerCISDeductions" : {
        "totalDeductionAmount" : 400,
        "totalCostOfMaterials" : 400,
        "totalGrossAmountPaid" : 400,
        "cisDeductions" : [
          {
            "fromDate" : "2022-04-06",
            "toDate" : "2023-04-05",
            "contractorName" : "Michele Lamy Paving Ltd",
            "employerRef" : "111/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2022-04-06",
                "deductionToDate" : "2022-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2022-05-06",
                "deductionToDate" : "2022-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              }
            ]
          },
          {
            "fromDate" : "2022-04-06",
            "toDate" : "2023-04-05",
            "contractorName" : "Jun Takahashi Window Fitting",
            "employerRef" : "222/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2022-04-06",
                "deductionToDate" : "2022-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2022-05-06",
                "deductionToDate" : "2022-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              }
            ]
          }
        ]
      },
      "contractorCISDeductions" : {
        "totalDeductionAmount" : 400,
        "totalCostOfMaterials" : 400,
        "totalGrossAmountPaid" : 400,
        "cisDeductions" : [
          {
            "fromDate" : "2022-04-06",
            "toDate" : "2023-04-05",
            "contractorName" : "Michele Lamy Paving Ltd",
            "employerRef" : "111/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2022-04-06",
                "deductionToDate" : "2022-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2022-05-06",
                "deductionToDate" : "2022-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          },
          {
            "fromDate" : "2022-04-06",
            "toDate" : "2023-04-05",
            "contractorName" : "Jun Takahashi Window Fitting",
            "employerRef" : "222/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2022-04-06",
                "deductionToDate" : "2022-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2022-05-06",
                "deductionToDate" : "2022-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2023-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          }
        ]
      }
    },
    {
      "taxYear" : 2022,
      "customerCISDeductions" : {
        "totalDeductionAmount" : 2400,
        "totalCostOfMaterials" : 2400,
        "totalGrossAmountPaid" : 2400,
        "cisDeductions" : [
          {
            "fromDate" : "2021-04-06",
            "toDate" : "2022-04-05",
            "contractorName" : "Michele Lamy Paving Ltd",
            "employerRef" : "111/11111",
            "totalDeductionAmount" : 300,
            "totalCostOfMaterials" : 300,
            "totalGrossAmountPaid" : 300,
            "periodData" : [
              {
                "deductionFromDate" : "2021-04-06",
                "deductionToDate" : "2021-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-05-06",
                "deductionToDate" : "2021-06-05",
                "deductionAmount" : 200,
                "costOfMaterials" : 200,
                "grossAmountPaid" : 200,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              }
            ]
          },
          {
            "fromDate" : "2021-04-06",
            "toDate" : "2022-04-05",
            "contractorName" : "Jun Takahashi Window Fitting",
            "employerRef" : "222/11111",
            "totalDeductionAmount" : 2100,
            "totalCostOfMaterials" : 2100,
            "totalGrossAmountPaid" : 2100,
            "periodData" : [
              {
                "deductionFromDate" : "2021-04-06",
                "deductionToDate" : "2021-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-05-06",
                "deductionToDate" : "2021-06-05",
                "deductionAmount" : 200,
                "costOfMaterials" : 200,
                "grossAmountPaid" : 200,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-06-06",
                "deductionToDate" : "2021-07-05",
                "deductionAmount" : 300,
                "costOfMaterials" : 300,
                "grossAmountPaid" : 300,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-07-06",
                "deductionToDate" : "2021-08-05",
                "deductionAmount" : 400,
                "costOfMaterials" : 400,
                "grossAmountPaid" : 400,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-08-06",
                "deductionToDate" : "2021-09-05",
                "deductionAmount" : 500,
                "costOfMaterials" : 500,
                "grossAmountPaid" : 500,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              },
              {
                "deductionFromDate" : "2021-09-06",
                "deductionToDate" : "2021-10-05",
                "deductionAmount" : 600,
                "costOfMaterials" : 600,
                "grossAmountPaid" : 600,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "submissionId" : "4557ecb5-fd32-48cc-81f5-e6acd1099f3c",
                "source" : "customer"
              }
            ]
          }
        ]
      },
      "contractorCISDeductions" : {
        "totalDeductionAmount" : 23000,
        "totalCostOfMaterials" : 23000,
        "totalGrossAmountPaid" : 23000,
        "cisDeductions" : [
          {
            "fromDate" : "2021-04-06",
            "toDate" : "2022-04-05",
            "contractorName" : "Michele Lamy Paving Ltd",
            "employerRef" : "111/11111",
            "totalDeductionAmount" : 3000,
            "totalCostOfMaterials" : 3000,
            "totalGrossAmountPaid" : 3000,
            "periodData" : [
              {
                "deductionFromDate" : "2021-06-06",
                "deductionToDate" : "2021-07-05",
                "deductionAmount" : 1000,
                "costOfMaterials" : 1000,
                "grossAmountPaid" : 1000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2021-07-06",
                "deductionToDate" : "2021-08-05",
                "deductionAmount" : 2000,
                "costOfMaterials" : 2000,
                "grossAmountPaid" : 2000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          },
          {
            "fromDate" : "2021-04-06",
            "toDate" : "2022-04-05",
            "contractorName" : "Jun Takahashi Window Fitting",
            "employerRef" : "222/11111",
            "totalDeductionAmount" : 20000,
            "totalCostOfMaterials" : 20000,
            "totalGrossAmountPaid" : 20000,
            "periodData" : [
              {
                "deductionFromDate" : "2021-11-06",
                "deductionToDate" : "2021-12-05",
                "deductionAmount" : 2000,
                "costOfMaterials" : 2000,
                "grossAmountPaid" : 2000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2021-12-06",
                "deductionToDate" : "2022-01-05",
                "deductionAmount" : 3000,
                "costOfMaterials" : 3000,
                "grossAmountPaid" : 3000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2021-01-06",
                "deductionToDate" : "2022-02-05",
                "deductionAmount" : 4000,
                "costOfMaterials" : 4000,
                "grossAmountPaid" : 4000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2021-02-06",
                "deductionToDate" : "2022-03-05",
                "deductionAmount" : 5000,
                "costOfMaterials" : 5000,
                "grossAmountPaid" : 5000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2021-03-06",
                "deductionToDate" : "2022-04-05",
                "deductionAmount" : 6000,
                "costOfMaterials" : 6000,
                "grossAmountPaid" : 6000,
                "submissionDate" : "2021-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          }
        ]
      }
    },
    {
      "taxYear" : 2021,
      "contractorCISDeductions" : {
        "totalDeductionAmount" : 400,
        "totalCostOfMaterials" : 400,
        "totalGrossAmountPaid" : 400,
        "cisDeductions" : [
          {
            "fromDate" : "2020-04-06",
            "toDate" : "2021-04-05",
            "contractorName" : "Michele Lamy Paving Ltd",
            "employerRef" : "111/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2020-04-06",
                "deductionToDate" : "2020-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2020-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2020-05-06",
                "deductionToDate" : "2020-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2020-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          },
          {
            "fromDate" : "2020-04-06",
            "toDate" : "2020-04-05",
            "contractorName" : "Jun Takahashi Window Fitting",
            "employerRef" : "222/11111",
            "totalDeductionAmount" : 200,
            "totalCostOfMaterials" : 200,
            "totalGrossAmountPaid" : 200,
            "periodData" : [
              {
                "deductionFromDate" : "2020-04-06",
                "deductionToDate" : "2020-05-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2020-05-11T16:38:57.489Z",
                "source" : "contractor"
              },
              {
                "deductionFromDate" : "2020-05-06",
                "deductionToDate" : "2020-06-05",
                "deductionAmount" : 100,
                "costOfMaterials" : 100,
                "grossAmountPaid" : 100,
                "submissionDate" : "2020-05-11T16:38:57.489Z",
                "source" : "contractor"
              }
            ]
          }
        ]
      }
    }
  ]
}
```

</details>


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").