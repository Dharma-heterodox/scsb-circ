package org.recap.converter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.recap.ReCAPConstants;
import org.recap.model.*;
import org.recap.model.jaxb.BibRecord;
import org.recap.model.marc.BibMarcRecord;
import org.recap.model.marc.HoldingsMarcRecord;
import org.recap.model.marc.ItemMarcRecord;
import org.recap.repository.BibliographicDetailsRepository;
import org.recap.repository.CollectionGroupDetailsRepository;
import org.recap.repository.InstitutionDetailsRepository;
import org.recap.repository.ItemStatusDetailsRepository;
import org.recap.util.DBReportUtil;
import org.recap.util.MarcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by premkb on 15/12/16.
 */
@Service
public class SCSBToBibEntityConverter implements XmlToBibEntityConverterInterface {

    private static final Logger logger = LoggerFactory.getLogger(SCSBToBibEntityConverter.class);

    @Autowired
    private DBReportUtil dbReportUtil;

    @Autowired
    private CollectionGroupDetailsRepository collectionGroupDetailsRepository;

    @Autowired
    private InstitutionDetailsRepository institutionDetailsRepository;

    @Autowired
    private ItemStatusDetailsRepository itemStatusDetailsRepository;

    @Autowired
    private MarcUtil marcUtil;

    @Autowired
    BibliographicDetailsRepository bibliographicDetailsRepository;
    private Map itemStatusMap;
    private Map collectionGroupMap;
    private Map institutionEntityMap;

    public Map convert(Object scsbRecord) {
        Map<String, Object> map = new HashMap<>();
        boolean processBib = false;

        List<HoldingsEntity> holdingsEntities = new ArrayList<>();
        List<ItemEntity> itemEntities = new ArrayList<>();
        List<ReportEntity> reportEntities = new ArrayList<>();

        getDbReportUtil().setInstitutionEntitiesMap(getInstitutionEntityMap());
        getDbReportUtil().setCollectionGroupMap(getCollectionGroupMap());

        BibRecord bibRecord = (BibRecord) scsbRecord;
        String owningInstitutionBibId = bibRecord.getBib().getOwningInstitutionBibId();
        BibMarcRecord bibMarcRecord = marcUtil.buildBibMarcRecord(bibRecord);
        Record bibRecordObject = bibMarcRecord.getBibRecord();
        String institutionName = bibRecord.getBib().getOwningInstitutionId();

        Integer owningInstitutionId = (Integer) getInstitutionEntityMap().get(institutionName);
        Date currentDate = new Date();
        Map<String, Object> bibMap = processAndValidateBibliographicEntity(bibRecordObject, owningInstitutionId, institutionName, owningInstitutionBibId,currentDate);
        BibliographicEntity bibliographicEntity = (BibliographicEntity) bibMap.get("bibliographicEntity");
        ReportEntity bibReportEntity = (ReportEntity) bibMap.get("bibReportEntity");
        if (bibReportEntity != null) {
            reportEntities.add(bibReportEntity);
        } else {
            processBib = true;
        }

        List<HoldingsMarcRecord> holdingsMarcRecords = bibMarcRecord.getHoldingsMarcRecords();
        if (CollectionUtils.isNotEmpty(holdingsMarcRecords)) {
            for (HoldingsMarcRecord holdingsMarcRecord : holdingsMarcRecords) {
                boolean processHoldings = false;
                Record holdingsRecord = holdingsMarcRecord.getHoldingsRecord();
                Map<String, Object> holdingsMap = processAndValidateHoldingsEntity(bibliographicEntity, institutionName, holdingsRecord, bibRecord,bibRecordObject,currentDate);
                HoldingsEntity holdingsEntity = (HoldingsEntity) holdingsMap.get("holdingsEntity");
                ReportEntity holdingsReportEntity = (ReportEntity) holdingsMap.get("holdingsReportEntity");
                if (holdingsReportEntity != null) {
                    reportEntities.add(holdingsReportEntity);
                } else {
                    processHoldings = true;
                    holdingsEntities.add(holdingsEntity);
                }
                String holdingsCallNumber = marcUtil.getDataFieldValue(holdingsRecord, "852", 'h');
                Character holdingsCallNumberType = marcUtil.getInd1(holdingsRecord, "852", 'h');

                List<ItemMarcRecord> itemMarcRecordList = holdingsMarcRecord.getItemMarcRecordList();
                if (CollectionUtils.isNotEmpty(itemMarcRecordList)) {
                    for (ItemMarcRecord itemMarcRecord : itemMarcRecordList) {
                        Record itemRecord = itemMarcRecord.getItemRecord();
                        Map<String, Object> itemMap = processAndValidateItemEntity(bibliographicEntity, holdingsEntity, owningInstitutionId, holdingsCallNumber, holdingsCallNumberType, itemRecord, institutionName, bibRecord, bibRecordObject,currentDate);
                        ItemEntity itemEntity = (ItemEntity) itemMap.get("itemEntity");
                        ReportEntity itemReportEntity = (ReportEntity) itemMap.get("itemReportEntity");
                        if (itemReportEntity != null) {
                            reportEntities.add(itemReportEntity);
                        } else if (processHoldings) {
                            if (holdingsEntity.getItemEntities() == null) {
                                holdingsEntity.setItemEntities(new ArrayList<>());
                            }
                            holdingsEntity.getItemEntities().add(itemEntity);
                            itemEntities.add(itemEntity);
                        }
                    }
                }

            }
            bibliographicEntity.setHoldingsEntities(holdingsEntities);
            bibliographicEntity.setItemEntities(itemEntities);
        }

        if (CollectionUtils.isNotEmpty(reportEntities)) {
            map.put("reportEntities", reportEntities);
        }
        if (processBib) {
            map.put("bibliographicEntity", bibliographicEntity);
        }

        return map;
    }

