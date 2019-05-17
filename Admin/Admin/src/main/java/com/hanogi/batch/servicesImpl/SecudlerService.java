package com.hanogi.batch.servicesImpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hanogi.batch.constants.DeploymentTypes;
import com.hanogi.batch.constants.EmailServiceProviders;
import com.hanogi.batch.entity.BatchRunDetails;
import com.hanogi.batch.entity.Email;
import com.hanogi.batch.entity.EmailDomainDetails;
import com.hanogi.batch.entity.ExecutionStatus;
import com.hanogi.batch.entity.SchedulerJobInfo;
import com.hanogi.batch.repositry.BatchRunDetailsRepo;
import com.hanogi.batch.repositry.EmailDomainDetailsRepo;
import com.hanogi.batch.repositry.EmailRepositry;
import com.hanogi.batch.repositry.SchedulerJobInfoRepo;
import com.hanogi.batch.repositry.WorldCityRepo;
import com.hanogi.batch.repositry.WorldCountryRepo;
import com.hanogi.batch.services.ISecudlerService;
import com.hanogi.batch.utility.Request;

@Service
public class SecudlerService implements ISecudlerService {

	@Autowired
	SchedulerJobInfoRepo schedulerJobInfoRepo;

	@Autowired
	EmailDomainDetailsRepo emailDomainDetailsRepo;

	@Autowired
	BatchRunDetailsRepo batchRunDetailsRepo;

	@Autowired
	EmailRepositry emailRepo;

	@Autowired
	private WorldCountryRepo worldReop;

	@Autowired
	private WorldCityRepo cityRepo;

