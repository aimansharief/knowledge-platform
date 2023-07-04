package org.sunbird.actors;

import akka.testkit.TestKit;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sunbird.common.dto.Request;
import org.sunbird.common.dto.Response;
import org.sunbird.common.exception.ResponseCode;
import org.sunbird.search.client.ElasticSearchUtil;
import org.sunbird.search.util.SearchConstants;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
public class AuditHistoryActorTest extends SearchBaseActorTest{

    @BeforeClass
    public static void before() throws Exception {
        createAuditIndex();
        insertAuditTestDoc();
        Thread.sleep(3000);
    }

    @AfterClass
    public static void after() throws Exception {
        System.out.println("deleting index: " + SearchConstants.AUDIT_HISTORY_INDEX);
        ElasticSearchUtil.deleteIndex(SearchConstants.AUDIT_HISTORY_INDEX);
        TestKit.shutdownActorSystem(system, Duration.create(2, TimeUnit.SECONDS), true);
        system = null;
    }

    boolean traversal = true;
    @SuppressWarnings("unchecked")
    @Test
    public void testReadAuditHistory(){
        Request request = getAuditRequest();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> sort = new HashMap<String, Object>();
        sort.put("createdOn", "desc");
        sort.put("operation", "desc");
        List<String> fields = new ArrayList<String>();
        fields.add("audit_id");
        fields.add("label");
        fields.add("objectId");
        fields.add("objectType");
        fields.add("operation");
        fields.add("requestId");
        fields.add("userId");
        fields.add("graphId");
        fields.add("createdOn");
        fields.add("logRecord");
        filters.put("graphId","domain");
        filters.put("objectId","1234");
        request.put("filters", filters);
        request.put("sort_by", sort);
        request.put("traversal", traversal);
        request.put("fields", fields);
        request.put("ACTOR","learning.platform");
        request.getContext().put("CHANNEL_ID","in.ekstep");
        request.getContext().put( "ENV","search");
        System.out.println("request: "+request);
        Response response = getAuditResponse(request);
        Map<String, Object> result = response.getResult();
        List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("results");
        System.out.println("result: "+result);
        Assert.assertNull(list);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvalidOperation(){
        Request request = nullOperationRequest();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> sort = new HashMap<String, Object>();
        sort.put("createdOn", "desc");
        sort.put("operation", "desc");
        List<String> fields = new ArrayList<String>();
        fields.add("audit_id");
        fields.add("label");
        fields.add("objectId");
        fields.add("objectType");
        fields.add("operation");
        fields.add("requestId");
        fields.add("userId");
        fields.add("graphId");
        fields.add("createdOn");
        fields.add("logRecord");
        filters.put("graphId","domain");
        filters.put("objectId","1234");
        request.put("filters", filters);
        request.put("sort_by", sort);
        request.put("traversal", traversal);
        request.put("fields", fields);
        Response response = getAuditResponse(request);
        Map<String, Object> result = response.getResult();
        //List<Map<String, Object>> list = (List<Map<String, Object>>) result.get("results");
        //Assert.assertNull(list);
        String message = (String) result.get("messages");
        Assert.assertTrue(message.contains("Unsupported operation"));
    }


    protected Request nullOperationRequest() {
        Request request = new Request();
        request.setContext(new HashMap<String, Object>());
        return setSearchContext(request, AUDIT_HISTORY_ACTOR , "");
    }
}