    private Map<String, Object> processAndValidateBibliographicEntity(Record bibRecord, Integer owningInstitutionId, String institutionName,String owningInstitutionBibId,Date currentDate) {
        int failedBibCount = 0;
        int successBibCount = 0;
        Map<String, Object> map = new HashMap<>();

        BibliographicEntity bibliographicEntity = new BibliographicEntity();
        StringBuffer errorMessage = new StringBuffer();
        if(StringUtils.isEmpty(owningInstitutionBibId)){
            owningInstitutionBibId = marcUtil.getControlFieldValue(bibRecord, "001");
        }
        if (StringUtils.isNotBlank(owningInstitutionBibId)) {
            bibliographicEntity.setOwningInstitutionBibId(owningInstitutionBibId);
        } else {
            errorMessage.append("Owning Institution Bib Id cannot be null");
        }
        if (owningInstitutionId != null) {
            bibliographicEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append("\n");
            errorMessage.append("Owning Institution Id cannot be null");
        }
        bibliographicEntity.setCreatedDate(currentDate);
        bibliographicEntity.setCreatedBy("submitCollection");
        bibliographicEntity.setLastUpdatedDate(currentDate);
        bibliographicEntity.setLastUpdatedBy("submitCollection");
        bibliographicEntity.setCatalogingStatus(ReCAPConstants.COMPLETE_STATUS);

        String bibXmlStringContent = marcUtil.writeMarcXml(bibRecord);
        if (StringUtils.isNotBlank(bibXmlStringContent)) {
            bibliographicEntity.setContent(bibXmlStringContent.getBytes());
        } else {
            errorMessage.append("\n");
            errorMessage.append("Bib Content cannot be empty");
        }

        boolean subFieldExistsFor245 = marcUtil.isSubFieldExists(bibRecord, "245");
        if (!subFieldExistsFor245) {
            errorMessage.append("\n");
            errorMessage.append("Atleast one subfield should be there for 245 tag");
        }
        Leader leader = bibRecord.getLeader();
        if (leader != null) {
            String leaderValue = bibRecord.getLeader().toString();
            if (!(StringUtils.isNotBlank(leaderValue) && leaderValue.length() == 24)) {
                errorMessage.append("\n");
                errorMessage.append("Leader Field value should be 24 characters");
            }
        }
        List<ReportDataEntity> reportDataEntities = null;

        if (errorMessage.toString().length() > 1) {
            reportDataEntities = getDbReportUtil().generateBibFailureReportEntity(bibliographicEntity, bibRecord);
            ReportDataEntity errorReportDataEntity = new ReportDataEntity();
            errorReportDataEntity.setHeaderName(ReCAPConstants.ERROR_DESCRIPTION);
            errorReportDataEntity.setHeaderValue(errorMessage.toString());
            reportDataEntities.add(errorReportDataEntity);
        }
        if (!CollectionUtils.isEmpty(reportDataEntities)) {
            ReportEntity reportEntity = new ReportEntity();
            reportEntity.setFileName("SubmitCollection_Failure_Report");
            reportEntity.setInstitutionName(institutionName);
            reportEntity.setType(org.recap.ReCAPConstants.FAILURE);
            reportEntity.setCreatedDate(new Date());
            reportEntity.addAll(reportDataEntities);
            map.put("bibReportEntity", reportEntity);
        }
        map.put("bibliographicEntity", bibliographicEntity);
        return map;
    }

