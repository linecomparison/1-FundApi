package com.starhouse.bank;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.starhouse.bank.moudles.zhongxin.queryFundNetVal.entity.QueryFundNetVal;
import com.starhouse.bank.moudles.zhongxin.queryFundNetVal.service.QueryFundNetValService;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class ZhongXinApplicationTests {

    private final String BaseUrl = "https://apitest.iservice.citics.com";
    private final String  consumerAuth  = "Zuv9eYzRUg60WmBd";
    private String  token  = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJpbnN0SWQiOiIxMzQ3MSIsIm5iZiI6MTY2MTQxODk4NywiY2hhbm5lbCI6ImdhdGV3YXkiLCJjdXN0VHlwZSI6IjIiLCJleHAiOjE2NjE1MDg5ODd9.nb_IfLi8l2TIVJ5nbjQ-uPB7Xls5zQ6j0ewfvHBXjKw";


    @Autowired
    private QueryFundNetValService queryFundNetValService;
    @Test
    public void getToken(){
        String result = HttpRequest.get(BaseUrl + "/v1/auth/getToken")
                .header(Header.CONTENT_TYPE, "application/form-data")
                .header("consumerAuth",consumerAuth)
                .timeout(80000)
                .execute().body();
        JSONObject jsonObject  = JSONObject.parseObject(result);
        JSONObject data = jsonObject.getObject("data",JSONObject.class);
        token = "Bearer " + data.getObject("token", String.class);
        Console.log(result);
        Console.log(token);
    }

    @SneakyThrows
    private List<JSONObject> getList(String api, HashMap<String,Object> postDataMap){
        if (Strings.isEmpty(token)){
            getToken();
        }
        List<JSONObject> voList = new ArrayList<>();
        boolean isLastPage = false;
        int total = getTotal(api, postDataMap);
        if (total == 0 ){
            throw new Exception("当前无数据");
        }
        Thread.sleep(1000);
        while(!isLastPage){
            String result = HttpRequest.post(BaseUrl + api)
                    .header("Authorization",token)
                    .header("consumerAuth",consumerAuth)
                    .form(postDataMap)
                    .timeout(80000)
                    .execute().body();
            JSONObject jsonObject  = JSONObject.parseObject(result);
            JSONObject data = jsonObject.getObject("data",JSONObject.class);
            // 获取数据
            JSONArray jsonArray = data.getJSONArray("list");
//            List<JSONObject> list = (List<JSONObject>) data.get("list");
            for (Object object : jsonArray) {
                voList.add((JSONObject) object);
            }
            // 更新下一页
            isLastPage = data.getObject("isLastPage",Boolean.class);
            if (!isLastPage){
                String oldPageNum = (String) postDataMap.get("pageNum");
                postDataMap.put("pageNum",oldPageNum + 1);
            }
            Thread.sleep(1000);
        }
        return voList;
    }

    public int getTotal(String api, HashMap<String,Object> postDataMap){
        String result = HttpRequest.post(BaseUrl + api)
                .header("Authorization",token)
                .header("consumerAuth",consumerAuth)
                .form(postDataMap)
                .timeout(80000)
                .execute().body();
        JSONObject jsonObject  = JSONObject.parseObject(result);
        JSONObject data = jsonObject.getObject("data",JSONObject.class);
        int total = data.getObject("total", Integer.class);
        if (total == 0 ){
            System.out.println("当前无数据");
            return 0;
        }
        return total;
    }

    @Test
    public void queryFundNetValForApi(){
        HashMap<String,Object> postDataMap = new HashMap<>();
        postDataMap.put("netBeginDate","20220101");
        postDataMap.put("netEndDate","20220908");
        postDataMap.put("pageSize","100");
        postDataMap.put("pageNum","1");
        List<JSONObject> list = getList("/v1/fa/queryFundNetValForApi", postDataMap);
        List<QueryFundNetVal> voList = new ArrayList<>();
        for (JSONObject object : list) {
            QueryFundNetVal queryFundNetVal = object.toJavaObject(QueryFundNetVal.class);
            queryFundNetVal.setCompany("中信证券");
            voList.add(queryFundNetVal);
        }
        for (QueryFundNetVal queryFundNetVal : voList){
            System.out.println(queryFundNetVal);
        }

//        queryFundNetValService.saveBatch(voList);
    }
}
