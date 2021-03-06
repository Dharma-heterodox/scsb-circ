package org.recap.ils;

import org.junit.Test;
import org.recap.BaseTestCase;
import org.recap.ils.model.response.ItemCheckinResponse;
import org.recap.ils.model.response.ItemCheckoutResponse;
import org.recap.ils.model.response.ItemHoldResponse;
import org.recap.ils.model.response.ItemInformationResponse;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by rajeshbabuk on 19/12/16.
 */
public class NyplApiServiceConnectorUT extends BaseTestCase {

    @Autowired
    NyplApiServiceConnector nyplApiServiceConnector;

    @Test
    public void lookupItem() throws Exception {
        String itemId = "2322222222";
        String source = "nypl-sierra";
        ItemInformationResponse itemInformationResponse = (ItemInformationResponse) nyplApiServiceConnector.lookupItem(itemId);
        assertNotNull(itemInformationResponse);
        assertNotNull(itemInformationResponse.getItemBarcode());
        assertNotNull(itemInformationResponse.getBibID());
        assertTrue(itemInformationResponse.isSuccess());
    }

    @Test
    public void checkoutItem() throws Exception {
        String itemBarcode = "33433001888415";
        String patronBarcode = "23333095887111";
        ItemCheckoutResponse itemCheckoutResponse = (ItemCheckoutResponse) nyplApiServiceConnector.checkOutItem(itemBarcode, patronBarcode);
        assertNotNull(itemCheckoutResponse);
    }

    @Test
    public void checkinItem() throws Exception {
        String itemBarcode = "33433001888415";
        ItemCheckinResponse itemCheckinResponse = (ItemCheckinResponse) nyplApiServiceConnector.checkInItem(itemBarcode, null);
        assertNotNull(itemCheckinResponse);
    }

    @Test
    public void placeHold() throws Exception {
        String itemBarcode = "33433001888415";
        String patronBarcode = "23333095887111";
        String callInstitutionId = "NYPL";
        String itemInstitutionId = "NYPL";
        String expirationDate = "";
        String bibId = "";
        String pickupLocation = "";
        String trackingId = "";
        String title = "";
        String author = "";
        String callNumber = "";
        ItemHoldResponse itemHoldResponse = (ItemHoldResponse)nyplApiServiceConnector.placeHold(itemBarcode, patronBarcode, callInstitutionId, itemInstitutionId, expirationDate, bibId, pickupLocation, trackingId, title, author, callNumber);
        assertNotNull(itemHoldResponse);
    }

    @Test
    public void cancelHold() throws Exception {
        String itemBarcode = "33433001888415";
        String patronBarcode = "23333095887111";
        String institutionId = "NYPL";
        String expirationDate = "";
        String bibId = "";
        String pickupLocation = "";
        String trackingId = "";
        ItemHoldResponse itemHoldResponse = (ItemHoldResponse)nyplApiServiceConnector.cancelHold(itemBarcode, patronBarcode, institutionId, expirationDate, bibId, pickupLocation, trackingId);
        assertNotNull(itemHoldResponse);
    }
}