    private Map<String, Object> processAndValidateHoldingsEntity(BibliographicEntity bibliographicEntity, String institutionName, Record holdingsRecord, BibRecord bibRecord , Record bibRecordObject,Date currentDate) {
        StringBuffer errorMessage = new StringBuffer();
        Map<String, Object> map = new HashMap<>();
        HoldingsEntity holdingsEntity = new HoldingsEntity();

        String holdingsContent = new MarcUtil().writeMarcXml(holdingsRecord);
        if (StringUtils.isNotBlank(holdingsContent)) {
            holdingsEntity.setContent(holdingsContent.getBytes());
        } else {
            errorMessage.append("Holdings Content cannot be empty");
        }
        holdingsEntity.setCreatedDate(currentDate);
        holdingsEntity.setCreatedBy("submitCollection");
        holdingsEntity.setLastUpdatedDate(currentDate);
        holdingsEntity.setLastUpdatedBy("submitCollection");
        Integer owningInstitutionId = bibliographicEntity.getOwningInstitutionId();
        holdingsEntity.setOwningInstitutionId(owningInstitutionId);
        String owningInstitutionHoldingsId = marcUtil.getDataFieldValue(holdingsRecord, "852", '0');
        if (StringUtils.isBlank(owningInstitutionHoldingsId)) {
            owningInstitutionHoldingsId = UUID.randomUUID().toString();
        } else if (owningInstitutionHoldingsId.length() > 100) {
            owningInstitutionHoldingsId = UUID.randomUUID().toString();
        }
        holdingsEntity.setOwningInstitutionHoldingsId(owningInstitutionHoldingsId);
        List<ReportDataEntity> reportDataEntities = null;
        if (errorMessage.toString().length() > 1) {
            reportDataEntities = getDbReportUtil().generateBibHoldingsFailureReportEntity(bibliographicEntity, holdingsEntity, institutionName, bibRecordObject);
            ReportDataEntity errorReportDataEntity = new ReportDataEntity();
            errorReportDataEntity.setHeaderName(ReCAPConstants.ERROR_DESCRIPTION);
            errorReportDataEntity.setHeaderValue(errorMessage.toString());
            reportDataEntities.add(errorReportDataEntity);
        }

        if (!org.springframework.util.CollectionUtils.isEmpty(reportDataEntities)) {
            ReportEntity reportEntity = new ReportEntity();
            reportEntity.setFileName("SubmitCollection_Failure_Report");
            reportEntity.setInstitutionName(institutionName);
            reportEntity.setType(org.recap.ReCAPConstants.FAILURE);
            reportEntity.setCreatedDate(new Date());
            reportEntity.addAll(reportDataEntities);
            map.put("holdingsReportEntity", reportEntity);
        }
        map.put("holdingsEntity", holdingsEntity);
        return map;
    }