	public boolean saveNewScheduler(SchedulerJobInfo schedulerJobInfo) {
		try {
			return (schedulerJobInfoRepo.save(schedulerJobInfo) != null);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public Integer saveNewSchedulerBatchDetails(Map<String, String> dateRange) {
		Integer batchId = null;
		if (dateRange != null) {
			try {
				batchId = saveBatchRunDetails(dateRange);
			} catch (Exception e) {
				// TODO: handle exception
			}
		} else {
		}

		return batchId;
	}

	private Integer saveBatchRunDetails(Map<String, String> dateRange) {
		Integer batchId = null;
		BatchRunDetails batchRunDetails = new BatchRunDetails();

		if (dateRange != null) {
			try {

				batchRunDetails.setStatus("1");

				batchRunDetails.setBatchExecutionStatus(null);

				batchRunDetails.setBatchStatusDetails(createStatusJson());

				batchRunDetails.setFromDate(new SimpleDateFormat("DD-MM-YYYY").parse(dateRange.get("From_Date")));

				batchRunDetails.setToDate(new SimpleDateFormat("DD-MM-YYYY").parse(dateRange.get("End_Date")));
				batchRunDetails.setBatchRunDate("");
				batchRunDetails.setBatchStatusId(new ExecutionStatus());

				BatchRunDetails isBatchSaved = batchRunDetailsRepo.save(batchRunDetails);

				batchId = (isBatchSaved != null) ? isBatchSaved.getBatchRunId() : null;

			} catch (Exception e) {
				System.out.println("Error while saving batch:" + e.getMessage());
			}
		} else {

		}

		return batchId;
	}

	private String createStatusJson() {
		JsonObject statusJson = new JsonObject();

		return null;
	}

	public DeploymentTypes[] getAllDeploymentTypes() {
		return DeploymentTypes.values();

	}

	public EmailServiceProviders[] getAllServiceProviders() {
		return EmailServiceProviders.values();
	}

	@Override
	public Map<String, Object> getConfigOptions() {

		Map<String, Object> configOptions = new HashMap<String, Object>();

		configOptions.put("Domain_Types", getAllDeploymentTypes());

		configOptions.put("Service_Providers", getAllServiceProviders());

		return configOptions;
	}

	@Override
	public Boolean saveConfigOptions(Request request) {
		Boolean isSaved = false;

		if (request != null) {
			List<String> allDomainsName = emailDomainDetailsRepo.getAllDomainsName();

			EmailDomainDetails domainDetails = new EmailDomainDetails();

			Map<String, String> requestParam = request.getRequestParam();

			if (requestParam != null) {

				String domainName = requestParam.get("Email_Domain_Name");

				if (!allDomainsName.contains(domainName)) {

					try {

						domainDetails.setEmailDomainName(domainName);

						JsonObject serverConfigProperties = new JsonObject();

						serverConfigProperties.addProperty("adminUserName", requestParam.get("User_Name"));

						serverConfigProperties.addProperty("adminPassword", requestParam.get("Pwd"));

						serverConfigProperties.addProperty("exchangeServerURL", requestParam.get("Server_Url"));

						serverConfigProperties.addProperty("exchangeVersion", requestParam.get("Exchange_version"));

						String domainType = requestParam.get("Domain_Type");

						if (domainType.equalsIgnoreCase("OnCloud")) {

							serverConfigProperties.addProperty("clientId", requestParam.get("Client_Id"));

							serverConfigProperties.addProperty("secreatKey", requestParam.get("Secreat_Key"));

							serverConfigProperties.addProperty("graphApiUrl", requestParam.get("Graph_Api_Url"));
						}

						domainDetails.setEmailServerConfig(serverConfigProperties.toString());

						// Must be Values From given list
						domainDetails.setEmailServiceProvider(requestParam.get("Service_Provider"));
						domainDetails.setServerDeploymentType(domainType);

						// For new entry status must be '1'
						domainDetails.setStatus("1");

						isSaved = (emailDomainDetailsRepo.save(domainDetails) != null);
					} catch (Exception e) {
						// TO_DO Apply logging
						return isSaved;
					}

				} else {
					// Domain name already exists
				}
			} else {
				// TO_DO Apply logging
			}

		} else {
			// TO_DO Apply logging
		}
		return isSaved;
	}

	@Override
	public List<Map<String, String>> getAllServerConfigs() {
		JsonParser parser = new JsonParser();

		List<Map<String, String>> allServerDetails = new ArrayList<>();

		Iterable<EmailDomainDetails> allSavedServerConfigs = emailDomainDetailsRepo.findAll();

		for (EmailDomainDetails emailDomainDetails : allSavedServerConfigs) {
			try {
				Map<String, String> serverConfigsValue = new HashMap<String, String>();

				ObjectMapper oMapper = new ObjectMapper();

				// String emailServerConfig = emailDomainDetails.getEmailServerConfig();

				serverConfigsValue = oMapper.convertValue(emailDomainDetails, Map.class);

				String emailServerConfig = serverConfigsValue.remove("emailServerConfig");

				serverConfigsValue.remove("versionNum");

				JsonObject serverConfigs = parser.parse(emailServerConfig).getAsJsonObject();

				// Map adminData = oMapper.convertValue(serverConfigs, Map.class);
				HashMap adminData = new Gson().fromJson(emailServerConfig, HashMap.class);

				serverConfigsValue.put("userName", (String) adminData.get("adminUserName"));
				serverConfigsValue.put("exchangeServerURL", (String) adminData.get("exchangeServerURL"));
				serverConfigsValue.put("exchangeVersion", (String) adminData.get("exchangeVersion"));

				allServerDetails.add(serverConfigsValue);
			} catch (Exception e) {
				// TO_Do add logging
			}

		}
		return allServerDetails;

	}

	@Override
	public List<Map<String, Object>> getAllSchedulers() {
		Iterable<SchedulerJobInfo> allScheduledJobs = schedulerJobInfoRepo.findAll();

		List<Map<String, Object>> allSchedulers = new ArrayList<>();

		for (SchedulerJobInfo schedulerJobInfo : allScheduledJobs) {
			Map<String, Object> schedulerDetails = new HashMap<String, Object>();

			schedulerDetails.put("Job_Name", schedulerJobInfo.getJobName());
			schedulerDetails.put("Job_group", schedulerJobInfo.getJobGroup());
			schedulerDetails.put("Is_Cron_Job", schedulerJobInfo.getCronJob());
			schedulerDetails.put("Cron_Expres", schedulerJobInfo.getCronExpression());
			schedulerDetails.put("Is_Active", schedulerJobInfo.getIsActive());
			schedulerDetails.put("Batch_Id", schedulerJobInfo.getBatchId());

			allSchedulers.add(schedulerDetails);
		}

		return allSchedulers;
	}

	@Override
	public Boolean scheduleNewJobs(Request scheduleJobRequest) {
		boolean isSaved = false;
		SchedulerJobInfo jobInfo = new SchedulerJobInfo();

		Map<String, String> dateRange = new HashMap<String, String>();

		if (scheduleJobRequest != null) {
			Map<String, String> requestParam = scheduleJobRequest.getRequestParam();
			if (requestParam != null) {
				jobInfo.setIsActive(Boolean.TRUE);
				jobInfo.setJobGroup(requestParam.get("Job_Group"));
				jobInfo.setJobName(requestParam.get("Job_Name"));
				jobInfo.setBatchId(new Integer(requestParam.get("Batch_Id")));

				Boolean isCronJob = new Boolean(requestParam.get("Is_Cron_Job"));
				jobInfo.setCronJob(isCronJob);
				String startDate = requestParam.get("From_date");
				String endDate = requestParam.get("End_date");
				dateRange.put("From_Date", startDate);
				dateRange.put("End_Date", endDate);

				Integer batchId = saveNewSchedulerBatchDetails(dateRange);

				// Use this batch_id to save
				if (isCronJob) {
					jobInfo.setCronExpression(requestParam.get("Cron_Expres"));

				} else {
					String repeatTime = requestParam.get("Repeat_Time");
					if (repeatTime != null) {
						jobInfo.setRepeatTime(new Long(requestParam.get("Repeat_Time")));
					}

				}

				// Integer saveBatchRunDetails = saveBatchRunDetails(dateRange);
				isSaved = saveNewScheduler(jobInfo);
			} else {

			}
		} else {

		}
		return isSaved;
	}

	@Override
	public Boolean saveMailIdList(Request addMailIdListRequest) {
		Boolean isSaved = false;
		if (addMailIdListRequest != null) {
			Map<String, String> requestParam = addMailIdListRequest.getRequestParam();

			if (requestParam != null) {
				Email email = new Email();

				email.setEmailId(requestParam.get("Email_Id"));
				email.setStatus("1");

				isSaved = (emailRepo.save(email) != null);
			} else {

			}
		} else {

		}
		return isSaved;
	}

	@Override
	public Boolean updateConfigOptions(Request configRequest) {

		Boolean isSaved = false;

		if (configRequest != null) {
			List<String> allDomainsName = emailDomainDetailsRepo.getAllDomainsName();

			EmailDomainDetails domainDetails = new EmailDomainDetails();

			Map<String, String> requestParam = configRequest.getRequestParam();

			if (requestParam != null) {
				String id = requestParam.get("id");

				Optional<EmailDomainDetails> domainById = emailDomainDetailsRepo.findById(new Integer(id));

				boolean idExists = domainById.isPresent();

				if (idExists) {

					EmailDomainDetails emailDomainDetails = domainById.get();
					String domainName = requestParam.get("Email_Domain_Name");

					if (!allDomainsName.contains(domainName)) {

						try {

							emailDomainDetails.setEmailDomainName(domainName);

							JsonObject serverConfigProperties = new JsonObject();

							serverConfigProperties.addProperty("adminUserName", requestParam.get("User_Name"));

							serverConfigProperties.addProperty("adminPassword", requestParam.get("Pwd"));

							serverConfigProperties.addProperty("exchangeServerURL", requestParam.get("Server_Url"));

							serverConfigProperties.addProperty("exchangeVersion", requestParam.get("Exchange_version"));

							String domainType = requestParam.get("Domain_Type");

							if (domainType.equalsIgnoreCase("OnCloud")) {

								serverConfigProperties.addProperty("clientId", requestParam.get("Client_Id"));

								serverConfigProperties.addProperty("secreatKey", requestParam.get("Secreat_Key"));

								serverConfigProperties.addProperty("graphApiUrl", requestParam.get("Graph_Api_Url"));
							}

							emailDomainDetails.setEmailServerConfig(serverConfigProperties.toString());

							// Must be Values From given list
							emailDomainDetails.setEmailServiceProvider(requestParam.get("Service_Provider"));
							emailDomainDetails.setServerDeploymentType(domainType);

							// For new entry status must be '1'
							emailDomainDetails.setStatus("1");

							isSaved = (emailDomainDetailsRepo.save(domainDetails) != null);
						} catch (Exception e) {
							// TO_DO Apply logging
							return isSaved;
						}

					} else {
						// Domain name already exists
					}

				} else {

				}
			} else {
				// TO_DO Apply logging
			}

		} else {
			// TO_DO Apply logging
		}
		return isSaved;

	}

	@Override
	public List<String> getWorldCountry() {

		try {
			return worldReop.getCountryNames();
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
			return null;
		}

	}

	@Override
	public Object getCities() {
		try {
			return cityRepo.getCities();
		} catch (Exception e) {
			return null;
		}

	}

	@Override
	public Boolean saveBusinessUnit(Request request) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Boolean saveBusinessDivision(Request request) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Boolean saveNewAccount(Request request) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Boolean saveBusinessUnitDetails(Request request) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Object getBatchStatus() {
		List<Map<String, String>> allBatchStatusList = new ArrayList<Map<String, String>>();

		for (int i = 0; i < 3; i++) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("batchId", (i + 1) + "");
			map.put("totalMailsProceed", "35");
			map.put("timeTaken", "12.7");
			map.put("failedMail", "3");
			map.put("runOn", "2019-01-09 00:00:00");
			map.put("nextTrigger", "2019-01-09 00:00:00");
			allBatchStatusList.add(map);
		}
		return allBatchStatusList;
	}

}