    private Map<String, Object> processAndValidateItemEntity(BibliographicEntity bibliographicEntity, HoldingsEntity holdingsEntity, Integer owningInstitutionId, String holdingsCallNumber, Character holdingsCallNumberType, Record itemRecord, String institutionName, BibRecord bibRecord, Record bibRecordObject,Date currentDate) {
        StringBuffer errorMessage = new StringBuffer();
        Map<String, Object> map = new HashMap<>();
        ItemEntity itemEntity = new ItemEntity();
        String itemBarcode = marcUtil.getDataFieldValue(itemRecord, "876", 'p');
        if (StringUtils.isNotBlank(itemBarcode)) {
            itemEntity.setBarcode(itemBarcode);
            map.put("itemBarcode",itemBarcode);
        } else {
            errorMessage.append("Item Barcode cannot be null");
        }
        String customerCode = marcUtil.getDataFieldValue(itemRecord, "900", 'b');
        if (StringUtils.isNotBlank(customerCode)) {
            itemEntity.setCustomerCode(customerCode);
        } else {
            errorMessage.append("\n");
            errorMessage.append("Customer Code cannot be null");
        }
        itemEntity.setCallNumber(holdingsCallNumber);
        itemEntity.setCallNumberType(String.valueOf(holdingsCallNumberType));
        itemEntity.setItemAvailabilityStatusId((Integer) getItemStatusMap().get("Available"));//TODO need to change
        String copyNumber = marcUtil.getDataFieldValue(itemRecord, "876", 't');
        if (StringUtils.isNotBlank(copyNumber) && org.apache.commons.lang3.math.NumberUtils.isNumber(copyNumber)) {
            itemEntity.setCopyNumber(Integer.valueOf(copyNumber));
        }
        if (owningInstitutionId != null) {
            itemEntity.setOwningInstitutionId(owningInstitutionId);
        } else {
            errorMessage.append("\n");
            errorMessage.append("Owning Institution Id cannot be null");
        }
        String collectionGroupCode = marcUtil.getDataFieldValue(itemRecord, "900", 'a');
        if (StringUtils.isNotBlank(collectionGroupCode) && getCollectionGroupMap().containsKey(collectionGroupCode)) {
            itemEntity.setCollectionGroupId((Integer) getCollectionGroupMap().get(collectionGroupCode));
        } else {
            itemEntity.setCollectionGroupId((Integer) getCollectionGroupMap().get("Open"));
        }
        itemEntity.setCreatedDate(currentDate);
        itemEntity.setCreatedBy("submitCollection");
        itemEntity.setLastUpdatedDate(currentDate);
        itemEntity.setLastUpdatedBy("submitCollection");
        itemEntity.setCatalogingStatus(ReCAPConstants.COMPLETE_STATUS);

        String useRestrictions = marcUtil.getDataFieldValue(itemRecord, "876", 'h');
        if (StringUtils.isNotBlank(useRestrictions) && (useRestrictions.equalsIgnoreCase("In Library Use") || useRestrictions.equalsIgnoreCase("Supervised Use"))) {
            itemEntity.setUseRestrictions(useRestrictions);
        }

        itemEntity.setVolumePartYear(marcUtil.getDataFieldValue(itemRecord, "876", '3'));
        String owningInstitutionItemId = marcUtil.getDataFieldValue(itemRecord, "876", 'a');
        if (StringUtils.isNotBlank(owningInstitutionItemId)) {
            itemEntity.setOwningInstitutionItemId(owningInstitutionItemId);
        } else {
            errorMessage.append("\n");
            errorMessage.append("Item Owning Institution Id cannot be null");
        }

        List<ReportDataEntity> reportDataEntities = null;
        if (errorMessage.toString().length() > 1) {
            reportDataEntities = getDbReportUtil().generateBibHoldingsAndItemsFailureReportEntities(bibliographicEntity, holdingsEntity, itemEntity, institutionName, bibRecordObject);
            ReportDataEntity errorReportDataEntity = new ReportDataEntity();
            errorReportDataEntity.setHeaderName(ReCAPConstants.ERROR_DESCRIPTION);
            errorReportDataEntity.setHeaderValue(errorMessage.toString());
            reportDataEntities.add(errorReportDataEntity);
        }
        if (!org.springframework.util.CollectionUtils.isEmpty(reportDataEntities)) {
            ReportEntity reportEntity = new ReportEntity();
            reportEntity.setFileName("SubmitCollection_Failure_Report");
            reportEntity.setInstitutionName(institutionName);
            reportEntity.setType(org.recap.ReCAPConstants.FAILURE);
            reportEntity.setCreatedDate(new Date());
            reportEntity.addAll(reportDataEntities);
            map.put("itemReportEntity", reportEntity);
        }
        map.put("itemEntity", itemEntity);
        return map;
    }

    public Map getItemStatusMap() {
        if (null == itemStatusMap) {
            itemStatusMap = new HashMap();
            try {
                Iterable<ItemStatusEntity> itemStatusEntities = itemStatusDetailsRepository.findAll();
                for (Iterator iterator = itemStatusEntities.iterator(); iterator.hasNext(); ) {
                    ItemStatusEntity itemStatusEntity = (ItemStatusEntity) iterator.next();
                    itemStatusMap.put(itemStatusEntity.getStatusCode(), itemStatusEntity.getItemStatusId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return itemStatusMap;
    }

    public Map getCollectionGroupMap() {
        if (null == collectionGroupMap) {
            collectionGroupMap = new HashMap();
            try {
                Iterable<CollectionGroupEntity> collectionGroupEntities = collectionGroupDetailsRepository.findAll();
                for (Iterator iterator = collectionGroupEntities.iterator(); iterator.hasNext(); ) {
                    CollectionGroupEntity collectionGroupEntity = (CollectionGroupEntity) iterator.next();
                    collectionGroupMap.put(collectionGroupEntity.getCollectionGroupCode(), collectionGroupEntity.getCollectionGroupId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return collectionGroupMap;
    }

    public Map getInstitutionEntityMap() {
        if (null == institutionEntityMap) {
            institutionEntityMap = new HashMap();
            try {
                Iterable<InstitutionEntity> institutionEntities = institutionDetailsRepository.findAll();
                for (Iterator iterator = institutionEntities.iterator(); iterator.hasNext(); ) {
                    InstitutionEntity institutionEntity = (InstitutionEntity) iterator.next();
                    institutionEntityMap.put(institutionEntity.getInstitutionCode(), institutionEntity.getInstitutionId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return institutionEntityMap;
    }

    public DBReportUtil getDbReportUtil() {
        return dbReportUtil;
    }

    public void setDbReportUtil(DBReportUtil dbReportUtil) {
        this.dbReportUtil = dbReportUtil;
    }
}
